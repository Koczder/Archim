package com.demushrenich.archim.domain.utils

import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.SortCategory
import com.demushrenich.archim.domain.SortType

object SortingUtils {

    private val naturalOrderComparator = Comparator<String> { a, b ->
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]

            if (ca.isDigit() && cb.isDigit()) {
                var iEnd = i
                while (iEnd < a.length && a[iEnd].isDigit()) iEnd++
                var jEnd = j
                while (jEnd < b.length && b[jEnd].isDigit()) jEnd++

                val numA = a.substring(i, iEnd).trimStart('0').ifEmpty { "0" }
                val numB = b.substring(j, jEnd).trimStart('0').ifEmpty { "0" }

                val cmp = if (numA.length != numB.length) {
                    numA.length - numB.length
                } else {
                    numA.compareTo(numB)
                }
                if (cmp != 0) return@Comparator cmp

                i = iEnd
                j = jEnd
            } else {
                if (ca != cb) return@Comparator ca.compareTo(cb)
                i++
                j++
            }
        }
        (a.length - i) - (b.length - j)
    }

    private fun naturalSortKey(name: String): String = name.lowercase()

    private fun String.naturalCompareTo(other: String): Int =
        naturalOrderComparator.compare(naturalSortKey(this), naturalSortKey(other))

    fun <T> sortByName(items: List<T>, ascending: Boolean, nameSelector: (T) -> String): List<T> {
        val comparator = Comparator<T> { a, b -> nameSelector(a).naturalCompareTo(nameSelector(b)) }
        return items.sortedWith(if (ascending) comparator else comparator.reversed())
    }

    fun sortImages(images: List<ImageItem>, sortType: SortType): List<ImageItem> {
        return when (sortType) {
            SortType.NAME_ASC -> images.sortedWith(compareBy(naturalOrderComparator) { naturalSortKey(it.fileName) })
            SortType.NAME_DESC -> images.sortedWith(compareByDescending(naturalOrderComparator) { naturalSortKey(it.fileName) })
            SortType.DATE_ASC -> {
                images.sortedWith(compareBy<ImageItem> {
                    if (it.creationTime == 0L) Long.MAX_VALUE else it.creationTime
                }.thenComparator { a, b -> a.fileName.naturalCompareTo(b.fileName) })
            }
            SortType.DATE_DESC -> {
                images.sortedWith(compareByDescending<ImageItem> {
                    if (it.creationTime == 0L) Long.MIN_VALUE else it.creationTime
                }.thenComparator { a, b -> a.fileName.naturalCompareTo(b.fileName) })
            }
            else -> images
        }
    }

    fun sortArchives(archives: List<ArchiveInfo>, sortType: SortType): List<ArchiveInfo> {
        return when (sortType) {
            SortType.NAME_ASC -> archives.sortedWith(compareBy(naturalOrderComparator) { naturalSortKey(it.displayName) })
            SortType.NAME_DESC -> archives.sortedWith(compareByDescending(naturalOrderComparator) { naturalSortKey(it.displayName) })

            SortType.DATE_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    if (it.lastModified == 0L) Long.MAX_VALUE else it.lastModified
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
            }
            SortType.DATE_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    if (it.lastModified == 0L) Long.MIN_VALUE else it.lastModified
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
            }

            SortType.PROGRESS_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    val progress = it.readingProgress
                    if (progress == null || progress.totalImages == 0) {
                        -1f
                    } else {
                        progress.currentIndex.toFloat() / progress.totalImages
                    }
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
            }
            SortType.PROGRESS_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    val progress = it.readingProgress
                    if (progress == null || progress.totalImages == 0) {
                        -1f
                    } else {
                        progress.currentIndex.toFloat() / progress.totalImages
                    }
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
            }

            SortType.LAST_OPENED_ASC -> {
                archives.sortedWith(compareBy<ArchiveInfo> {
                    val timestamp = it.readingProgress?.lastReadTimestamp
                    if (timestamp == null || timestamp == 0L) {
                        Long.MAX_VALUE
                    } else {
                        timestamp
                    }
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
            }
            SortType.LAST_OPENED_DESC -> {
                archives.sortedWith(compareByDescending<ArchiveInfo> {
                    val timestamp = it.readingProgress?.lastReadTimestamp
                    if (timestamp == null || timestamp == 0L) {
                        Long.MIN_VALUE
                    } else {
                        timestamp
                    }
                }.thenComparator { a, b -> a.displayName.naturalCompareTo(b.displayName) })
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