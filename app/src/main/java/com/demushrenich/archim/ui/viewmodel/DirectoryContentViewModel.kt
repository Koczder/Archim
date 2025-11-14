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
import com.demushrenich.archim.data.utils.SortingUtils
import com.demushrenich.archim.data.utils.generatePreviewForArchive
import com.demushrenich.archim.domain.PreviewGenerationMode
import com.demushrenich.archim.domain.SortType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

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

    private var _loadedImages by mutableStateOf(setOf<String>())
    val loadedImages: Set<String> get() = _loadedImages

    private var _shouldShowPreviewDialog by mutableStateOf(false)
    val shouldShowPreviewDialog: Boolean get() = _shouldShowPreviewDialog

    private val handledDirectories = mutableSetOf<String>()

    private val archivesUpdateMutex = Mutex()

    private var currentGenerationJob: Job? = null

    private val scrollPositions = mutableMapOf<String, ScrollPosition>()

    fun saveScroll(key: String, index: Int, offset: Int) {
        scrollPositions[key] = ScrollPosition(index, offset)
    }

    fun getSavedScroll(key: String): ScrollPosition {
        return scrollPositions[key] ?: ScrollPosition(0, 0)
    }


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

    fun sortArchives(sortType: SortType, onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?) {
        viewModelScope.launch {
            val sortedArchives = withContext(Dispatchers.Default) {
                SortingUtils.sortArchives(_archivesWithPreviews, sortType)
            }

            _archivesWithPreviews = sortedArchives
            onUpdateArchives?.invoke(sortedArchives)

            Log.d(TAG, "Archives sorted by: $sortType")
        }
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
                _currentPreviewProgress = "Сканирование директории..."
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
                    _currentPreviewProgress = "Все превью уже сгенерированы"
                    delay(2000)
                    return@launch
                }

                processArchivesInParallel(
                    context = context,
                    archives = archivesToProcess,
                    onUpdateArchives = onUpdateArchives
                )

                _currentPreviewProgress = "Завершено! Обработано ${archivesToProcess.size} архивов"
                delay(2000)

            } catch (e: CancellationException) {
                Log.d(TAG, "Preview generation cancelled")
                _currentPreviewProgress = "Генерация отменена"
                delay(1000)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating previews", e)
                _currentPreviewProgress = "Ошибка при генерации превью: ${e.message}"
                delay(3000)
            } finally {
                _isGeneratingPreviews = false
                _currentPreviewProgress = ""
                Log.d(TAG, "Preview generation finished")
            }
        }
    }

    // 1) Обновлённый processArchivesInParallel
    private suspend fun processArchivesInParallel(
        context: Context,
        archives: List<ArchiveInfo>,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)?
    ) = coroutineScope {

        val totalCount = archives.size
        val processedCount = AtomicInteger(0)

        val workers = min(16, archives.size.coerceAtLeast(1))

        val channel = Channel<ArchiveInfo>(capacity = archives.size)

        val workerResults = List(workers) { mutableMapOf<String, String>() }

        archives.forEach { channel.send(it) }
        channel.close()

        val jobs = List(workers) { workerId ->
            launch(Dispatchers.IO) {
                val localMap = workerResults[workerId]

                for (archive in channel) {
                    if (!isActive) break

                    val currentCount = processedCount.incrementAndGet()

                    withContext(Dispatchers.Main) {
                        _currentPreviewProgress =
                            "Обработка ($currentCount/$totalCount): ${archive.displayName}"
                    }

                    Log.d(TAG, "Worker $workerId processing ${archive.displayName} ($currentCount/$totalCount)")

                    try {
                        val previewPath = generatePreviewWithRetry(
                            context = context,
                            archive = archive,
                            currentCount = currentCount,
                            totalCount = totalCount
                        )

                        if (previewPath != null) {
                            localMap[archive.filePath] = previewPath
                            updateArchivePreview(archive, previewPath, onUpdateArchives)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Worker $workerId: error on ${archive.displayName}", e)
                    }
                }

                Log.d(TAG, "Worker $workerId finished, saved ${localMap.size} previews locally")
            }
        }

        jobs.joinAll()
        val mergedResults = workerResults
            .flatMap { it.entries }
            .associate { it.toPair() }

        Log.d(TAG, "Merging ${mergedResults.size} preview entries into prefs")

        withContext(Dispatchers.IO) {
            mergedResults.forEach { (uri, previewPath) ->
                try {
                    val file = DocumentFile.fromSingleUri(context, uri.toUri())
                    val fileName = file?.name ?: uri.substringAfterLast('/')
                    val fileSize = file?.length() ?: 0L

                    PreviewManager.savePreviewPath(
                        context = context,
                        archiveUri = uri,
                        previewPath = previewPath,
                        fileName = fileName,
                        fileSize = fileSize
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save merged preview for $uri", e)
                }
            }
        }
        reloadAllPreviews(context, onUpdateArchives)
    }


    // 2) Обновлённый generatePreviewWithRetry — НЕ пишет в PreviewManager
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
                        _currentPreviewProgress = "Обработка ($currentCount/$totalCount): $progress"
                    }
                }

                if (previewPath != null) {
                    val file = File(previewPath)
                    if (file.exists() && file.length() > 0) {
                        // проверяем читаемость
                        file.inputStream().use { }

                        // НЕ записываем в PreviewManager здесь — это делаем в конце одним батчем
                        Log.d(TAG, "Preview generated for ${archive.displayName}: $previewPath (attempt ${attempt + 1})")
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


    // 3) Новая функция: перечитывает превью из PreviewManager и обновляет in-memory список
    private suspend fun reloadAllPreviews(
        context: Context,
        onUpdateArchives: ((List<ArchiveInfo>) -> Unit)? = null
    ) {
        // читаем в IO
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

        // атомарно применяем обновление и уведомляем UI
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
                removeLoadedImage(archive.filePath)
                onUpdateArchives?.invoke(updatedArchives)
            }
        }
    }

    private suspend fun getAllArchivesWithProgress(
        context: Context,
        dirUri: android.net.Uri
    ): List<ArchiveInfo> {
        val allArchives = mutableListOf<ArchiveInfo>()
        var scannedCount = 0

        suspend fun scanRecursive(uri: android.net.Uri) {
            try {
                if (scannedCount % 10 == 0) {
                    _currentPreviewProgress = "Сканирование... найдено архивов: ${allArchives.size}"
                    yield()
                }

                val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return

                documentFile.listFiles().forEach { file ->
                    scannedCount++

                    when {
                        file.isDirectory -> {
                            scanRecursive(file.uri)
                        }
                        file.isFile && isArchiveFile(file.name) -> {
                            val archiveInfo = createArchiveInfo(context, file)
                            allArchives.add(archiveInfo)

                            if (allArchives.size % 5 == 0) {
                                _currentPreviewProgress = "Сканирование... найдено архивов: ${allArchives.size}"
                                yield()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning directory: ${e.message}", e)
            }
        }

        scanRecursive(dirUri)
        return allArchives
    }

    private fun isArchiveFile(fileName: String?): Boolean {
        if (fileName == null) return false
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in setOf("7z","zip","rar","tar","bz2","xz","lzma",
            "cab","iso","arj","lzh","chm","cpio","deb","rpm",
            "wim","xar","z", "cbz", "cbr", "cb7")
    }

    private fun createArchiveInfo(context: Context, file: DocumentFile): ArchiveInfo {
        val fileName = file.name ?: "unknown"
        val fileSize = file.length()
        val previewPath = PreviewManager.getPreviewPath(context, fileName, fileSize)

        val readingProgress = PreviewManager.getReadingProgressForPreview(context, fileName, fileSize)

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
        viewModelScope.launch {
            generatePreviewForArchive(context = context, archiveUri = archive.filePath.toUri())?.let { previewPath ->
                removeLoadedImage(archive.filePath)
                _archivesWithPreviews = _archivesWithPreviews.map {
                    if (it.filePath == archive.filePath) it.copy(previewPath = previewPath) else it
                }
            }
        }
    }

    fun clearPreviewForArchive(context: Context, archive: ArchiveInfo) {
        Log.d(TAG, "clearPreviewForArchive: ${archive.displayName}")
        PreviewManager.removePreviewAndProgressByUri(context, archive.filePath)
        removeLoadedImage(archive.filePath)
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
        Log.d(TAG, "loadArchivesWithPreviewsAndSort: ${archives.size} archives for $directoryUri")
        viewModelScope.launch {
            if (_currentDirectoryUri != directoryUri) {
                Log.d(TAG, "Directory changed from $_currentDirectoryUri to $directoryUri, resetting state")
                _currentDirectoryUri = directoryUri
                _currentSortType = null
            }
            val archivesWithPreviews = archives.map { archive ->
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
            }

            val savedSortType = DirectoryManager.loadSortTypeForDirectory(context, directoryUri)
            val sortTypeToApply = savedSortType ?: SortType.NAME_ASC

            _currentSortType = savedSortType

            Log.d(TAG, "Applying sort type: $sortTypeToApply (saved: $savedSortType)")

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

    fun addLoadedImage(filePath: String) {
        _loadedImages = _loadedImages + filePath
    }

    fun removeLoadedImage(filePath: String) {
        _loadedImages = _loadedImages - filePath
    }

    fun clearLoadedImages() {
        _loadedImages = emptySet()
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