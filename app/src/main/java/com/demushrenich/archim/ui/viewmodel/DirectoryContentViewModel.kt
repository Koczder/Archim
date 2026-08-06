package com.demushrenich.archim.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demushrenich.archim.data.*
import com.demushrenich.archim.data.managers.ArchiveStructureManager
import com.demushrenich.archim.data.managers.DirectoryManager
import com.demushrenich.archim.data.managers.PreviewManager
import com.demushrenich.archim.domain.utils.SortingUtils
import com.demushrenich.archim.domain.utils.generatePreviewForArchive
import com.demushrenich.archim.domain.PreviewGenerationMode
import com.demushrenich.archim.domain.SortType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import com.demushrenich.archim.R
import com.demushrenich.archim.domain.utils.ArchiveFormats

class DirectoryContentViewModel : ViewModel() {

    companion object {
        private const val TAG = "DirectoryContentVM"
        private const val FILE_WRITE_RETRY_ATTEMPTS = 2
        private const val FILE_WRITE_RETRY_DELAY = 500L
    }

    private var _archivesWithPreviews by mutableStateOf(listOf<ArchiveInfo>())
    val archivesWithPreviews: List<ArchiveInfo> get() = _archivesWithPreviews

    private var _currentSortType by mutableStateOf<SortType?>(null)
    val currentSortType: SortType? get() = _currentSortType

    private var _currentDirectoryUri by mutableStateOf<String?>(null)

    private var _isGeneratingPreviews by mutableStateOf(false)
    val isGeneratingPreviews: Boolean get() = _isGeneratingPreviews

    private var _currentPreviewProgress by mutableStateOf("")
    val currentPreviewProgress: String get() = _currentPreviewProgress

    private var _loadedPreviewKeys by mutableStateOf(setOf<String>())
    val loadedPreviewKeys: Set<String> get() = _loadedPreviewKeys

    private var _shouldShowPreviewDialog by mutableStateOf(false)
    val shouldShowPreviewDialog: Boolean get() = _shouldShowPreviewDialog

    private val handledDirectories = mutableSetOf<String>()

    private val archivesUpdateMutex = Mutex()

    private var currentGenerationJob: Job? = null

    fun checkAndShowPreviewDialog(
        currentDirUri: String,
        isNewDirectory: Boolean,
        previewMode: PreviewGenerationMode = PreviewGenerationMode.DIALOG
    ) {
        Log.d(TAG, "checkAndShowPreviewDialog: currentDirUri=$currentDirUri, isNewDirectory=$isNewDirectory, previewMode=$previewMode")

        when (previewMode) {
            PreviewGenerationMode.DIALOG -> {
                Log.d(TAG, "Preview mode is DIALOG")
                if (isNewDirectory && !handledDirectories.contains(currentDirUri)) {
                    Log.d(TAG, "Showing preview dialog for new directory")
                    _shouldShowPreviewDialog = true
                    handledDirectories.add(currentDirUri)
                } else {
                    Log.d(TAG, "Not showing dialog - isNewDirectory=$isNewDirectory, already handled=${handledDirectories.contains(currentDirUri)}")
                }
            }
            PreviewGenerationMode.AUTO -> {
                Log.d(TAG, "Preview mode is AUTO - no dialog will be shown")
                if (isNewDirectory && !handledDirectories.contains(currentDirUri)) {
                    Log.d(TAG, "Starting auto preview generation for new directory")
                    handledDirectories.add(currentDirUri)
                }
            }
            PreviewGenerationMode.MANUAL -> {
                Log.d(TAG, "Preview mode is MANUAL - no dialog, no auto generation")
                if (isNewDirectory) {
                    handledDirectories.add(currentDirUri)
                }
            }
        }
    }

