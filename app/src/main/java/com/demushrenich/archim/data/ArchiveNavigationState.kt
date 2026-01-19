package com.demushrenich.archim.data

import com.demushrenich.archim.domain.ImageItem
import androidx.compose.runtime.*
import com.demushrenich.archim.domain.utils.ArchiveNode
import com.demushrenich.archim.domain.utils.ArchiveTreeBuilder

data class ArchiveLevel(
    val path: String,
    val displayName: String,
    val entries: List<ImageItem>
)

data class ScrollPosition(val index: Int, val offset: Int)

class ArchiveNavigationState(private val allItems: List<ImageItem>) {
    private val _levels = mutableStateMapOf<Int, ArchiveLevel>()
    private var _currentLevel by mutableIntStateOf(0)
    val allImages: List<ImageItem> = createOrderedImageList(allItems)

    // Построим дерево один раз при инициализации
    private val tree: ArchiveNode = ArchiveTreeBuilder.buildTree(allItems)

    fun getCurrentLevel(): ArchiveLevel? = _levels[_currentLevel]

    fun setRootLevel(skipAutoNavigation: Boolean = false) {
        val entries = ArchiveTreeBuilder.getChildrenAtPath(tree, "")
        _levels[0] = ArchiveLevel(
            path = "",
            displayName = "/",
            entries = entries
        )
        _currentLevel = 0

        if (!skipAutoNavigation) {
            val folders = entries.filter { it.isFolder }
            val images = entries.filter { !it.isFolder }

            if (folders.size == 1 && images.isEmpty()) {
                navigateToNext(folders.first(), skipAutoNavigation = false)
            }
        }
    }

    fun navigateToNext(folder: ImageItem, skipAutoNavigation: Boolean = false) {
        val entries = ArchiveTreeBuilder.getChildrenAtPath(tree, folder.archivePath)
        val nextLevel = _currentLevel + 1
        _levels[nextLevel] = ArchiveLevel(
            path = folder.archivePath,
            displayName = folder.fileName,
            entries = entries
        )
        _currentLevel = nextLevel

        if (!skipAutoNavigation) {
            val folders = entries.filter { it.isFolder }
            val images = entries.filter { !it.isFolder }

            if (folders.size == 1 && images.isEmpty()) {
                navigateToNext(folders.first(), skipAutoNavigation = false)
            }
        }
    }

    fun navigateBack(): Boolean {
        if (_currentLevel > 0) {
            val keysToRemove = _levels.keys.filter { it > _currentLevel - 1 }
            keysToRemove.forEach { _levels.remove(it) }
            _currentLevel--
            return true
        }
        return false
    }

    fun canNavigateBack(): Boolean = _currentLevel > 0

    private fun createOrderedImageList(items: List<ImageItem>): List<ImageItem> {
        val result = mutableListOf<ImageItem>()

        fun getDepth(path: String): Int = if (path.isEmpty()) 0 else path.count { it == '/' }

        val imagesByLevel = items.filter { !it.isFolder }
            .groupBy { getDepth(it.archivePath) }
            .toSortedMap()
        imagesByLevel.forEach { (_, levelImages) ->
            result.addAll(levelImages.sortedBy { it.id })
        }

        return result
    }

    fun findPathToImage(imageId: String): List<String>? {
        val target = allItems.find { it.id == imageId } ?: return null
        val pathParts = target.archivePath.split("/")
            .filter { it.isNotBlank() }

        return pathParts
    }

    fun navigateToPath(path: List<String>) {
        setRootLevel(skipAutoNavigation = true)
        var currentEntries = getCurrentLevel()?.entries ?: return

        for (folderName in path) {
            val folder = currentEntries.firstOrNull { it.isFolder && it.fileName == folderName }
                ?: return
            navigateToNext(folder, skipAutoNavigation = true)
            currentEntries = getCurrentLevel()?.entries ?: return
        }
    }
}