package com.demushrenich.archim.data

data class PreviewInfo(
    val archiveUri: String? = null,
    val archiveUris: Set<String>? = null,
    val previewPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val readingProgress: ReadingProgress? = null
) {

    fun getAllUris(): Set<String> {
        val uris = (archiveUris ?: emptySet()).toMutableSet()
        if (archiveUri != null) uris.add(archiveUri)
        return uris
    }
}

data class ReadingProgress(
    val currentIndex: Int,
    val totalImages: Int,
    val lastReadTimestamp: Long
) {

    fun getProgressPercentage(): Float {
        if (totalImages == 0) return 0f
        return (currentIndex.toFloat() / totalImages.toFloat()).coerceIn(0f, 1f)
    }

    fun isCompleted(): Boolean {
        return currentIndex >= totalImages - 1
    }
}