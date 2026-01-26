package com.demushrenich.archim.domain.utils

import android.content.Context
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache


fun clearArchiveImagesFromCache(context: Context, imageIds: List<String>) {
    val imageLoader: ImageLoader = context.imageLoader

    val memoryCache = imageLoader.memoryCache
    imageIds.forEach { id ->
        val key = MemoryCache.Key(id)
        memoryCache?.remove(key)
    }

    val diskCache = imageLoader.diskCache
    diskCache?.let { cache ->
        imageIds.forEach { id ->
            cache.remove(id)
        }
    }
}
