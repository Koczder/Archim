package com.demushrenich.archim.domain.utils

import android.content.Context
import android.net.Uri
import com.demushrenich.archim.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.SevenZip
import java.io.File
import com.demushrenich.archim.domain.ImageItem

class PasswordRequiredException : Exception("Archive requires password")
class WrongPasswordException : Exception("Incorrect password")
class ExtractionCancelledException : Exception("Extraction cancelled")

fun isImageFile(filename: String): Boolean {
    val exts = listOf("png","jpg","jpeg","webp","bmp","heif","heic","gif","tiff","tif")
    return exts.any { filename.lowercase().endsWith(".$it") }
}

fun getFileExtension(uri: Uri): String =
    uri.toString().substringAfterLast('.', "")

private fun getLargeArchiveCacheDir(context: Context): File {
    val largeArchiveDir = File(context.cacheDir, "largearchive")
    if (!largeArchiveDir.exists()) {
        largeArchiveDir.mkdirs()
    }
    return largeArchiveDir
}

fun clearLargeArchiveCache(context: Context) {
    val largeArchiveDir = getLargeArchiveCacheDir(context)
    try {
        largeArchiveDir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
}

fun clearCacheDir(context: Context) {
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { file ->
        try {
            if (file.name == "largearchive") {
                return@forEach
            }

            if (file.name.startsWith("img_") || file.name.startsWith("archive_") || file.name.startsWith("clipboard")) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}

suspend fun extractFirstImageFromArchive(
    context: Context,
    archiveUri: Uri,
    password: String?,
    onProgress: (String) -> Unit
): ImageItem? = withContext(Dispatchers.IO) {
    onProgress(context.getString(R.string.extracting_opening_archive))

    context.contentResolver.openFileDescriptor(archiveUri, "r")?.use { pfd ->
        FileDescriptorInStream(pfd.fileDescriptor).use { inStream ->
            SevenZip.openInArchive(null, inStream, password).use { inArchive ->
                val simple = inArchive.simpleInterface

                if (simple.archiveItems.any { it.isEncrypted } && password.isNullOrEmpty()) {
                    throw PasswordRequiredException()
                }

                onProgress(context.getString(R.string.extracting_finding_first_image))

                val sortedItems = simple.archiveItems
                    .filter { !it.isFolder && it.path != null }
                    .sortedBy { it.path!!.lowercase() }

                for (item in sortedItems) {
                    coroutineContext.ensureActive()

                    val name = item.path!!

                    if (!isImageFile(name)) {
                        continue
                    }

                    val fileName = name.substringAfterLast('/')
                    onProgress(context.getString(R.string.extracting_extracting_file, fileName))

                    val creationTime = when {
                        item.creationTime != null -> item.creationTime.time
                        item.lastWriteTime != null -> item.lastWriteTime.time
                        item.lastAccessTime != null -> item.lastAccessTime.time
                        else -> System.currentTimeMillis()
                    }

                    val tempFile = File.createTempFile("preview_", ".tmp", context.cacheDir)
                    tempFile.outputStream().use { fos ->
                        val result = item.extractSlow({ data ->
                            if (!coroutineContext.isActive) {
                                throw ExtractionCancelledException()
                            }
                            fos.write(data)
                            data.size
                        }, password)

                        if (item.isEncrypted && result != ExtractOperationResult.OK) {
                            throw WrongPasswordException()
                        }
                    }

                    return@withContext ImageItem(
                        filePath = tempFile.absolutePath,
                        fileName = fileName,
                        creationTime = creationTime,
                        archivePath = name,
                        isFolder = false
                    )
                }

                onProgress(context.getString(R.string.extracting_no_images_found))
                return@withContext null
            }
        }
    }
}

suspend fun extractImagesFromArchive(
    context: Context,
    archiveUri: Uri,
    password: String?,
    onProgress: (Float, String) -> Unit
): List<ImageItem> = withContext(Dispatchers.IO) {
    val entries = mutableListOf<ImageItem>()

    onProgress(0.05f, context.getString(R.string.extracting_opening_archive))

    context.contentResolver.openFileDescriptor(archiveUri, "r")?.use { pfd ->
        FileDescriptorInStream(pfd.fileDescriptor).use { inStream ->
            SevenZip.openInArchive(null, inStream, password).use { inArchive ->
                val simple = inArchive.simpleInterface
                val totalItems = simple.archiveItems.size
                var processedItems = 0

                if (simple.archiveItems.any { it.isEncrypted } && password.isNullOrEmpty()) {
                    throw PasswordRequiredException()
                }

                onProgress(0.1f, context.getString(R.string.extracting_analyzing_content))

                val largeArchiveDir = File(context.cacheDir, "largearchive").apply { mkdirs() }

                for (item in simple.archiveItems) {
                    coroutineContext.ensureActive()

                    processedItems++
                    val progress = 0.1f + (processedItems.toFloat() / totalItems) * 0.9f

                    val name = item.path ?: continue
                    if (item.isFolder) {
                        entries.add(
                            ImageItem(
                                fileName = name.substringAfterLast('/').ifEmpty { name },
                                creationTime = System.currentTimeMillis(),
                                archivePath = name.trimEnd('/'),
                                isFolder = true
                            )
                        )
                        continue
                    }

                    if (!isImageFile(name)) {
                        onProgress(progress, context.getString(R.string.extracting_skipping_file, name.substringAfterLast('/')))
                        continue
                    }

                    val fileName = name.substringAfterLast('/')
                    onProgress(progress, context.getString(R.string.extracting_unpacking_file, fileName))

                    val creationTime = when {
                        item.creationTime != null -> item.creationTime.time
                        item.lastWriteTime != null -> item.lastWriteTime.time
                        item.lastAccessTime != null -> item.lastAccessTime.time
                        else -> System.currentTimeMillis()
                    }

                    val tempFile = File.createTempFile("img_", ".webp", largeArchiveDir)

                    try {
                        tempFile.outputStream().use { fos ->
                            val result = item.extractSlow({ data ->
                                if (!coroutineContext.isActive) {
                                    throw ExtractionCancelledException()
                                }
                                fos.write(data)
                                data.size
                            }, password)

                            if (item.isEncrypted && result != ExtractOperationResult.OK) {
                                throw WrongPasswordException()
                            }
                        }

                        entries.add(
                            ImageItem(
                                filePath = tempFile.absolutePath,
                                fileName = fileName,
                                creationTime = creationTime,
                                archivePath = name,
                                isFolder = false
                            )
                        )
                    } catch (e: ExtractionCancelledException) {
                        tempFile.delete()
                        throw e
                    }
                }
            }
        }
    }

    onProgress(1.0f, context.getString(R.string.extracting_done))
    entries
}