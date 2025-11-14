package com.demushrenich.archim.data

data class PreviewInfo(
    val archiveUri: String,
    val previewPath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val readingProgress: ReadingProgress? = null
)

data class ReadingProgress(
    val currentIndex: Int,
    val totalImages: Int,
    val lastReadTimestamp: Long
) {

    fun getProgressPercentage(): Float {
        return if (totalImages > 0) (currentIndex + 1).toFloat() / totalImages.toFloat() else 0f
    }

    fun isCompleted(): Boolean {
        return currentIndex >= totalImages - 1
    }
}