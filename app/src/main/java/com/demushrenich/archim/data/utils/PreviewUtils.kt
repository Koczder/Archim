package com.demushrenich.archim.data.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.demushrenich.archim.data.managers.PreviewManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import androidx.core.graphics.scale
import androidx.documentfile.provider.DocumentFile
import com.demushrenich.archim.R

fun getPreviewCacheDir(context: Context): File {
    val previewsDir = File(context.filesDir, "previews")
    if (!previewsDir.exists()) previewsDir.mkdirs()
    return previewsDir
}

fun getPreviewFileForArchive(context: Context, fileName: String, fileSize: Long): File {
    val previewDir = getPreviewCacheDir(context)
    val uniqueKey = "${fileName}_${fileSize}".hashCode().toString()
    return File(previewDir, "$uniqueKey.webp")
}

@SuppressLint("Recycle")
suspend fun generatePreviewForArchive(
    context: Context,
    archiveUri: Uri,
    onProgress: ((String) -> Unit)? = null
): String? = withContext(Dispatchers.IO) {
    val archivePath = archiveUri.toString()
    val fileDescriptor = context.contentResolver.openFileDescriptor(archiveUri, "r")
    val fileSize = fileDescriptor?.statSize ?: 0L
    fileDescriptor?.close()
    val fileName = try {
        val documentFile = DocumentFile.fromSingleUri(context, archiveUri)
        documentFile?.name ?: archiveUri.lastPathSegment ?: archiveUri.toString().substringAfterLast('/')
    } catch (e: Exception) {
        archiveUri.lastPathSegment ?: archiveUri.toString().substringAfterLast('/')
    }
    val existingPreview = PreviewManager.getPreviewPath(context, fileName, fileSize)
    if (existingPreview != null && File(existingPreview).exists()) {
        return@withContext existingPreview
    }
    val previewFile = getPreviewFileForArchive(context, fileName, fileSize)
    onProgress?.invoke(context.getString(R.string.preview_generating))

    try {
        val firstImage = extractFirstImageFromArchive(
            context = context,
            archiveUri = archiveUri,
            password = null,
            onProgress = { msg -> onProgress?.invoke(msg) }
        )

        if (firstImage == null) {
            onProgress?.invoke(context.getString(R.string.extracting_no_images_found))
            return@withContext null
        }

        onProgress?.invoke(context.getString(R.string.preview_creating_from_file, firstImage.fileName))

        val bitmap: Bitmap? = firstImage.filePath?.let { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                try {
                    file.delete()
                } catch (_: Exception) {}
                bitmap
            } else null
        }

        if (bitmap != null) {
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
                onProgress?.invoke(context.getString(R.string.preview_write_error))
                return@withContext null
            }
            PreviewManager.savePreviewPath(
                context,
                archivePath,
                previewFile.absolutePath,
                fileName,
                fileSize
            )

            onProgress?.invoke(context.getString(R.string.preview_created_success))
            return@withContext previewFile.absolutePath
        }

        onProgress?.invoke(context.getString(R.string.preview_image_read_failed))
        null
    } catch (e: PasswordRequiredException) {
        onProgress?.invoke(context.getString(R.string.preview_password_protected))
        null
    } catch (e: Exception) {
        onProgress?.invoke(context.getString(R.string.preview_error_with_message, e.message ?: ""))
        null
    }
}

fun createPreviewBitmapWithAspectRatio(original: Bitmap): Bitmap {
    val width = original.width.toFloat()
    val height = original.height.toFloat()
    val aspectRatio = width / height

    val (targetWidth, targetHeight) = when {
        aspectRatio > 2.5f -> {
            val w = 400
            val h = (w / aspectRatio).toInt()
            w to max(h, 120)
        }
        aspectRatio > 1.3f -> {
            val w = 350
            val h = (w / aspectRatio).toInt()
            w to max(h, 150)
        }

        aspectRatio >= 0.8f && aspectRatio <= 1.2f -> {
            280 to 280
        }

        aspectRatio > 0.5f -> {
            val h = 350
            val w = (h * aspectRatio).toInt()
            max(w, 150) to h
        }

        else -> {
            val h = 400
            val w = (h * aspectRatio).toInt()
            max(w, 120) to h
        }
    }
    if (width.toInt() == targetWidth && height.toInt() == targetHeight) {
        return original.copy(original.config ?: Bitmap.Config.ARGB_8888, false)
    }

    return original.scale(targetWidth, targetHeight)
}