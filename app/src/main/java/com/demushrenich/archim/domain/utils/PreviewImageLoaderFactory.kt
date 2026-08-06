package com.demushrenich.archim.domain.utils

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache
import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.domain.PreviewLoadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

private const val TAG = "PreviewImageLoader"

fun buildPreviewImageLoader(context: Context, mode: PreviewLoadingMode): ImageLoader {
    return when (mode) {
        PreviewLoadingMode.FULL -> ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.35)
                    .build()
            }
            .build()

        PreviewLoadingMode.DYNAMIC -> context.imageLoader

        PreviewLoadingMode.DYNAMIC_UNLOAD -> ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.06)
                    .build()
            }
            .build()
    }
}

suspend fun warmUpPreviewCache(
    context: Context,
    imageLoader: ImageLoader,
    archives: List<ArchiveInfo>,
    concurrency: Int = 4,
    batchSize: Int = 20,
    onLoaded: (cacheKey: String) -> Unit = {}
) = withContext(Dispatchers.IO) {
    val semaphore = Semaphore(concurrency)

    archives.chunked(batchSize).forEach { batch ->
        batch.map { archive ->
            async {
                val path = archive.previewPath ?: return@async
                if (!File(path).exists()) return@async
                semaphore.withPermit {
                    try {
                        imageLoader.execute(buildPreviewImageRequest(context, path))
                        onLoaded(previewCacheKey(path))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to warm up preview for ${archive.displayName}: ${e.message}")
                    }
                }
            }
        }.awaitAll()
        yield()
    }
}