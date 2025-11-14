package com.demushrenich.archim.data

import androidx.compose.runtime.*



data class NavigationLevel(
    val uri: String,
    val displayName: String,
    val folders: List<DirectoryItem>,
    val archives: List<ArchiveInfo>,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isNewDirectory: Boolean = false
)

class NavigationState {
    private val _levels = mutableStateMapOf<Int, NavigationLevel>()
    private var _currentLevel by mutableIntStateOf(0)
    val currentLevel: Int get() = _currentLevel

    fun getCurrentLevel(): NavigationLevel? {
        return _levels[_currentLevel]
    }


    fun setRootLevel(directories: List<DirectoryItem>) {
        _levels[0] = NavigationLevel(
            uri = "",
            displayName = "/",
            folders = directories,
            archives = emptyList(),
            isLoading = false
        )
        _currentLevel = 0
    }

    fun navigateToNext(
        uri: String,
        displayName: String,
        folders: List<DirectoryItem>,
        archives: List<ArchiveInfo>,
        isNewDirectory: Boolean = false
    ) {
        val nextLevel = _currentLevel + 1
        _levels[nextLevel] = NavigationLevel(
            uri = uri,
            displayName = displayName,
            folders = folders,
            archives = archives,
            isLoading = false,
            isNewDirectory = isNewDirectory
        )
        _currentLevel = nextLevel
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

    fun updateCurrentLevel(
        folders: List<DirectoryItem>? = null,
        archives: List<ArchiveInfo>? = null,
        isLoading: Boolean? = null,
        error: String? = null
    ) {
        val current = _levels[_currentLevel] ?: return
        _levels[_currentLevel] = current.copy(
            folders = folders ?: current.folders,
            archives = archives ?: current.archives,
            isLoading = isLoading ?: current.isLoading,
            error = error ?: current.error
        )
    }

    fun setLoading(isLoading: Boolean) {
        updateCurrentLevel(isLoading = isLoading)
    }

    fun setError(error: String?) {
        updateCurrentLevel(error = error)
    }


    fun getBreadcrumbs(): List<Pair<Int, String>> {
        return (0.._currentLevel).mapNotNull { level ->
            _levels[level]?.let { level to it.displayName }
        }
    }


    fun navigateToLevel(targetLevel: Int): Boolean {
        if (targetLevel >= 0 && targetLevel <= _currentLevel && _levels.containsKey(targetLevel)) {
            val keysToRemove = _levels.keys.filter { it > targetLevel }
            keysToRemove.forEach { _levels.remove(it) }

            _currentLevel = targetLevel
            return true
        }
        return false
    }

    fun canNavigateBack(): Boolean {
        return _currentLevel > 0
    }


}