package com.demushrenich.archim.data.managers

import android.content.Context
import android.util.Log
import com.demushrenich.archim.data.ArchiveLevelData
import com.demushrenich.archim.data.ArchiveNavigationState
import com.demushrenich.archim.data.ArchiveStructure
import com.demushrenich.archim.domain.SortType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.collections.forEach

object ArchiveStructureManager {
    private const val TAG = "ArchiveStructureManager"
    private const val STRUCTURES_DIR = "archive_structures"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private fun getStructuresDir(context: Context): File {
        val dir = File(context.filesDir, STRUCTURES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getArchiveKey(fileName: String, fileSize: Long): String {
        return "${fileName}_${fileSize}".hashCode().toString()
    }

    private fun getStructureFile(context: Context, fileName: String, fileSize: Long): File {
        val key = getArchiveKey(fileName, fileSize)
        return File(getStructuresDir(context), "$key.json")
    }

    fun saveArchiveStructure(
        context: Context,
        archiveUri: String,
        fileName: String,
        fileSize: Long,
        archiveNavState: ArchiveNavigationState,
        currentSortType: SortType? = null,
        currentLevelPath: String? = null
    ) {
        if (fileSize <= 0) {
            Log.w(TAG, "saveArchiveStructure: invalid file size")
            return
        }

        try {
            val existingStructure = loadArchiveStructure(context, fileName, fileSize)
            val existingLevels = existingStructure?.levels?.associateBy { it.path }?.toMutableMap() ?: mutableMapOf()

            val allLevels = archiveNavState.exportLevels()

            allLevels.forEach { (_, archiveLevel) ->
                val imagesOnLevel = archiveLevel.entries.filter { !it.isFolder }

                val existingLevel = existingLevels[archiveLevel.path]

                val sortTypeForLevel = if (currentLevelPath == archiveLevel.path && currentSortType != null) {
                    currentSortType
                } else {
                    existingLevel?.sortType ?: SortType.NAME_ASC
                }

                val imageIdsForLevel = if (existingLevel?.imageIds?.isNotEmpty() == true) {
                    existingLevel.imageIds
                } else {
                    imagesOnLevel.map { it.id }
                }

                val readCountForLevel = existingLevel?.readCount ?: 0
                val lastImageIdLevelForLevel = existingLevel?.lastImageIdLevel

                existingLevels[archiveLevel.path] = ArchiveLevelData(
                    path = archiveLevel.path,
                    imageIds = imageIdsForLevel,
                    sortType = sortTypeForLevel,
                    readCount = readCountForLevel,
                    lastImageIdLevel = lastImageIdLevelForLevel
                )
            }

            val updatedStructure = ArchiveStructure(
                archiveUri = archiveUri,
                fileName = fileName,
                fileSize = fileSize,
                totalImages = archiveNavState.allImages.size,
                levels = existingLevels.values.toList(),
                lastImageId = existingStructure?.lastImageId
            )

            val file = getStructureFile(context, fileName, fileSize)
            file.writeText(gson.toJson(updatedStructure))

            Log.d(TAG, "saveArchiveStructure: merged ${allLevels.size} new levels, total ${existingLevels.size}")
        } catch (e: Exception) {
            Log.e(TAG, "saveArchiveStructure: error", e)
        }
    }

    fun loadArchiveStructure(
        context: Context,
        fileName: String,
        fileSize: Long
    ): ArchiveStructure? {
        if (fileSize <= 0) {
            return null
        }

        try {
            val file = getStructureFile(context, fileName, fileSize)
            if (!file.exists()) {
                Log.d(TAG, "loadArchiveStructure: file not found for $fileName")
                return null
            }

            val json = file.readText()
            val structure = gson.fromJson(json, ArchiveStructure::class.java)

            Log.d(TAG, "loadArchiveStructure: loaded structure for $fileName with ${structure.levels.size} levels")
            return structure
        } catch (e: Exception) {
            Log.e(TAG, "loadArchiveStructure: error", e)
            return null
        }
    }

    fun updateLevelImageIds(
        context: Context,
        fileName: String,
        fileSize: Long,
        levelPath: String,
        imageIds: List<String>
    ) {
        if (fileSize <= 0) return

        try {
            val structure = loadArchiveStructure(context, fileName, fileSize) ?: return

            val updatedLevels = structure.levels.map { level ->
                if (level.path == levelPath) {
                    level.copy(imageIds = imageIds)
                } else {
                    level
                }
            }

            val updatedStructure = structure.copy(
                levels = updatedLevels,
                lastModified = System.currentTimeMillis()
            )

            val file = getStructureFile(context, fileName, fileSize)
            file.writeText(gson.toJson(updatedStructure))

            Log.d(TAG, "updateLevelImageIds: updated image order for level '$levelPath' with ${imageIds.size} images")
        } catch (e: Exception) {
            Log.e(TAG, "updateLevelImageIds: error", e)
        }
    }

    fun updateLevelReadCount(
        context: Context,
        fileName: String,
        fileSize: Long,
        levelPath: String,
        readCount: Int
    ) {
        if (fileSize <= 0) return

        try {
            val structure = loadArchiveStructure(context, fileName, fileSize) ?: return

            val updatedLevels = structure.levels.map { level ->
                if (level.path == levelPath) {
                    level.copy(readCount = readCount)
                } else {
                    level
                }
            }

            val updatedStructure = structure.copy(
                levels = updatedLevels,
                lastModified = System.currentTimeMillis()
            )

            val file = getStructureFile(context, fileName, fileSize)
            file.writeText(gson.toJson(updatedStructure))

            Log.d(TAG, "updateLevelReadCount: updated read count for level '$levelPath' to $readCount")
        } catch (e: Exception) {
            Log.e(TAG, "updateLevelReadCount: error", e)
        }
    }

    fun updateLastImageId(
        context: Context,
        fileName: String,
        fileSize: Long,
        lastImageId: String
    ) {
        if (fileSize <= 0) return

        try {
            val structure = loadArchiveStructure(context, fileName, fileSize) ?: return

            val updatedStructure = structure.copy(
                lastImageId = lastImageId,
                lastModified = System.currentTimeMillis()
            )

            val file = getStructureFile(context, fileName, fileSize)
            file.writeText(gson.toJson(updatedStructure))

            Log.d(TAG, "updateLastImageId: updated to $lastImageId")
        } catch (e: Exception) {
            Log.e(TAG, "updateLastImageId: error", e)
        }
    }

    fun updateLastImageIdLevel(
        context: Context,
        fileName: String,
        fileSize: Long,
        levelPath: String,
        lastImageIdLevel: String
    ) {
        if (fileSize <= 0) return

        try {
            val structure = loadArchiveStructure(context, fileName, fileSize) ?: return

            val updatedLevels = structure.levels.map { level ->
                if (level.path == levelPath) {
                    level.copy(lastImageIdLevel = lastImageIdLevel)
                } else {
                    level
                }
            }

            val updatedStructure = structure.copy(
                levels = updatedLevels,
                lastModified = System.currentTimeMillis()
            )

            val file = getStructureFile(context, fileName, fileSize)
            file.writeText(gson.toJson(updatedStructure))

            Log.d(TAG, "updateLastImageIdLevel: updated to $lastImageIdLevel for level '$levelPath'")
        } catch (e: Exception) {
            Log.e(TAG, "updateLastImageIdLevel: error", e)
        }
    }

    fun syncProgressToPreview(
        context: Context,
        fileName: String,
        fileSize: Long
    ) {
        if (fileSize <= 0) return

        try {
            val structure = loadArchiveStructure(context, fileName, fileSize) ?: return

            val totalReadCount = structure.getTotalReadCount()

            PreviewManager.saveReadingProgressForPreview(
                context = context,
                archiveUri = structure.archiveUri,
                fileName = fileName,
                fileSize = fileSize,
                currentIndex = totalReadCount,
                totalImages = structure.totalImages
            )

            Log.d(TAG, "syncProgressToPreview: synced progress $totalReadCount/${structure.totalImages}")
        } catch (e: Exception) {
            Log.e(TAG, "syncProgressToPreview: error", e)
        }
    }

    fun deleteArchiveStructure(
        context: Context,
        fileName: String,
        fileSize: Long
    ) {
        if (fileSize <= 0) return

        try {
            val file = getStructureFile(context, fileName, fileSize)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "deleteArchiveStructure: deleted structure for $fileName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteArchiveStructure: error", e)
        }
    }

}