    fun checkAndAutoGeneratePreviews(
        context: Context,
        currentDirUri: String,
        isNewDirectory: Boolean,
        previewMode: PreviewGenerationMode,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)? = null
    ) {
        Log.d(TAG, "checkAndAutoGeneratePreviews: previewMode=$previewMode, isNewDirectory=$isNewDirectory")

        if (previewMode == PreviewGenerationMode.AUTO &&
            isNewDirectory &&
            !handledDirectories.contains(currentDirUri)) {

            Log.d(TAG, "Starting auto generation for directory: $currentDirUri")
            handledDirectories.add(currentDirUri)
            generatePreviewsForAllArchives(
                context = context,
                currentDirUri = currentDirUri,
                onUpdateArchives = onUpdateArchives
            )
        } else {
            Log.d(TAG, "Auto generation skipped - mode is not AUTO or directory already handled")
        }
    }

    fun hidePreviewDialog() {
        Log.d(TAG, "hidePreviewDialog called")
        _shouldShowPreviewDialog = false
    }

    fun clearHandledDirectory(directoryUri: String) {
        Log.d(TAG, "clearHandledDirectory: $directoryUri")
        handledDirectories.remove(directoryUri)
    }

    fun generatePreviewsForAllArchives(
        context: Context?,
        currentDirUri: String,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)? = null
    ) {
        Log.d(TAG, "generatePreviewsForAllArchives called for: $currentDirUri")

        if (context == null) {
            Log.e(TAG, "Context is null, cannot generate previews")
            return
        }

        currentGenerationJob?.cancel()

        currentGenerationJob = viewModelScope.launch {
            _isGeneratingPreviews = true
            try {
                _currentPreviewProgress = context.getString(R.string.preview_scanning_directory)
                Log.d(TAG, "Starting directory scan")

                val allArchivesRecursive = getAllArchivesWithProgress(
                    context = context,
                    dirUri = currentDirUri.toUri()
                )

                Log.d(TAG, "Scan complete, found ${allArchivesRecursive.size} total archives")

                val archivesToProcess = allArchivesRecursive.filter {
                    it.previewPath == null || !File(it.previewPath).exists()
                }

                Log.d(TAG, "Found ${archivesToProcess.size} archives to process")

                if (archivesToProcess.isEmpty()) {
                    _currentPreviewProgress = context.getString(R.string.preview_all_already_generated)
                    delay(2000)
                    return@launch
                }

                processArchivesInParallel(
                    context = context,
                    archives = archivesToProcess,
                    onUpdateArchives = onUpdateArchives
                )

                _currentPreviewProgress = context.getString(R.string.preview_generation_complete, archivesToProcess.size)
                delay(2000)

            } catch (e: CancellationException) {
                Log.d(TAG, "Preview generation cancelled")
                _currentPreviewProgress = context.getString(R.string.preview_generation_cancelled)
                delay(1000)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating previews", e)
                _currentPreviewProgress = context.getString(R.string.preview_generation_error, e.message ?: "")
                delay(3000)
            } finally {
                _isGeneratingPreviews = false
                _currentPreviewProgress = ""
                Log.d(TAG, "Preview generation finished")
            }
        }
    }

    private suspend fun processArchivesInParallel(
        context: Context,
        archives: List<ArchiveInfo>,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?
    ) = coroutineScope {

        val totalCount = archives.size
        val processedCount = AtomicInteger(0)

        val workers = min(16, archives.size.coerceAtLeast(1))
        val channel = Channel<ArchiveInfo>(capacity = archives.size)

        val workerResults = List(workers) { mutableListOf<PreviewManager.PreviewBatchEntry>() }

        archives.forEach { channel.send(it) }
        channel.close()

        val jobs = List(workers) { workerId ->
            launch(Dispatchers.IO) {
                val localList = workerResults[workerId]

                for (archive in channel) {
                    if (!isActive) break

                    val currentCount = processedCount.incrementAndGet()

                    withContext(Dispatchers.Main) {
                        _currentPreviewProgress =
                            context.getString(R.string.preview_processing_archive, currentCount, totalCount, archive.displayName)
                    }

                    try {
                        val previewPath = generatePreviewWithRetry(
                            context = context,
                            archive = archive,
                            currentCount = currentCount,
                            totalCount = totalCount
                        )

                        if (previewPath != null) {
                            localList.add(
                                PreviewManager.PreviewBatchEntry(
                                    archiveUri = archive.filePath,
                                    previewPath = previewPath,
                                    fileName = archive.originalName,
                                    fileSize = archive.fileSize
                                )
                            )
                            updateArchivePreview(archive, previewPath, onUpdateArchives)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Worker $workerId: error on ${archive.displayName}", e)
                    }
                }
            }
        }

        jobs.joinAll()

        val mergedResults = workerResults.flatten()
        Log.d(TAG, "Merging ${mergedResults.size} preview entries into prefs")

        withContext(Dispatchers.IO) {
            PreviewManager.savePreviewPaths(context, mergedResults)
        }

        reloadAllPreviews(context, onUpdateArchives)
    }


    private suspend fun generatePreviewWithRetry(
        context: Context,
        archive: ArchiveInfo,
        currentCount: Int,
        totalCount: Int
    ): String? {
        repeat(FILE_WRITE_RETRY_ATTEMPTS) { attempt ->
            try {
                val previewPath = generatePreviewForArchive(
                    context = context,
                    archiveUri = archive.filePath.toUri()
                ) { progress ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _currentPreviewProgress = context.getString(R.string.preview_processing_with_progress, currentCount, totalCount, progress)
                    }
                }

                if (previewPath != null) {
                    val file = File(previewPath)
                    if (file.exists() && file.length() > 0) {
                        file.inputStream().use { }
                        return previewPath
                    } else {
                        Log.w(TAG, "Preview file not properly written, attempt ${attempt + 1}/$FILE_WRITE_RETRY_ATTEMPTS")
                    }
                } else {
                    Log.w(TAG, "Preview generation returned null, attempt ${attempt + 1}/$FILE_WRITE_RETRY_ATTEMPTS")
                }

                if (attempt < FILE_WRITE_RETRY_ATTEMPTS - 1) {
                    delay(FILE_WRITE_RETRY_DELAY)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating preview, attempt ${attempt + 1}/$FILE_WRITE_RETRY_ATTEMPTS", e)
                if (attempt < FILE_WRITE_RETRY_ATTEMPTS - 1) {
                    delay(FILE_WRITE_RETRY_DELAY)
                }
            }
        }
        return null
    }

    private suspend fun reloadAllPreviews(
        context: Context,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)? = null
    ) {
        val refreshed = withContext(Dispatchers.IO) {
            _archivesWithPreviews.map { archive ->
                try {
                    val documentFile = DocumentFile.fromSingleUri(context, archive.filePath.toUri())
                    val fileName = documentFile?.name ?: archive.originalName
                    val fileSize = documentFile?.length() ?: archive.fileSize

                    val previewPath = PreviewManager.getPreviewPath(context, fileName, fileSize)
                    val readingProgress = PreviewManager.getReadingProgressForPreview(context, fileName, fileSize)
                        ?: archive.readingProgress

                    archive.copy(
                        fileSize = fileSize,
                        previewPath = previewPath,
                        readingProgress = readingProgress
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "reloadAllPreviews: failed for ${archive.displayName}", e)
                    archive
                }
            }
        }

        archivesUpdateMutex.withLock {
            withContext(Dispatchers.Main) {
                _archivesWithPreviews = refreshed
                onUpdateArchives?.invoke(refreshed)
                Log.d(TAG, "reloadAllPreviews: UI refreshed with ${refreshed.size} entries")
            }
        }
    }

    private suspend fun updateArchivePreview(
        archive: ArchiveInfo,
        previewPath: String,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?
    ) {
        archivesUpdateMutex.withLock {
            val updatedArchives = _archivesWithPreviews.map {
                if (it.filePath == archive.filePath) {
                    it.copy(previewPath = previewPath)
                } else it
            }

            withContext(Dispatchers.Main) {
                _archivesWithPreviews = updatedArchives
                onUpdateArchives?.invoke(updatedArchives)
            }
        }
    }

    private suspend fun getAllArchivesWithProgress(
        context: Context,
        dirUri: android.net.Uri
    ): List<ArchiveInfo> = withContext(Dispatchers.IO) {
        val allArchives = mutableListOf<ArchiveInfo>()
        var scannedCount = 0
        var lastUpdateTime = System.currentTimeMillis()

        val previewsSnapshot = PreviewManager.getPreviewsSnapshot(context)

        suspend fun scanRecursive(uri: android.net.Uri) {
            try {
                val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return

                documentFile.listFiles().forEach { file ->
                    scannedCount++

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 300) {
                        withContext(Dispatchers.Main) {
                            _currentPreviewProgress = context.getString(R.string.preview_scanning_found_archives, allArchives.size)
                        }
                        lastUpdateTime = currentTime
                    }

                    when {
                        file.isDirectory -> scanRecursive(file.uri)
                        file.isFile && isArchiveFile(file.name) -> {
                            allArchives.add(createArchiveInfo(file, previewsSnapshot))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning directory: ${e.message}", e)
            }
        }

        scanRecursive(dirUri)
        allArchives
    }

    private fun isArchiveFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return ArchiveFormats.SUPPORTED_EXTENSIONS.contains(extension)
    }

    private fun createArchiveInfo(file: DocumentFile, previewsSnapshot: Map<String, PreviewInfo>): ArchiveInfo {
        val fileName = file.name ?: "unknown"
        val fileSize = file.length()
        val previewPath = PreviewManager.getPreviewPathFromSnapshot(previewsSnapshot, fileName, fileSize)
        val readingProgress = PreviewManager.getReadingProgressFromSnapshot(previewsSnapshot, fileName, fileSize)

        return ArchiveInfo(
            filePath = file.uri.toString(),
            originalName = fileName,
            displayName = fileName,
            lastModified = file.lastModified(),
            previewPath = previewPath,
            fileSize = fileSize,
            readingProgress = readingProgress
        )
    }

    fun regeneratePreviewForArchive(context: Context, archive: ArchiveInfo) {
        Log.d(TAG, "regeneratePreviewForArchive: ${archive.displayName}")
        clearPreviewForArchive(context, archive)
        viewModelScope.launch {
            generatePreviewForArchive(context = context, archiveUri = archive.filePath.toUri())?.let { previewPath ->
                _archivesWithPreviews = _archivesWithPreviews.map {
                    if (it.filePath == archive.filePath) it.copy(previewPath = previewPath) else it
                }
            }
        }
    }

    fun clearPreviewForArchive(context: Context, archive: ArchiveInfo) {
        Log.d(TAG, "clearPreviewForArchive: ${archive.displayName}")
        PreviewManager.removePreviewAndProgressByUri(context, archive.filePath)
        _archivesWithPreviews = _archivesWithPreviews.map {
            if (it.filePath == archive.filePath) it.copy(previewPath = null, readingProgress = null) else it
        }
    }

    fun loadArchivesWithPreviewsAndSort(
        context: Context,
        archives: List<ArchiveInfo>,
        directoryUri: String,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?
    ) {
        viewModelScope.launch {
            if (_currentDirectoryUri != directoryUri) {
                _currentDirectoryUri = directoryUri
                _currentSortType = null
            }

            val archivesWithPreviews = withContext(Dispatchers.IO) {
                val previewsSnapshot = PreviewManager.getPreviewsSnapshot(context)
                archives.map { archive ->
                    async {
                        val documentFile = DocumentFile.fromSingleUri(context, archive.filePath.toUri())
                        val fileName = documentFile?.name ?: archive.originalName
                        val fileSize = documentFile?.length() ?: archive.fileSize

                        val previewPath = PreviewManager.getPreviewPathFromSnapshot(previewsSnapshot, fileName, fileSize)
                        val readingProgress = PreviewManager.getReadingProgressFromSnapshot(previewsSnapshot, fileName, fileSize)
                            ?: archive.readingProgress

                        archive.copy(fileSize = fileSize, previewPath = previewPath, readingProgress = readingProgress)
                    }
                }.awaitAll()
            }

            val savedSortType = DirectoryManager.loadSortTypeForDirectory(context, directoryUri)
            val sortTypeToApply = savedSortType ?: SortType.NAME_ASC
            _currentSortType = savedSortType

            val sortedArchives = withContext(Dispatchers.Default) {
                SortingUtils.sortArchives(archivesWithPreviews, sortTypeToApply)
            }

            _archivesWithPreviews = sortedArchives
            onUpdateArchives?.invoke(sortedArchives)
        }
    }

    fun loadSavedSortType(context: Context, directoryUri: String) {
        val savedSortType = DirectoryManager.loadSortTypeForDirectory(context, directoryUri)
        _currentSortType = savedSortType
        Log.d(TAG, "Loaded saved sort type for UI: $savedSortType")
    }

    fun sortArchivesAndSave(
        context: Context,
        directoryUri: String,
        sortType: SortType,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?
    ) {
        viewModelScope.launch {
            val sortedArchives = withContext(Dispatchers.Default) {
                SortingUtils.sortArchives(_archivesWithPreviews, sortType)
            }

            _archivesWithPreviews = sortedArchives
            _currentSortType = sortType
            onUpdateArchives?.invoke(sortedArchives)

            DirectoryManager.saveSortTypeForDirectory(context, directoryUri, sortType)

            Log.d(TAG, "Archives sorted by: $sortType and saved for directory: $directoryUri")
        }
    }

    fun clearProgressForArchive(context: Context, archive: ArchiveInfo) {
        Log.d(TAG, "clearProgressForArchive: ${archive.displayName}")
        val documentFile = DocumentFile.fromSingleUri(context, archive.filePath.toUri())
        val fileName = documentFile?.name ?: archive.originalName
        val fileSize = documentFile?.length() ?: 0L

        ArchiveStructureManager.deleteArchiveStructure(context, fileName, fileSize)
        PreviewManager.saveReadingProgressForPreview(
            context = context,
            archiveUri = archive.filePath,
            fileName = fileName,
            fileSize = fileSize,
            currentIndex = 0,
            totalImages = 0
        )

        _archivesWithPreviews = _archivesWithPreviews.map {
            if (it.filePath == archive.filePath) it.copy(readingProgress = null) else it
        }
    }

    fun markPreviewLoaded(cacheKey: String) {
        if (cacheKey !in _loadedPreviewKeys) {
            viewModelScope.launch(Dispatchers.Main) {
                _loadedPreviewKeys = _loadedPreviewKeys + cacheKey
            }
        }
    }

    fun clearLoadedPreviewKeys() {
        _loadedPreviewKeys = emptySet()
    }

    fun cancelPreviewGeneration() {
        Log.d(TAG, "Cancelling preview generation")
        currentGenerationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelPreviewGeneration()
    }
}