package com.demushrenich.archim.domain.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

private const val PDF_RENDER_SCALE = 2
private const val MAX_BITMAP_DIMENSION = 3000
private const val JPEG_QUALITY = 90

suspend fun renderPdfToImageItems(
    context: Context,
    pdfFile: File,
    archivePathPrefix: String,
    onPageProgress: ((completedCount: Int, totalPages: Int) -> Unit)? = null
): List<ImageItem> = withContext(Dispatchers.IO) {
    val outDir = File(context.cacheDir, "largearchive/pdf_${pdfFile.nameWithoutExtension}").apply { mkdirs() }

    val totalPages = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { it.pageCount }
    }

    if (totalPages <= 0) return@withContext emptyList()

    val results = arrayOfNulls<ImageItem>(totalPages)
    val completed = AtomicInteger(0)
    val concurrency = min(8, Runtime.getRuntime().availableProcessors().coerceAtLeast(1))
    val semaphore = Semaphore(concurrency)

    coroutineScope {
        (0 until totalPages).map { pageIndex ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()

                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            renderer.openPage(pageIndex).use { page ->
                                val rawWidth = page.width * PDF_RENDER_SCALE
                                val rawHeight = page.height * PDF_RENDER_SCALE
                                val scaleDown = min(
                                    1f,
                                    MAX_BITMAP_DIMENSION.toFloat() / maxOf(rawWidth, rawHeight)
                                )
                                val width = (rawWidth * scaleDown).toInt().coerceAtLeast(1)
                                val height = (rawHeight * scaleDown).toInt().coerceAtLeast(1)

                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val pageName = "page_%03d.jpg".format(pageIndex + 1)
                                val outFile = File(outDir, pageName)
                                FileOutputStream(outFile).use { fos ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
                                }
                                bitmap.recycle()

                                results[pageIndex] = ImageItem(
                                    filePath = outFile.absolutePath,
                                    fileName = pageName,
                                    creationTime = System.currentTimeMillis(),
                                    archivePath = "$archivePathPrefix/$pageName",
                                    mediaType = MediaType.IMAGE,
                                    isFolder = false
                                )
                            }
                        }
                    }

                    val done = completed.incrementAndGet()
                    onPageProgress?.invoke(done, totalPages)
                }
            }
        }.awaitAll()
    }

    results.filterNotNull()
}

suspend fun renderPdfFromUri(
    context: Context,
    uri: Uri,
    onProgress: (Float, String) -> Unit
): List<ImageItem> = withContext(Dispatchers.IO) {
    onProgress(0.05f, "Opening PDF")
    val tempPdf = File.createTempFile("opened_", ".pdf", context.cacheDir)
    context.contentResolver.openInputStream(uri)?.use { input ->
        tempPdf.outputStream().use { output -> input.copyTo(output) }
    }
    onProgress(0.1f, "Rendering pages")
    val pages = renderPdfToImageItems(context, tempPdf, archivePathPrefix = tempPdf.nameWithoutExtension) { page, total ->
        val progress = 0.1f + (page.toFloat() / total) * 0.85f
        onProgress(progress, "Rendering page $page / $total")
    }
    tempPdf.delete()
    onProgress(1f, "Done")
    pages
}