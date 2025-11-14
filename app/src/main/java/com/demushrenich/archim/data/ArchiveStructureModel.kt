package com.demushrenich.archim.data

import com.demushrenich.archim.domain.SortType

data class ArchiveLevelData(
    val path: String,
    val imageIds: List<String>,
    val sortType: SortType = SortType.NAME_ASC,
    val readCount: Int = 0,
    val lastImageIdLevel: String? = null
)

data class ArchiveStructure(
    val archiveUri: String,
    val fileName: String,
    val fileSize: Long,
    val totalImages: Int,
    val levels: List<ArchiveLevelData>,
    val lastImageId: String? = null,
    val lastModified: Long = System.currentTimeMillis()
) {
    fun getTotalReadCount(): Int {
        return levels.sumOf { it.readCount }
    }

    fun getProgressPercentage(): Float {
        if (totalImages == 0) return 0f
        return (getTotalReadCount().toFloat() / totalImages.toFloat()).coerceIn(0f, 1f)
    }

    fun isCompleted(): Boolean {
        return getTotalReadCount() >= totalImages
    }
}