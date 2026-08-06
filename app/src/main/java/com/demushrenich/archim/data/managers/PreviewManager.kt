package com.demushrenich.archim.data.managers

import android.content.Context
import android.content.SharedPreferences
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.demushrenich.archim.data.PreviewInfo
import com.demushrenich.archim.data.ReadingProgress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PreviewManager {
    private const val PREVIEW_PREFS = "preview_metadata"
    private const val PREVIEWS_KEY = "previews"
    private const val TAG = "PreviewManager"

    private val gson = Gson()

    data class PreviewBatchEntry(
        val archiveUri: String,
        val previewPath: String,
        val fileName: String,
        val fileSize: Long
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREVIEW_PREFS, Context.MODE_PRIVATE)
    }

    private fun getArchiveKey(fileName: String, fileSize: Long): String {
        return "${fileName}_${fileSize}".hashCode().toString()
    }

    private fun getPreviewsMap(prefs: SharedPreferences): MutableMap<String, PreviewInfo> {
        val json = prefs.getString(PREVIEWS_KEY, null)
        if (json.isNullOrBlank()) return mutableMapOf()

        return try {
            val type = object : TypeToken<MutableMap<String, PreviewInfo>>() {}.type
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            Log.e(TAG, "getPreviewsMap: error", e)
            mutableMapOf()
        }
    }

    private fun updatePrefsSafely(prefs: SharedPreferences, map: MutableMap<String, PreviewInfo>) {
        val json = gson.toJson(map)
        prefs.edit(commit = true) {
            putString(PREVIEWS_KEY, json)
        }
    }

    fun hasArchiveEntries(context: Context, fileName: String, fileSize: Long): Boolean {
        if (fileName.isEmpty() || fileSize <= 0) return false
        val prefs = getPrefs(context)
        val previews = getPreviewsMap(prefs)
        val key = getArchiveKey(fileName, fileSize)
        return previews.containsKey(key)
    }

    fun getPreviewPath(context: Context, fileName: String? = null, fileSize: Long? = null): String? {
        if (fileName == null || fileSize == null || fileSize <= 0) return null

        val prefs = getPrefs(context)
        val previews = getPreviewsMap(prefs)
        val key = getArchiveKey(fileName, fileSize)
        val previewInfo = previews[key]

        if (previewInfo != null) {
            val previewFile = File(previewInfo.previewPath)
            if (previewFile.exists() && previewFile.length() > 0) {
                return previewInfo.previewPath
            } else {
                removeInvalidPreview(context, key)
                return null
            }
        }
        return null
    }

    fun savePreviewPath(
        context: Context,
        archiveUri: String,
        previewPath: String,
        fileName: String,
        fileSize: Long
    ) {
        if (fileSize <= 0) return

        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val key = getArchiveKey(fileName, fileSize)
            val existingPreview = previews[key]

            val updatedUris = existingPreview?.getAllUris()?.toMutableSet() ?: mutableSetOf()
            updatedUris.add(archiveUri)

            val updatedPreview = existingPreview?.copy(
                archiveUris = updatedUris,
                archiveUri = null,
                previewPath = previewPath,
                timestamp = System.currentTimeMillis()
            ) ?: PreviewInfo(
                archiveUris = setOf(archiveUri),
                previewPath = previewPath,
                timestamp = System.currentTimeMillis()
            )

            previews[key] = updatedPreview
            updatePrefsSafely(prefs, previews)

        } catch (e: Exception) {
            Log.e(TAG, "savePreviewPath: error", e)
        }
    }

    fun savePreviewPaths(context: Context, entries: List<PreviewBatchEntry>) {
        if (entries.isEmpty()) return
        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val now = System.currentTimeMillis()

            entries.forEach { entry ->
                if (entry.fileSize <= 0) return@forEach

                val key = getArchiveKey(entry.fileName, entry.fileSize)
                val existing = previews[key]

                val updatedUris = existing?.getAllUris()?.toMutableSet() ?: mutableSetOf()
                updatedUris.add(entry.archiveUri)

                previews[key] = existing?.copy(
                    archiveUris = updatedUris,
                    archiveUri = null,
                    previewPath = entry.previewPath,
                    timestamp = now
                ) ?: PreviewInfo(
                    archiveUris = setOf(entry.archiveUri),
                    previewPath = entry.previewPath,
                    timestamp = now
                )
            }

            updatePrefsSafely(prefs, previews)
            Log.d(TAG, "savePreviewPaths: saved ${entries.size} entries in one batch")

        } catch (e: Exception) {
            Log.e(TAG, "savePreviewPaths: error", e)
        }
    }

    fun getPreviewsSnapshot(context: Context): Map<String, PreviewInfo> {
        return getPreviewsMap(getPrefs(context))
    }

    fun getPreviewPathFromSnapshot(snapshot: Map<String, PreviewInfo>, fileName: String?, fileSize: Long?): String? {
        if (fileName == null || fileSize == null || fileSize <= 0) return null
        val info = snapshot[getArchiveKey(fileName, fileSize)] ?: return null
        if (info.previewPath.isEmpty()) return null

        val file = File(info.previewPath)
        return if (file.exists() && file.length() > 0) info.previewPath else null
    }

    fun getReadingProgressFromSnapshot(snapshot: Map<String, PreviewInfo>, fileName: String?, fileSize: Long?): ReadingProgress? {
        if (fileName == null || fileSize == null || fileSize <= 0) return null
        return snapshot[getArchiveKey(fileName, fileSize)]?.readingProgress
    }

    fun hasArchiveEntriesFromSnapshot(snapshot: Map<String, PreviewInfo>, fileName: String, fileSize: Long): Boolean {
        if (fileName.isEmpty() || fileSize <= 0) return false
        return snapshot.containsKey(getArchiveKey(fileName, fileSize))
    }

    fun saveReadingProgressForPreview(
        context: Context,
        archiveUri: String,
        fileName: String,
        fileSize: Long,
        currentIndex: Int,
        totalImages: Int
    ) {
        if (fileSize <= 0) return

        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val key = getArchiveKey(fileName, fileSize)
            val existingPreview = previews[key]

            val updatedUris = existingPreview?.getAllUris()?.toMutableSet() ?: mutableSetOf()
            updatedUris.add(archiveUri)

            val readingProgress = ReadingProgress(
                currentIndex = currentIndex,
                totalImages = totalImages,
                lastReadTimestamp = System.currentTimeMillis()
            )

            val updatedPreview = existingPreview?.copy(
                archiveUris = updatedUris,
                archiveUri = null,
                readingProgress = readingProgress
            ) ?: PreviewInfo(
                archiveUris = setOf(archiveUri),
                previewPath = "",
                timestamp = System.currentTimeMillis(),
                readingProgress = readingProgress
            )

            previews[key] = updatedPreview
            updatePrefsSafely(prefs, previews)
        } catch (e: Exception) {
            Log.e(TAG, "saveReadingProgressForPreview: error", e)
        }
    }

    fun getReadingProgressForPreview(
        context: Context,
        fileName: String? = null,
        fileSize: Long? = null
    ): ReadingProgress? {
        if (fileName == null || fileSize == null || fileSize <= 0) return null
        val prefs = getPrefs(context)
        val previews = getPreviewsMap(prefs)
        return previews[getArchiveKey(fileName, fileSize)]?.readingProgress
    }

    fun removePreviewAndProgressByUri(context: Context, archiveUri: String) {
        fun normalizeUri(uri: String): String {
            return try { DocumentsContract.getDocumentId(uri.toUri()) } catch (_: Exception) { uri }
        }

        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val normTarget = normalizeUri(archiveUri)
            var changed = false

            val iterator = previews.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val info = entry.value
                val uris = info.getAllUris().toMutableSet()

                val hasMatch = uris.removeAll { normalizeUri(it) == normTarget }

                if (hasMatch) {
                    changed = true
                    if (uris.isEmpty()) {
                        if (info.previewPath.isNotEmpty()) {
                            val file = File(info.previewPath)
                            if (file.exists()) file.delete()
                        }
                        iterator.remove()
                    } else {
                        entry.setValue(info.copy(archiveUris = uris, archiveUri = null))
                    }
                }
            }

            if (changed) {
                updatePrefsSafely(prefs, previews)
                Log.d(TAG, "removePreviewAndProgressByUri: changes applied")
            }
        } catch (e: Exception) {
            Log.e(TAG, "removePreviewAndProgressByUri: error", e)
        }
    }

    fun clearMetadataForDirectory(context: Context, directoryUri: String) {
        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            var changed = false

            val iterator = previews.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val key = entry.key
                val info = entry.value

                val uris = info.getAllUris().toMutableSet()

                val hasMatch = uris.removeAll { it.startsWith(directoryUri) }

                if (hasMatch) {
                    changed = true
                    if (uris.isEmpty()) {
                        if (info.previewPath.isNotEmpty()) {
                            val file = File(info.previewPath)
                            if (file.exists()) file.delete()
                        }
                        iterator.remove()
                        Log.d(TAG, "clearMetadataForDirectory: Chache deleted $key")
                    } else {
                        entry.setValue(info.copy(archiveUris = uris, archiveUri = null))
                        Log.d(TAG, "clearMetadataForDirectory: Uri to folder deleted, but copy of $key saved")
                    }
                }
            }

            if (changed) {
                updatePrefsSafely(prefs, previews)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error of cleaning meta for folder $directoryUri", e)
        }
    }

    private fun removeInvalidPreview(context: Context, key: String) {
        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val info = previews[key]

            if (info != null && info.previewPath.isNotEmpty()) {
                val previewFile = File(info.previewPath)
                if (previewFile.exists()) previewFile.delete()
            }
            previews.remove(key)
            updatePrefsSafely(prefs, previews)
        } catch (e: Exception) {
            Log.e(TAG, "removeInvalidPreview: error", e)
        }
    }

    fun cleanupOrphanedPreviews(context: Context): Int {
        var deletedCount = 0
        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)

            val registeredFileNames = previews.values
                .mapNotNull { if (it.previewPath.isNotEmpty()) File(it.previewPath).name else null }
                .toSet()

            val previewDir = File(context.filesDir, "previews")
            if (!previewDir.exists()) return 0

            previewDir.listFiles()?.forEach { file ->
                if (file.isFile && !registeredFileNames.contains(file.name)) {
                    if (file.delete()) deletedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "cleanupOrphanedPreviews: error", e)
        }
        return deletedCount
    }
}