package com.demushrenich.archim.data.managers

import com.demushrenich.archim.domain.ImageItem

object ArchiveTreeBuilder {

    fun buildHierarchy(allItems: List<ImageItem>, currentPath: String): List<ImageItem> {
        val normalized = if (currentPath.isEmpty()) "" else "$currentPath/"

        val addedFolders = mutableSetOf<String>()
        val results = mutableListOf<ImageItem>()

        for (item in allItems) {
            if (item.archivePath.startsWith(normalized)) {
                val relative = item.archivePath.removePrefix(normalized)
                if (relative.isNotEmpty() && !relative.contains("/")) {
                    results.add(item.copy())
                    if (item.isFolder) {
                        addedFolders.add(item.archivePath)
                    }
                }
            }
        }

        for (item in allItems) {
            if (item.archivePath.startsWith(normalized)) {
                val relative = item.archivePath.removePrefix(normalized)

                if (relative.contains("/")) {
                    val folderName = relative.substringBefore("/")
                    val folderPath = normalized + folderName
                    if (!addedFolders.contains(folderPath)) {
                        addedFolders.add(folderPath)
                        results.add(
                            ImageItem(
                                fileName = folderName,
                                archivePath = folderPath,
                                isFolder = true
                            )
                        )
                    }
                }
            }
        }
        return results.sortedBy { it.fileName }
    }
}