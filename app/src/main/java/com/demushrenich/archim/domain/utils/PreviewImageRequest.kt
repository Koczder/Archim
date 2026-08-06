package com.demushrenich.archim.domain.utils

import android.content.Context
import coil.request.ImageRequest
import java.io.File

fun buildPreviewImageRequest(context: Context, previewPath: String): ImageRequest {
    val cacheKey = previewCacheKey(previewPath)
    return ImageRequest.Builder(context)
        .data(File(previewPath))
        .memoryCacheKey(cacheKey)
        .diskCacheKey(cacheKey)
        .crossfade(false)
        .build()
}


fun previewCacheKey(previewPath: String): String {
    val file = File(previewPath)
    return "$previewPath:${file.lastModified()}:${file.length()}"
}