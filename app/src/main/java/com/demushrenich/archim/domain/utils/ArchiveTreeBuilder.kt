package com.demushrenich.archim.domain.utils

import com.demushrenich.archim.domain.ImageItem

class ArchiveNode(
    val item: ImageItem?,
    val children: MutableMap<String, ArchiveNode> = mutableMapOf()
)

object ArchiveTreeBuilder {

    fun buildTree(allItems: List<ImageItem>): ArchiveNode {
        val root = ArchiveNode(item = null)

        for (item in allItems) {
            val parts = item.archivePath.split("/").filter { it.isNotEmpty() }
            var currentNode = root

            for (i in parts.indices) {
                val part = parts[i]

                if (!currentNode.children.containsKey(part)) {
                    val isLastPart = i == parts.lastIndex

                    if (isLastPart) {
                        currentNode.children[part] = ArchiveNode(item = item)
                    } else {
                        val folderPath = parts.subList(0, i + 1).joinToString("/")
                        val folderItem = ImageItem(
                            fileName = part,
                            archivePath = folderPath,
                            isFolder = true
                        )
                        currentNode.children[part] = ArchiveNode(item = folderItem)
                    }
                }

                currentNode = currentNode.children[part]!!
            }
        }

        return root
    }

    fun getChildrenAtPath(root: ArchiveNode, path: String): List<ImageItem> {
        if (path.isEmpty()) {
            return root.children.values
                .mapNotNull { it.item }
                .sortedBy { it.fileName }
        }

        val parts = path.split("/").filter { it.isNotEmpty() }
        var currentNode = root

        for (part in parts) {
            currentNode = currentNode.children[part] ?: return emptyList()
        }

        return currentNode.children.values
            .mapNotNull { it.item }
            .sortedBy { it.fileName }
    }
}