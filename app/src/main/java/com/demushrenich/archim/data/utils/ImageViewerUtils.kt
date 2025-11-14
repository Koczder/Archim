package com.demushrenich.archim.data.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.demushrenich.archim.data.ArchiveNavigationState
import com.demushrenich.archim.data.managers.ArchiveStructureManager
import com.demushrenich.archim.domain.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageViewerUtils {
    suspend fun updateProgress(
        context: Context,
        archiveUri: Uri?,
        currentImage: ImageItem,
        archiveNavState: ArchiveNavigationState?
    ) = withContext(Dispatchers.IO) {
        try {
            val uri = archiveUri ?: return@withContext
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            val fileName = documentFile?.name ?: uri.lastPathSegment ?: ""
            val fileSize = documentFile?.length() ?: 0L

            if (fileSize <= 0) return@withContext

            val levelPath = archiveNavState?.getCurrentLevel()?.path ?: ""

            val structure = ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)
            val levelData = structure?.levels?.find { it.path == levelPath }

            val effectiveOrder: List<String> = levelData?.imageIds ?: emptyList()

            val indexInLevel = effectiveOrder.indexOf(currentImage.id)
            val readCount = if (indexInLevel >= 0) indexInLevel + 1 else 0

            ArchiveStructureManager.updateLevelReadCount(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                levelPath = levelPath,
                readCount = readCount
            )

            ArchiveStructureManager.updateLastImageId(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                lastImageId = currentImage.id
            )

            ArchiveStructureManager.updateLastImageIdLevel(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                levelPath = levelPath,
                lastImageIdLevel = currentImage.id
            )

            ArchiveStructureManager.syncProgressToPreview(
                context = context,
                fileName = fileName,
                fileSize = fileSize
            )

            Log.d("ImageViewer", "Updated progress: readCount=$readCount for level=$levelPath, lastImageId=${currentImage.id}")

        } catch (e: Exception) {
            Log.e("ImageViewer", "Error updating progress: ${e.message}", e)
        }
    }

    suspend fun calculateDisplayText(
        context: Context,
        archiveUri: Uri?,
        archiveNavState: ArchiveNavigationState?,
        imagesList: List<ImageItem>,
        selectedIndex: Int
    ): String = withContext(Dispatchers.IO) {
        try {
            if (archiveNavState == null || archiveUri == null) {
                return@withContext "${selectedIndex + 1} / ${imagesList.size}"
            }

            val documentFile = DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: ""
            val fileSize = documentFile?.length() ?: 0L

            if (fileSize <= 0) {
                return@withContext "${selectedIndex + 1} / ${imagesList.size}"
            }

            val levelPath = archiveNavState.getCurrentLevel()?.path ?: ""
            val structure = ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)
            val levelData = structure?.levels?.find { it.path == levelPath }

            if (levelData != null && levelData.imageIds.isNotEmpty()) {
                val currentImageId = imagesList[selectedIndex].id
                val localIndex = levelData.imageIds.indexOf(currentImageId)
                val totalInLevel = levelData.imageIds.size

                return@withContext if (localIndex >= 0) {
                    "${localIndex + 1} / $totalInLevel"
                } else {
                    "${selectedIndex + 1} / ${imagesList.size}"
                }
            }

            return@withContext "${selectedIndex + 1} / ${imagesList.size}"

        } catch (e: Exception) {
            Log.e("ImageViewer", "Error calculating display index: ${e.message}", e)
            return@withContext "${selectedIndex + 1} / ${imagesList.size}"
        }
    }

    fun filterImages(viewerImages: List<ImageItem>): List<ImageItem> {
        return viewerImages.filter { !it.isFolder }
    }
}