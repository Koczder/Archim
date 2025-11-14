package com.demushrenich.archim.data.utils

import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.SortCategory
import com.demushrenich.archim.domain.SortType

object SortingUtils {
    fun sortImages(images: List<ImageItem>, sortType: SortType): List<ImageItem> {
        return when (sortType) {
            SortType.NAME_ASC -> images.sortedBy { it.fileName.lowercase() }
            SortType.NAME_DESC -> images.sortedByDescending { it.fileName.lowercase() }
            SortType.DATE_ASC -> {
                images.sortedWith(compareBy<ImageItem> {
                    if (it.creationTime == 0L) Long.MAX_VALUE else it.creationTime
                }.thenBy { it.fileName.lowercase() })
            }
            SortType.DATE_DESC -> {
                images.sortedWith(compareByDescending<ImageItem> {
                    if (it.creationTime == 0L) Long.MIN_VALUE else it.creationTime
                }.thenBy { it.fileName.lowercase() })
            }
            else -> images
        }
    }

    fun sortArchives(archives: List<ArchiveInfo>, sortType: SortType): List<ArchiveInfo> {
        return when (sortType) {
            SortType.NAME_ASC -> archives.sortedBy { it.displayName.lowercase() }
            SortType.NAME_DESC -> archives.sortedByDescending { it.displayName.lowercase() }

            SortType.DATE_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    if (it.lastModified == 0L) Long.MAX_VALUE else it.lastModified
                }.thenBy { it.displayName.lowercase() })
            }
            SortType.DATE_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    if (it.lastModified == 0L) Long.MIN_VALUE else it.lastModified
                }.thenBy { it.displayName.lowercase() })
            }

            SortType.PROGRESS_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    val progress = it.readingProgress
                    if (progress == null || progress.totalImages == 0) {
                        -1f
                    } else {
                        progress.currentIndex.toFloat() / progress.totalImages
                    }
                }.thenBy { it.displayName.lowercase() })
            }
            SortType.PROGRESS_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    val progress = it.readingProgress
                    if (progress == null || progress.totalImages == 0) {
                        -1f
                    } else {
                        progress.currentIndex.toFloat() / progress.totalImages
                    }
                }.thenBy { it.displayName.lowercase() })
            }

            SortType.LAST_OPENED_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    val timestamp = it.readingProgress?.lastReadTimestamp
                    if (timestamp == null || timestamp == 0L) {
                        Long.MAX_VALUE
                    } else {
                        timestamp
                    }
                }.thenBy { it.displayName.lowercase() })
            }
            SortType.LAST_OPENED_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    val timestamp = it.readingProgress?.lastReadTimestamp
                    if (timestamp == null || timestamp == 0L) {
                        Long.MIN_VALUE
                    } else {
                        timestamp
                    }
                }.thenBy { it.displayName.lowercase() })
            }
        }
    }

    fun toggleSortType(currentSortType: SortType, clickedType: SortCategory): SortType {
        return when (clickedType) {
            SortCategory.NAME -> {
                when (currentSortType) {
                    SortType.NAME_ASC -> SortType.NAME_DESC
                    SortType.NAME_DESC -> SortType.NAME_ASC
                    else -> SortType.NAME_ASC
                }
            }
            SortCategory.DATE -> {
                when (currentSortType) {
                    SortType.DATE_ASC -> SortType.DATE_DESC
                    SortType.DATE_DESC -> SortType.DATE_ASC
                    else -> SortType.DATE_DESC
                }
            }
            SortCategory.PROGRESS -> {
                when (currentSortType) {
                    SortType.PROGRESS_ASC -> SortType.PROGRESS_DESC
                    SortType.PROGRESS_DESC -> SortType.PROGRESS_ASC
                    else -> SortType.PROGRESS_DESC
                }
            }
            SortCategory.LAST_OPENED -> {
                when (currentSortType) {
                    SortType.LAST_OPENED_ASC -> SortType.LAST_OPENED_DESC
                    SortType.LAST_OPENED_DESC -> SortType.LAST_OPENED_ASC
                    else -> SortType.LAST_OPENED_DESC
                }
            }
        }
    }
}