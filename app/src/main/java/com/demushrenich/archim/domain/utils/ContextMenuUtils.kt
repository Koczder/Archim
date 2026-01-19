package com.demushrenich.archim.domain.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider

import androidx.documentfile.provider.DocumentFile
import com.demushrenich.archim.R
import com.demushrenich.archim.data.managers.PreviewManager
import com.demushrenich.archim.domain.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun generatePreviewFromImage(
    context: Context,
    imageItem: ImageItem,
    archiveUri: Uri
): String? = withContext(Dispatchers.IO) {
    try {
        val bitmap = when {
            imageItem.data != null -> {
                BitmapFactory.decodeByteArray(imageItem.data, 0, imageItem.data.size)
            }
            !imageItem.filePath.isNullOrEmpty() -> {
                BitmapFactory.decodeFile(imageItem.filePath)
            }
            else -> null
        }

        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.failed_to_load_image), Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }

        val archivePath = archiveUri.toString()
        val documentFile = DocumentFile.fromSingleUri(context, archiveUri)
        val fileName = documentFile?.name ?: archiveUri.lastPathSegment ?: "unknown"
        val fileSize = documentFile?.length() ?: 0L
        val previewFile = getPreviewFileForArchive(context, fileName, fileSize)
        val previewBitmap = createPreviewBitmapWithAspectRatio(bitmap)

        previewFile.parentFile?.mkdirs()

        FileOutputStream(previewFile).use { fos ->
            previewBitmap.compress(Bitmap.CompressFormat.WEBP, 85, fos)
            fos.flush()
            fos.fd.sync()
        }

        bitmap.recycle()
        previewBitmap.recycle()

        if (!previewFile.exists() || previewFile.length() == 0L) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.failed_to_load_image), Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
        PreviewManager.savePreviewPath(context, archivePath, previewFile.absolutePath, fileName, fileSize)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.preview_created_success), Toast.LENGTH_SHORT).show()
        }

        previewFile.absolutePath
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.preview_creation_error, e.message), Toast.LENGTH_SHORT).show()
        }
        null
    }
}

suspend fun copyImageToClipboard(
    context: Context,
    imageItem: ImageItem
): Boolean = withContext(Dispatchers.IO) {
    var tempFile: File? = null
    try {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val cacheDir = File(context.cacheDir, "clipboard")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        cleanOldClipboardFiles(cacheDir)

        val fileName = "clipboard_${System.currentTimeMillis()}.jpg"
        tempFile = File(cacheDir, fileName)
        when {
            imageItem.data != null -> {
                tempFile.writeBytes(imageItem.data)
            }
            !imageItem.filePath.isNullOrEmpty() -> {
                File(imageItem.filePath).copyTo(tempFile, overwrite = true)
            }
            else -> {
                throw Exception(context.getString(R.string.no_image_data))
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        val clip = ClipData.newUri(context.contentResolver, context.getString(R.string.clipboard_image_label), uri)

        clipboardManager.setPrimaryClip(clip)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.image_copied), Toast.LENGTH_SHORT).show()
        }

        true
    } catch (e: Exception) {
        tempFile?.delete()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.copy_error, e.message), Toast.LENGTH_SHORT).show()
        }
        false
    }
}

private fun cleanOldClipboardFiles(cacheDir: File) {
    try {
        val currentTime = System.currentTimeMillis()
        val oneHourAgo = currentTime - (60 * 60 * 1000)

        cacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    } catch (e: Exception) {
    }
}