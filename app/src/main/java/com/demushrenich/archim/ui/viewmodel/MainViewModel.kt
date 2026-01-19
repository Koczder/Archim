package com.demushrenich.archim.ui.viewmodel

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.demushrenich.archim.*
import com.demushrenich.archim.data.*
import com.demushrenich.archim.domain.*
import androidx.core.net.toUri
import com.demushrenich.archim.data.managers.ArchiveStructureManager
import com.demushrenich.archim.data.managers.DirectoryManager
import com.demushrenich.archim.data.managers.PreviewManager
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.domain.utils.PasswordRequiredException
import com.demushrenich.archim.domain.utils.SortingUtils
import com.demushrenich.archim.domain.utils.WrongPasswordException
import com.demushrenich.archim.domain.utils.clearLargeArchiveCache
import com.demushrenich.archim.domain.utils.copyImageToClipboard
import com.demushrenich.archim.domain.utils.extractImagesFromArchive
import com.demushrenich.archim.domain.utils.generatePreviewFromImage
import com.demushrenich.archim.domain.utils.getFileExtension
import com.demushrenich.archim.domain.utils.isSupportedArchive
import com.demushrenich.archim.utils.clearArchiveImagesFromCache


class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var viewerImagesList: List<ImageItem> = emptyList()

    private val _directories = mutableStateListOf<DirectoryItem>()
    private val directoryContentViewModel = DirectoryContentViewModel()
    private val _navigationState = NavigationState()
    val navigationState: NavigationState = _navigationState

    var archiveNavState: ArchiveNavigationState? = null
        private set

    private val newRootDirectoryUris = mutableSetOf<String>()
    private var extractionJob: Job? = null
    private var settingsManager: SettingsManager? = null
    private var archiveStructureSaved = false

    private var currentArchiveFile: DocumentFile? = null


    private var currentArchiveStructure: ArchiveStructure? = null

    fun initializeSettings(context: Context) {
        if (settingsManager == null) {
            settingsManager = SettingsManager(context)
            settingsManager?.initializeLanguage()

            viewModelScope.launch {
                settingsManager?.currentLanguage?.collect { language ->
                    _uiState.value = _uiState.value.updateSettings { copy(currentLanguage = language) }
                }
            }

            viewModelScope.launch {
                settingsManager?.readingDirection?.collect { direction ->
                    _uiState.value = _uiState.value.updateSettings { copy(readingDirection = direction) }
                }
            }

            viewModelScope.launch {
                settingsManager?.previewGenerationMode?.collect { mode ->
                    _uiState.value = _uiState.value.updateSettings { copy(previewGenerationMode = mode) }
                }
            }

            viewModelScope.launch {
                settingsManager?.backgroundMode?.collect { mode ->
                    _uiState.value = _uiState.value.updateSettings { copy(backgroundMode = mode) }
                }
            }

            viewModelScope.launch {
                settingsManager?.archiveCornerStyle?.collect { style ->
                    _uiState.value = _uiState.value.updateSettings { copy(archiveCornerStyle = style) }
                }
            }

            viewModelScope.launch {
                settingsManager?.imageCornerStyle?.collect { style ->
                    _uiState.value = _uiState.value.updateSettings { copy(imageCornerStyle = style) }
                }
            }

            viewModelScope.launch {
                settingsManager?.archiveOpenMode?.collect { mode ->
                    _uiState.value = _uiState.value.updateSettings { copy(archiveOpenMode = mode) }
                }
            }
        }
    }

    fun showSettings() {
        _uiState.value = _uiState.value.updateSettings { copy(showSettings = true) }
    }

    fun hideSettings() {
        _uiState.value = _uiState.value.updateSettings { copy(showSettings = false) }
    }

    fun changeArchiveOpenMode(mode: ArchiveOpenMode) {
        settingsManager?.setArchiveOpenMode(mode)
    }

    fun changeLanguage(language: Language) {
        settingsManager?.setLanguage(language)
    }

    fun changePreviewGenerationMode(mode: PreviewGenerationMode) {
        settingsManager?.setPreviewGenerationMode(mode)
    }

    fun changeReadingDirection(direction: ReadingDirection) {
        settingsManager?.setReadingDirection(direction)
    }

    fun loadSavedDirectories(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedDirectories = DirectoryManager.loadSavedDirectories(context)
            withContext(Dispatchers.Main) {
                _directories.clear()
                _directories.addAll(savedDirectories)
                _navigationState.setRootLevel(savedDirectories)
            }
        }
    }

    fun changeArchiveCornerStyle(style: CornerStyle) {
        settingsManager?.setArchiveCornerStyle(style)
    }

    fun changeImageCornerStyle(style: CornerStyle) {
        settingsManager?.setImageCornerStyle(style)
    }

    fun getDirectoryContentViewModel(): DirectoryContentViewModel = directoryContentViewModel

    fun handleDirectoryPicked(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {}

        viewModelScope.launch(Dispatchers.IO) {
            DirectoryManager.addDirectory(context, uri)?.let { added ->
                withContext(Dispatchers.Main) {
                    if (_directories.none { d -> d.uri == added.uri }) {
                        _directories.add(added)
                        newRootDirectoryUris.add(added.uri)
                        _navigationState.setRootLevel(_directories.toList())
                    }
                }
                loadSavedDirectories(context)
            }
        }
    }

    fun handleArchivePicked(context: Context, uri: Uri) {
        archiveStructureSaved = false
        currentArchiveStructure = null
        _uiState.value = _uiState.value.updateArchive { copy(currentArchiveUri = uri) }
        Log.d("ArchiveDebug", "handleArchivePicked: opening new archive $uri, old was ${currentArchiveFile?.uri}")
        loadArchive(context, uri, null)
    }

    fun showPasswordDialog() {
        _uiState.value = _uiState.value
            .updateArchive { copy(showPasswordDialog = true) }
            .updateLoading { copy(errorMessage = null) }
    }

    fun dismissPasswordDialog() {
        _uiState.value = _uiState.value.updateArchive {
            copy(showPasswordDialog = false, passwordError = false, passwordErrorMessage = "")
        }
    }

    fun confirmPassword(context: Context, password: String) {
        _uiState.value.archive.currentArchiveUri?.let { uri ->
            _uiState.value = _uiState.value.updateArchive {
                copy(showPasswordDialog = false, passwordError = false, passwordErrorMessage = "")
            }
            loadArchive(context, uri, password)
        }
    }

    fun cancelExtraction() {
        extractionJob?.cancel()
        _uiState.value = _uiState.value.updateLoading {
            copy(isLoading = false, extractionProgress = 0f, currentFileName = "")
        }
    }

    fun clearSelectedImage() {
        viewerImagesList = emptyList()
        _uiState.value = _uiState.value.updateImageView { copy(selectedImageIndex = null) }
    }

    fun setSelectedImageIndex(index: Int?) {
        _uiState.value = _uiState.value.updateImageView { copy(selectedImageIndex = index) }
    }

    fun cleanupOrphanedPreviews(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deletedCount = PreviewManager.cleanupOrphanedPreviews(context)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.previews_deleted, deletedCount),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                Log.d("MainViewModel", "Cleaned up files: $deletedCount")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.cleanup_error, e.message ?: ""),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                Log.e("MainViewModel", "Error cleaning up previews", e)
            }
        }
    }

    fun changeBackgroundMode(mode: BackgroundMode) {
        settingsManager?.setBackgroundMode(mode)
    }

    fun clearAllCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clearAllCache(context)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.cache_cleared),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cleanup error:", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.cache_clear_error),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    fun getViewerImagesList(): List<ImageItem> = viewerImagesList

    private fun getImageIdsForCurrentLevel(context: Context): List<ImageItem> {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return emptyList()
        val navState = archiveNavState ?: return emptyList()
        val currentLevel = navState.getCurrentLevel() ?: return emptyList()

        try {
            val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: return emptyList()
            val fileSize = documentFile.length()

            if (fileSize <= 0) return emptyList()

            val structure = currentArchiveStructure
                ?: ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)
                ?: return emptyList()

            currentArchiveStructure = structure

            val levelData = structure.levels.find { it.path == currentLevel.path }
                ?: return emptyList()

            val allImages = navState.allImages
            val orderedImages = levelData.imageIds.mapNotNull { imageId ->
                allImages.find { it.id == imageId && !it.isFolder }
            }

            Log.d("MainViewModel", "Loaded ${orderedImages.size} images from JSON for level: ${currentLevel.path}")
            return orderedImages

        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading imageIds from JSON", e)
            return emptyList()
        }
    }

    private fun saveArchiveStructure(context: Context) {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return
        val navState = archiveNavState ?: return

        try {
            val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: archiveUri.lastPathSegment ?: return
            val fileSize = documentFile?.length() ?: 0L

            if (fileSize > 0) {
                ArchiveStructureManager.saveArchiveStructure(
                    context = context,
                    archiveUri = archiveUri.toString(),
                    fileName = fileName,
                    fileSize = fileSize,
                    archiveNavState = navState
                )

                currentArchiveStructure = ArchiveStructureManager.loadArchiveStructure(
                    context, fileName, fileSize
                )

                Log.d("MainViewModel", "Archive structure updated for $fileName")
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving archive structure", e)
        }
    }

    fun changeSortType(sortCategory: SortCategory, context: Context) {
        val newSortType = SortingUtils.toggleSortType(_uiState.value.imageView.sortType, sortCategory)
        viewModelScope.launch {
            val currentImages = _uiState.value.imageView.images
            val sortedImages = withContext(Dispatchers.Default) {
                SortingUtils.sortImages(currentImages, newSortType)
            }

            _uiState.value = _uiState.value.updateImageView {
                copy(sortType = newSortType, images = sortedImages)
            }

            withContext(Dispatchers.IO) {
                saveSortTypeAndOrderForCurrentLevel(context, newSortType, sortedImages)
            }
        }
    }

    private fun handleArchiveLoaded(context: Context, uri: Uri, images: List<ImageItem>) {
        val archiveNavState = ArchiveNavigationState(images)
        archiveNavState.setRootLevel()

        var currentEntries = archiveNavState.getCurrentLevel()?.entries ?: emptyList()
        var initialSelectedIndex: Int? = null
        var shouldOpenViewer = false

        try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            val fileName = documentFile?.name ?: uri.lastPathSegment ?: ""
            val fileSize = documentFile?.length() ?: 0L

            if (fileSize > 0) {
                val totalImages = archiveNavState.allImages.size
                PreviewManager.saveReadingProgressForPreview(
                    context = context,
                    archiveUri = uri.toString(),
                    fileName = fileName,
                    fileSize = fileSize,
                    currentIndex = 0,
                    totalImages = totalImages
                )

                val structure = ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)

                if (structure != null && structure.lastImageId != null) {
                    val targetImage = archiveNavState.allImages.find { it.id == structure.lastImageId }
                    if (targetImage != null) {
                        val pathToTarget = archiveNavState.findPathToImage(targetImage.id)
                        if (pathToTarget != null) {
                            archiveNavState.navigateToPath(pathToTarget)
                            currentEntries = archiveNavState.getCurrentLevel()?.entries ?: emptyList()

                            val currentLevel = archiveNavState.getCurrentLevel()
                            val levelData = structure.levels.find { it.path == currentLevel?.path }
                            val sortType = levelData?.sortType ?: SortType.NAME_ASC

                            val sortedEntries = SortingUtils.sortImages(currentEntries, sortType)

                            val openMode = _uiState.value.settings.archiveOpenMode
                            if (openMode == ArchiveOpenMode.CONTINUE) {
                                shouldOpenViewer = true
                                val filesOnly = sortedEntries.filter { !it.isFolder }
                                initialSelectedIndex = filesOnly.indexOfFirst { it.id == targetImage.id }
                                viewerImagesList = filesOnly
                            } else {
                                initialSelectedIndex = sortedEntries.indexOfFirst { it.id == targetImage.id }
                            }

                            currentEntries = sortedEntries
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading archive position", e)
        }

        this.archiveNavState = archiveNavState

        _uiState.value = _uiState.value
            .updateImageView {
                copy(
                    images = currentEntries,
                    sortType = SortType.NAME_ASC,
                    selectedImageIndex = if (shouldOpenViewer) initialSelectedIndex else null
                )
            }
            .updateLoading {
                copy(isLoading = false, extractionProgress = 1f, currentFileName = "Done")
            }
    }

    private fun loadSortTypeForCurrentLevel(context: Context?): SortType? {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return null
        val navState = archiveNavState ?: return null
        val currentLevel = navState.getCurrentLevel() ?: return null
        val structure = currentArchiveStructure ?: run {
            if (context == null) return null

            try {
                val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
                val fileName = documentFile?.name ?: return null
                val fileSize = documentFile.length()

                if (fileSize <= 0) return null

                ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)
                    .also { currentArchiveStructure = it }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading sort type", e)
                null
            }
        } ?: return null

        val levelData = structure.levels.find { it.path == currentLevel.path }
        return levelData?.sortType
    }

    private fun applyJsonSortToCurrentLevel(context: Context, fileName: String, fileSize: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val navState = archiveNavState ?: return@launch
                val structure = ArchiveStructureManager.loadArchiveStructure(context, fileName, fileSize)
                val currentLevel = navState.getCurrentLevel() ?: return@launch
                val level = structure?.levels?.find { it.path == currentLevel.path } ?: return@launch

                withContext(Dispatchers.Main) {
                    var updatedImages = _uiState.value.imageView.images

                    val updatedSortType = level.sortType

                    if (level.imageIds.isNotEmpty()) {
                        updatedImages = updatedImages.sortedBy { imageItem ->
                            level.imageIds.indexOf(imageItem.id)
                        }
                    }

                    _uiState.value = _uiState.value.updateImageView {
                        copy(images = updatedImages, sortType = updatedSortType)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "applyJsonSortToCurrentLevel failed: ${e.message}", e)
            }
        }
    }

    private fun saveSortTypeAndOrderForCurrentLevel(
        context: Context,
        sortType: SortType,
        sortedImages: List<ImageItem>
    ) {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return
        val navState = archiveNavState ?: return
        val currentLevel = navState.getCurrentLevel() ?: return

        try {
            val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: return
            val fileSize = documentFile.length()

            if (fileSize <= 0) return

            val imageIds = sortedImages.filter { !it.isFolder }.map { it.id }

            ArchiveStructureManager.saveArchiveStructure(
                context = context,
                archiveUri = archiveUri.toString(),
                fileName = fileName,
                fileSize = fileSize,
                archiveNavState = navState,
                currentSortType = sortType,
                currentLevelPath = currentLevel.path
            )

            ArchiveStructureManager.updateLevelImageIds(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                levelPath = currentLevel.path,
                imageIds = imageIds
            )

            currentArchiveStructure = ArchiveStructureManager.loadArchiveStructure(
                context, fileName, fileSize
            )

            Log.d("MainViewModel", "Sort type and order saved for level: ${currentLevel.path}")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving sort type and order", e)
        }
    }

    fun makePreview(context: Context, imageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = _uiState.value.imageView.images.filter { !it.isFolder }

                if (imageIndex < files.size && imageIndex >= 0) {
                    val selectedImage = files[imageIndex]
                    val archiveUri = _uiState.value.archive.currentArchiveUri
                    if (archiveUri != null) {
                        generatePreviewFromImage(context, selectedImage, archiveUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error making preview", e)
            }
        }
    }

    fun copyImage(context: Context, imageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = _uiState.value.imageView.images.filter { !it.isFolder }

                if (imageIndex < files.size && imageIndex >= 0) {
                    val selectedImage = files[imageIndex]
                    copyImageToClipboard(context, selectedImage)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error copying image", e)
            }
        }
    }

    fun deleteDirectory(context: Context, item: DirectoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (DirectoryManager.removeDirectory(context, item.uri)) {
                withContext(Dispatchers.Main) {
                    _directories.remove(item)
                    newRootDirectoryUris.remove(item.uri)
                    directoryContentViewModel.clearHandledDirectory(item.uri)
                    _navigationState.setRootLevel(_directories.toList())
                }
                loadSavedDirectories(context)
            }
        }
    }

    fun getNewRootDirectoryUris(): Set<String> = newRootDirectoryUris
    fun removeNewDirectoryUri(uri: String) { newRootDirectoryUris.remove(uri) }
    fun canNavigateBackInArchive(): Boolean = archiveNavState?.canNavigateBack() == true

    private fun loadArchive(context: Context, uri: Uri, password: String?) {
        Log.d("ArchiveDebug", "loadArchive: setting currentArchiveFile to ${uri}")
        archiveStructureSaved = false
        currentArchiveStructure = null
        currentArchiveFile = DocumentFile.fromSingleUri(context, uri)
        extractionJob?.cancel()
        viewerImagesList = emptyList()
        archiveNavState = null
        currentArchiveStructure = null
        currentArchiveFile = null
        extractionJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.updateLoading {
                    copy(isLoading = true, errorMessage = null, extractionProgress = 0f, currentFileName = "")
                }
            }

            try {
                if (!isSupportedArchive(getFileExtension(uri))) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.updateLoading {
                            copy(errorMessage = context.getString(R.string.unsupported_archive_format), isLoading = false)
                        }
                    }
                    return@launch
                }

                val images = extractImagesFromArchive(
                    context, uri, password
                ) { progress, fileName ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.value = _uiState.value.updateLoading {
                            copy(
                                extractionProgress = progress,
                                currentFileName = fileName,
                                isLoading = true
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (images.isEmpty()) {
                        _uiState.value = _uiState.value.updateLoading {
                            copy(errorMessage = context.getString(R.string.no_images_in_archive), isLoading = false)
                        }
                    } else {
                        handleArchiveLoaded(context, uri, images)

                        try {
                            val documentFile = DocumentFile.fromSingleUri(context, uri)
                            val fileName = documentFile?.name ?: uri.lastPathSegment ?: ""
                            val fileSize = documentFile?.length() ?: 0L
                            applyJsonSortToCurrentLevel(context, fileName, fileSize)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to apply sort after load: ${e.message}", e)
                        }
                    }
                }

            } catch (e: PasswordRequiredException) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateArchive { copy(showPasswordDialog = true) }
                        .updateLoading { copy(isLoading = false, errorMessage = null) }
                }
            } catch (e: WrongPasswordException) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateArchive {
                        copy(showPasswordDialog = true, passwordError = true, passwordErrorMessage = context.getString(R.string.invalid_password))
                    }.updateLoading { copy(isLoading = false) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateLoading {
                        copy(errorMessage = context.getString(R.string.archive_extraction_error, e.message ?: ""), isLoading = false)
                    }
                }
                e.printStackTrace()
            }
        }
    }

    fun openDirectory(context: Context, item: DirectoryItem, isNewDirectory: Boolean) {
        viewModelScope.launch {
            try {
                navigationState.setLoading(true)

                val result = withContext(Dispatchers.IO) {
                    val currentUri = item.uri.toUri()
                    val doc = DocumentFile.fromTreeUri(context, currentUri)
                    val (folders, archives) = DirectoryManager.listChildren(context, currentUri)

                    Triple(doc?.name ?: context.getString(R.string.default_folder_name), folders, archives)
                }

                if (isNewDirectory) {
                    removeNewDirectoryUri(item.uri)
                }

                navigationState.setLoading(false)
                navigationState.navigateToNext(
                    uri = item.uri,
                    displayName = result.first,
                    folders = result.second,
                    archives = result.third,
                    isNewDirectory = isNewDirectory
                )
            } catch (e: Exception) {
                navigationState.setLoading(false)
                navigationState.setError(context.getString(R.string.loading_error, e.message ?: ""))
            }
        }
    }

    fun openSubdirectory(context: Context, folder: DirectoryItem) {
        viewModelScope.launch {
            try {
                navigationState.setLoading(true)

                val result = withContext(Dispatchers.IO) {
                    val currentUri = folder.uri.toUri()
                    val doc = DocumentFile.fromTreeUri(context, currentUri)
                    val (folders, archives) = DirectoryManager.listChildren(context, currentUri)

                    Triple(doc?.name ?: context.getString(R.string.default_folder_name), folders, archives)
                }

                navigationState.setLoading(false)
                navigationState.navigateToNext(
                    uri = folder.uri,
                    displayName = result.first,
                    folders = result.second,
                    archives = result.third,
                    isNewDirectory = false
                )
            } catch (e: Exception) {
                navigationState.setLoading(false)
                navigationState.setError(context.getString(R.string.loading_error, e.message ?: ""))
            }
        }
    }

    fun continueReading(globalIndex: Int, context: Context) {
        viewModelScope.launch {
            val nav = archiveNavState ?: return@launch

            if (globalIndex < 0 || globalIndex >= nav.allImages.size) {
                Log.w("MainViewModel", "Invalid globalIndex: $globalIndex")
                return@launch
            }

            val targetItem = nav.allImages[globalIndex]
            val targetId = targetItem.id

            Log.d("MainViewModel", "continueReading: targetId=$targetId, globalIndex=$globalIndex")

            val pathToTarget = nav.findPathToImage(targetId) ?: run {
                Log.w("MainViewModel", "Path not found for imageId: $targetId")
                return@launch
            }

            Log.d("MainViewModel", "continueReading: pathToTarget=$pathToTarget")

            nav.navigateToPath(pathToTarget)

            val currentEntries = nav.getCurrentLevel()?.entries ?: emptyList()
            Log.d("MainViewModel", "continueReading: currentEntries size=${currentEntries.size}")

            withContext(Dispatchers.IO) {
                val sortType = loadSortTypeForCurrentLevel(context) ?: SortType.NAME_ASC
                val sortedImages = SortingUtils.sortImages(currentEntries, sortType)

                Log.d("MainViewModel", "continueReading: sortType=$sortType, sortedImages size=${sortedImages.size}")
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateImageView {
                        copy(
                            images = sortedImages,
                            sortType = sortType,
                            selectedImageIndex = null
                        )
                    }
                }

                val imagesForViewer = getImageIdsForCurrentLevel(context)

                val filesOnly = imagesForViewer.ifEmpty {
                    sortedImages.filter { !it.isFolder }
                }

                val sortedIndex = filesOnly.indexOfFirst { it.id == targetId }

                Log.d("MainViewModel", "continueReading: filesOnly size=${filesOnly.size}, sortedIndex=$sortedIndex")

                withContext(Dispatchers.Main) {
                    if (sortedIndex >= 0) {
                        viewerImagesList = filesOnly
                        _uiState.value = _uiState.value.updateImageView {
                            copy(selectedImageIndex = sortedIndex)
                        }
                        Log.d("MainViewModel", "continueReading: opened viewer at index $sortedIndex")
                    }
                }
            }
        }
    }

    fun selectImage(index: Int, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentImages = _uiState.value.imageView.images
            if (currentImages.isEmpty()) {
                Log.w("MainViewModel", "selectImage: no images")
                return@launch
            }

            val filesOnly = currentImages.filter { !it.isFolder }

            if (index < 0 || index >= filesOnly.size) {
                Log.w("MainViewModel", "selectImage: invalid index: $index for ${filesOnly.size} files")
                return@launch
            }

            val selectedImage = filesOnly[index]
            Log.d("MainViewModel", "selectImage: index=$index, imageId=${selectedImage.id}")

            val imagesForViewer = getImageIdsForCurrentLevel(context)

            if (imagesForViewer.isEmpty()) {
                viewerImagesList = filesOnly
                val indexInFiles = filesOnly.indexOfFirst { it.id == selectedImage.id }

                Log.d("MainViewModel", "selectImage: using filesOnly, viewerIndex=$indexInFiles")

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateImageView {
                        copy(
                            selectedImageIndex = if (indexInFiles >= 0) indexInFiles else 0
                        )
                    }
                }
            } else {
                viewerImagesList = imagesForViewer

                val indexInJsonList = imagesForViewer.indexOfFirst { it.id == selectedImage.id }

                Log.d("MainViewModel", "selectImage: using JSON list, viewerIndex=$indexInJsonList")

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateImageView {
                        copy(
                            selectedImageIndex = if (indexInJsonList >= 0) indexInJsonList else 0
                        )
                    }
                }
            }

            saveArchiveStructure(context)
            updateLastImageId(context, selectedImage.id)
            updateLastImageIdLevel(context, selectedImage.id)
            Log.d("MainViewModel", "selectImage: completed, lastImageId and lastImageIdLevel updated to ${selectedImage.id}")
        }
    }

    private fun updateLastImageIdLevel(context: Context, imageId: String) {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return
        val navState = archiveNavState ?: return
        val currentLevel = navState.getCurrentLevel() ?: return

        try {
            val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: return
            val fileSize = documentFile.length()

            if (fileSize <= 0) return

            ArchiveStructureManager.updateLastImageIdLevel(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                levelPath = currentLevel.path,
                lastImageIdLevel = imageId
            )

            currentArchiveStructure = ArchiveStructureManager.loadArchiveStructure(
                context, fileName, fileSize
            )

            Log.d("MainViewModel", "Updated lastImageIdLevel to: $imageId for level: ${currentLevel.path}")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error updating lastImageIdLevel", e)
        }
    }

    private fun updateLastImageId(context: Context, imageId: String) {
        val archiveUri = _uiState.value.archive.currentArchiveUri ?: return

        try {
            val documentFile = currentArchiveFile ?: DocumentFile.fromSingleUri(context, archiveUri)
            val fileName = documentFile?.name ?: return
            val fileSize = documentFile.length()

            if (fileSize <= 0) return

            ArchiveStructureManager.updateLastImageId(
                context = context,
                fileName = fileName,
                fileSize = fileSize,
                lastImageId = imageId
            )

            currentArchiveStructure = ArchiveStructureManager.loadArchiveStructure(
                context, fileName, fileSize
            )

            Log.d("MainViewModel", "Updated lastImageId to: $imageId")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error updating lastImageId", e)
        }
    }

    fun navigateBackInGrid(context: Context) {
        if (archiveNavState?.canNavigateBack() == true) {
            archiveNavState?.navigateBack()
            val currentEntries = archiveNavState?.getCurrentLevel()?.entries ?: emptyList()

            viewModelScope.launch(Dispatchers.IO) {
                val sortType = loadSortTypeForCurrentLevel(context) ?: SortType.NAME_ASC
                val sortedImages = SortingUtils.sortImages(currentEntries, sortType)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateImageView {
                        copy(images = sortedImages, selectedImageIndex = null, sortType = sortType)
                    }
                }
            }
        } else {
            val imageIds = archiveNavState?.allImages?.map { it.id } ?: emptyList()
            if (imageIds.isNotEmpty()) {
                clearArchiveImagesFromCache(context, imageIds)
                Log.d("MainViewModel", "Cleared ${imageIds.size} images from cache")
            }

            archiveNavState = null
            archiveStructureSaved = false
            currentArchiveStructure = null
            currentArchiveFile = null
            clearLargeArchiveCache(context)
            _uiState.value = _uiState.value.updateImageView {
                copy(images = emptyList(), selectedImageIndex = null, sortType = SortType.NAME_ASC)
            }
        }
    }

    fun openFolderInArchive(context: Context, folderItem: ImageItem) {
        viewModelScope.launch(Dispatchers.IO) {
            archiveNavState?.let { navState ->
                navState.navigateToNext(folderItem)
                val currentEntries = navState.getCurrentLevel()?.entries ?: emptyList()

                val sortType = loadSortTypeForCurrentLevel(context) ?: SortType.NAME_ASC
                val sortedImages = SortingUtils.sortImages(currentEntries, sortType)

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.updateImageView {
                        copy(images = sortedImages, sortType = sortType)
                    }
                }
            }
        }
    }
}