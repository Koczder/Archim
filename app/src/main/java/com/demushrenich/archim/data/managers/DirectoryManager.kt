package com.demushrenich.archim.data.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.data.DirectoryArchivesInfo
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.data.utils.isSupportedArchive
import com.demushrenich.archim.domain.SortType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import kotlin.collections.forEach
import kotlin.collections.plusAssign
import kotlin.math.abs
import androidx.core.content.edit
import com.demushrenich.archim.R

object DirectoryManager {

    private const val SORT_PREFERENCES_NAME = "directory_sort_preferences"
    private const val SORT_KEY_PREFIX = "sort_"

    private fun getAddedDirCacheDir(context: Context): File {
        val addedDirDir = File(context.filesDir, "addedDir")
        if (!addedDirDir.exists()) {
            addedDirDir.mkdirs()
        }
        return addedDirDir
    }

    private fun getDirectoriesListFile(context: Context): File {
        return File(getAddedDirCacheDir(context), "directories.json")
    }

    private fun saveDirectoriesList(context: Context, directories: List<DirectoryItem>) {
        try {
            val json = Gson().toJson(directories)
            getDirectoriesListFile(context).writeText(json)
        } catch (_: Exception) {}
    }

    fun loadSavedDirectories(context: Context): List<DirectoryItem> {
        return try {
            val file = getDirectoriesListFile(context)
            if (!file.exists()) return emptyList()

            val json = file.readText()
            val type = object : TypeToken<List<DirectoryItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
    fun saveSortTypeForDirectory(context: Context, directoryUri: String, sortType: SortType) {
        try {
            val prefs = context.getSharedPreferences(SORT_PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putString(SORT_KEY_PREFIX + directoryUri, sortType.name)
            }
            Log.d("DirectoryManager", "Saved sort type $sortType for directory $directoryUri")
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error saving sort type", e)
        }
    }
    fun loadSortTypeForDirectory(context: Context, directoryUri: String): SortType? {
        return try {
            val prefs = context.getSharedPreferences(SORT_PREFERENCES_NAME, Context.MODE_PRIVATE)
            val sortTypeName = prefs.getString(SORT_KEY_PREFIX + directoryUri, null)
            sortTypeName?.let {
                try {
                    SortType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    Log.w("DirectoryManager", "Invalid sort type name: $it")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error loading sort type", e)
            null
        }
    }

    fun clearSortTypeForDirectory(context: Context, directoryUri: String) {
        try {
            val prefs = context.getSharedPreferences(SORT_PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                remove(SORT_KEY_PREFIX + directoryUri)
            }
            Log.d("DirectoryManager", "Cleared sort type for directory $directoryUri")
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error clearing sort type", e)
        }
    }

    fun addDirectory(context: Context, treeUri: Uri): DirectoryItem? {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {}

        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        val directoryItem = DirectoryItem(
            uri = treeUri.toString(),
            displayName = dir.name ?: context.getString(R.string.no_name),
            isFolder = true,
            lastModified = dir.lastModified()
        )

        val savedDirectories = loadSavedDirectories(context).toMutableList()
        if (savedDirectories.none { it.uri == directoryItem.uri }) {
            savedDirectories.add(directoryItem)
            saveDirectoriesList(context, savedDirectories)
        }

        return directoryItem
    }

    fun removeDirectory(context: Context, directoryUri: String): Boolean {
        return try {
            val savedDirectories = loadSavedDirectories(context).toMutableList()
            val removed = savedDirectories.removeAll { it.uri == directoryUri }
            if (removed) {
                saveDirectoriesList(context, savedDirectories)

                clearSortTypeForDirectory(context, directoryUri)

                try {
                    context.contentResolver.releasePersistableUriPermission(
                        directoryUri.toUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
            }
            removed
        } catch (_: Exception) {
            false
        }
    }

    suspend fun removeAllPreviewsAndProgressForDirectory(
        context: Context,
        directoryUri: String,
        allDirectories: List<DirectoryItem>
    ) = withContext(Dispatchers.IO) {
        try {
            val archives = getAllArchivesFromDirectoryExcludingSubDirectories(
                context, directoryUri.toUri(), allDirectories
            )
            archives.forEach { archive ->
                PreviewManager.removePreviewAndProgressByUri(
                    context = context,
                    archiveUri = archive.filePath,
                )
                ArchiveStructureManager.deleteArchiveStructure(
                    context = context,
                    fileName = archive.originalName,
                    fileSize = archive.fileSize
                )

                yield()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun getArchivesInfoForDirectory(
        context: Context,
        directoryUri: String,
        allDirectories: List<DirectoryItem>
    ): DirectoryArchivesInfo = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, directoryUri.toUri())
            ?: return@withContext DirectoryArchivesInfo(0, 0, 0)

        val excludedDirs = allDirectories.map { it.uri }.toSet()

        var totalArchives = 0
        var archivesToDelete = 0
        var skippedDirs = 0

        suspend fun scan(directory: DocumentFile) {
            try {
                directory.listFiles().forEach { file ->
                    yield()

                    if (file.isDirectory) {
                        val isExcluded = excludedDirs.contains(file.uri.toString())
                        if (isExcluded) {
                            skippedDirs++
                        } else {
                            scan(file)
                        }
                    } else if (file.isFile) {
                        val ext = file.name.orEmpty().substringAfterLast('.', "")
                        if (isSupportedArchive(ext)) {
                            totalArchives++
                            archivesToDelete++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DirectoryManager", "Error scanning ${directory.uri}", e)
            }
        }

        scan(root)

        DirectoryArchivesInfo(
            totalArchivesCount = totalArchives,
            archivesToDeleteCount = archivesToDelete,
            skippedDirectoriesCount = skippedDirs
        )
    }

    internal suspend fun getAllArchivesFromDirectoryExcludingSubDirectories(
        context: Context,
        dirUri: Uri,
        allDirectories: List<DirectoryItem>
    ): List<ArchiveInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ArchiveInfo>()
        val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return@withContext emptyList()

        Log.d("DirectoryManager", "=== Starting scan of directory: ${dir.name} ===")

        val directoryDocumentFiles = allDirectories.mapNotNull { dirItem ->
            try {
                DocumentFile.fromTreeUri(context, dirItem.uri.toUri())
            } catch (e: Exception) {
                Log.e("DirectoryManager", "Error creating DocumentFile for ${dirItem.uri}", e)
                null
            }
        }

        fun isSameDirectory(dir1: DocumentFile, dir2: DocumentFile): Boolean {
            return try {
                val uriMatch = dir1.uri.toString() == dir2.uri.toString()
                val nameMatch = dir1.name == dir2.name
                val timeMatch = abs(dir1.lastModified() - dir2.lastModified()) < 1000
                uriMatch || (nameMatch && timeMatch)
            } catch (e: Exception) {
                false
            }
        }

        suspend fun scanDirectory(directory: DocumentFile, depth: Int = 0) {
            try {
                directory.listFiles().forEach { file ->
                    yield()

                    if (file.isDirectory) {
                        val isInDirectoryList = directoryDocumentFiles.any { savedDir ->
                            isSameDirectory(file, savedDir)
                        }

                        if (!isInDirectoryList) {
                            scanDirectory(file, depth + 1)
                        }
                    } else if (file.isFile) {
                        val ext = file.name.orEmpty().substringAfterLast('.', "")
                        if (isSupportedArchive(ext)) {
                            val previewPath = PreviewManager.getPreviewPath(
                                context = context,
                                fileName = file.name.orEmpty(),
                                fileSize = file.length()
                            )

                            result.add(
                                ArchiveInfo(
                                    filePath = file.uri.toString(),
                                    originalName = file.name.orEmpty(),
                                    displayName = file.name.orEmpty(),
                                    lastModified = file.lastModified(),
                                    fileSize = file.length(),
                                    previewPath = previewPath
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DirectoryManager", "Error scanning directory: ${directory.name}", e)
            }
        }

        scanDirectory(dir)
        Log.d("DirectoryManager", "Total archives found: ${result.size}")

        result
    }

    suspend fun listChildren(
        context: Context,
        dirUri: Uri
    ): Pair<List<DirectoryItem>, List<ArchiveInfo>> = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, dirUri)
            ?: return@withContext emptyList<DirectoryItem>() to emptyList()

        val folders = mutableListOf<DirectoryItem>()
        val archives = mutableListOf<ArchiveInfo>()

        dir.listFiles().forEach { f ->
            yield()

            if (f.isDirectory) {
                folders += DirectoryItem(
                    uri = f.uri.toString(),
                    displayName = f.name ?: context.getString(R.string.default_folder_name),
                    isFolder = true,
                    lastModified = f.lastModified()
                )
            } else if (f.isFile) {
                val ext = f.name.orEmpty().substringAfterLast('.', "")
                if (isSupportedArchive(ext)) {
                    val previewPath = PreviewManager.getPreviewPath(
                        context = context,
                        fileName = f.name.orEmpty(),
                        fileSize = f.length()
                    )

                    archives += ArchiveInfo(
                        filePath = f.uri.toString(),
                        originalName = f.name.orEmpty(),
                        displayName = f.name.orEmpty(),
                        lastModified = f.lastModified(),
                        fileSize = f.length(),
                        previewPath = previewPath
                    )
                }
            }
        }

        folders to archives
    }
}