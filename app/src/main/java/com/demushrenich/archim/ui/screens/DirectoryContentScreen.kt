package com.demushrenich.archim.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.Disposable
import com.demushrenich.archim.R
import com.demushrenich.archim.ui.components.NavigationBreadcrumbs
import com.demushrenich.archim.ui.components.NavigationStatusCard
import com.demushrenich.archim.data.ArchiveInfo
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.data.NavigationState
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.domain.PreviewGenerationMode
import com.demushrenich.archim.domain.CornerStyle
import com.demushrenich.archim.domain.SortType
import com.demushrenich.archim.domain.utils.getSavedScroll
import com.demushrenich.archim.domain.utils.saveScroll
import com.demushrenich.archim.ui.components.*
import com.demushrenich.archim.ui.viewmodel.DirectoryContentViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryContentScreen(
    currentDirName: String,
    folders: List<DirectoryItem>,
    archives: List<ArchiveInfo>,
    currentDirUri: String,
    isLoading: Boolean = false,
    error: String? = null,
    navigationState: NavigationState? = null,
    isNewDirectory: Boolean = false,
    archiveCornerStyle: CornerStyle = CornerStyle.ROUNDED,
    onBack: () -> Unit,
    onOpenFolder: (DirectoryItem) -> Unit,
    onOpenArchive: (ArchiveInfo) -> Unit,
    onUpdateArchives: ((List<ArchiveInfo>) -> Unit)? = null,
    onNewDirectoryPreviewHandled: (() -> Unit)? = null,
    viewModel: DirectoryContentViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = remember { ImageLoader(context) }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val preloadRequests = remember(currentDirUri) { mutableStateOf(listOf<Disposable>()) }
    val contextMenuState = rememberContextMenuState()

    val settingsManager = remember { SettingsManager(context) }
    val actualPreviewMode by settingsManager.previewGenerationMode.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    val currentSortType by remember { derivedStateOf { viewModel.currentSortType } }

    val saved = getSavedScroll(currentDirUri)
    val listState = remember(currentDirUri) {
        LazyListState(firstVisibleItemIndex = saved.index, firstVisibleItemScrollOffset = saved.offset)
    }

    val clipboardManager = LocalClipboardManager.current

    BackHandler {
        saveScroll(currentDirUri, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        onBack()
    }


    LaunchedEffect(currentDirUri) {
        viewModel.loadSavedSortType(context, currentDirUri)
    }

    LaunchedEffect(isNewDirectory, currentDirUri, actualPreviewMode) {
        when (actualPreviewMode) {
            PreviewGenerationMode.DIALOG -> {
                viewModel.checkAndShowPreviewDialog(currentDirUri, isNewDirectory, actualPreviewMode)
            }
            PreviewGenerationMode.AUTO -> {
                viewModel.checkAndAutoGeneratePreviews(
                    context = context,
                    currentDirUri = currentDirUri,
                    isNewDirectory = isNewDirectory,
                    previewMode = actualPreviewMode,
                    onUpdateArchives = onUpdateArchives
                )
            }
            PreviewGenerationMode.MANUAL -> {
                if (isNewDirectory) {
                    viewModel.checkAndShowPreviewDialog(currentDirUri, isNewDirectory, actualPreviewMode)
                }
            }
        }
    }

    LaunchedEffect(archives) {
        viewModel.loadArchivesWithPreviewsAndSort(
            context = context,
            archives = archives,
            directoryUri = currentDirUri,
            onUpdateArchives = onUpdateArchives
        )
    }

    LaunchedEffect(viewModel.archivesWithPreviews) {
        preloadRequests.value.forEach { it.dispose() }
        val newRequests = mutableListOf<Disposable>()

        viewModel.archivesWithPreviews.forEach { archive ->
            if (archive.previewPath != null &&
                File(archive.previewPath).exists() &&
                !viewModel.loadedImages.contains(archive.filePath)) {

                val request = imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(File(archive.previewPath))
                        .listener(
                            onSuccess = { _, _ ->
                                viewModel.addLoadedImage(archive.filePath)
                            },
                            onError = { _, _ ->
                                viewModel.addLoadedImage(archive.filePath)
                            }
                        )
                        .build()
                )
                newRequests.add(request)
            }
        }
        preloadRequests.value = newRequests
    }

    DisposableEffect(currentDirUri) {
        onDispose {
            preloadRequests.value.forEach { it.dispose() }
            viewModel.clearLoadedImages()
        }
    }

    if (actualPreviewMode == PreviewGenerationMode.DIALOG && viewModel.shouldShowPreviewDialog) {
        PreviewGenerationDialog(
            isVisible = true,
            onDismiss = {
                Log.d("DirectoryContentScreen", "Dialog dismissed")
                viewModel.hidePreviewDialog()
                onNewDirectoryPreviewHandled?.invoke()
            },
            onConfirm = {
                Log.d("DirectoryContentScreen", "Dialog confirmed")
                viewModel.hidePreviewDialog()
                onNewDirectoryPreviewHandled?.invoke()
                viewModel.generatePreviewsForAllArchives(
                    context = context,
                    currentDirUri = currentDirUri,
                    onUpdateArchives = onUpdateArchives
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentDirName)
                        navigationState?.let { navState ->
                            NavigationBreadcrumbs(navState, Modifier.padding(top = 2.dp))
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            Log.d("DirectoryContentScreen", "Manual preview generation button clicked")
                            if (!viewModel.isGeneratingPreviews) {
                                viewModel.generatePreviewsForAllArchives(
                                    context = context,
                                    currentDirUri = currentDirUri,
                                    onUpdateArchives = onUpdateArchives
                                )
                            }
                        },
                        enabled = !viewModel.isGeneratingPreviews
                    ) {
                        if (viewModel.isGeneratingPreviews) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = stringResource(R.string.preview_generation),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.sort_menu_label),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_by_name) + when (currentSortType) {
                                            SortType.NAME_ASC -> "↑"
                                            SortType.NAME_DESC -> "↓"
                                            else -> ""
                                        }
                                    )
                                },
                                onClick = {
                                    val newSortType = when (currentSortType) {
                                        SortType.NAME_ASC -> SortType.NAME_DESC
                                        SortType.NAME_DESC -> SortType.NAME_ASC
                                        else -> SortType.NAME_ASC
                                    }
                                    viewModel.sortArchivesAndSave(
                                        context = context,
                                        directoryUri = currentDirUri,
                                        sortType = newSortType,
                                        onUpdateArchives = onUpdateArchives
                                    )
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_by_date) + when (currentSortType) {
                                            SortType.DATE_ASC -> "↑"
                                            SortType.DATE_DESC -> "↓"
                                            else -> ""
                                        }
                                    )
                                },
                                onClick = {
                                    val newSortType = when (currentSortType) {
                                        SortType.DATE_ASC -> SortType.DATE_DESC
                                        SortType.DATE_DESC -> SortType.DATE_ASC
                                        else -> SortType.DATE_DESC
                                    }
                                    viewModel.sortArchivesAndSave(
                                        context = context,
                                        directoryUri = currentDirUri,
                                        sortType = newSortType,
                                        onUpdateArchives = onUpdateArchives
                                    )
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_by_progress) + when (currentSortType) {
                                            SortType.PROGRESS_ASC -> "↑"
                                            SortType.PROGRESS_DESC -> "↓"
                                            else -> ""
                                        }
                                    )
                                },
                                onClick = {
                                    val newSortType = when (currentSortType) {
                                        SortType.PROGRESS_ASC -> SortType.PROGRESS_DESC
                                        SortType.PROGRESS_DESC -> SortType.PROGRESS_ASC
                                        else -> SortType.PROGRESS_DESC
                                    }
                                    viewModel.sortArchivesAndSave(
                                        context = context,
                                        directoryUri = currentDirUri,
                                        sortType = newSortType,
                                        onUpdateArchives = onUpdateArchives
                                    )
                                    showSortMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_by_last_opened) + when (currentSortType) {
                                            SortType.LAST_OPENED_ASC -> "↑"
                                            SortType.LAST_OPENED_DESC -> "↓"
                                            else -> ""
                                        }
                                    )
                                },
                                onClick = {
                                    val newSortType = when (currentSortType) {
                                        SortType.LAST_OPENED_ASC -> SortType.LAST_OPENED_DESC
                                        SortType.LAST_OPENED_DESC -> SortType.LAST_OPENED_ASC
                                        else -> SortType.LAST_OPENED_DESC
                                    }
                                    viewModel.sortArchivesAndSave(
                                        context = context,
                                        directoryUri = currentDirUri,
                                        sortType = newSortType,
                                        onUpdateArchives = onUpdateArchives
                                    )
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { containerCoords = it }
        ) {
            Column {
                NavigationStatusCard(
                    isLoading = isLoading,
                    error = error,
                    progressMessage = viewModel.currentPreviewProgress,
                    isProcessing = viewModel.isGeneratingPreviews
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (folders.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.directories),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        items(folders, key = { it.uri }) { folder ->
                            FolderItemComponent(
                                folder = folder,
                                onOpenFolder = {
                                    saveScroll(currentDirUri, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                                    onOpenFolder(it)
                                }
                            )
                        }
                    }

                    if (viewModel.archivesWithPreviews.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.archives),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(4.dp)
                            )
                        }

                        items(viewModel.archivesWithPreviews, key = { it.filePath }) { archive ->
                            val isImageLoaded by remember(archive.filePath, archive.previewPath) {
                                derivedStateOf { viewModel.loadedImages.contains(archive.filePath) }
                            }

                            ArchiveItemComponent(
                                archive = archive,
                                isImageLoaded = isImageLoaded,
                                cornerStyle = archiveCornerStyle,
                                modifier = Modifier.contextMenuHandler(
                                    item = archive,
                                    contextMenuState = contextMenuState,
                                    containerCoords = containerCoords,
                                    onTap = {
                                        saveScroll(currentDirUri, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                                        onOpenArchive(archive)
                                    }
                                )
                            )
                        }
                    }
                }
            }

            ContextMenu(state = contextMenuState) { item ->
                val archive = item as? ArchiveInfo ?: return@ContextMenu

                ContextMenuItem(
                    text = stringResource(R.string.copy_name),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(archive.originalName))
                        contextMenuState.hide()
                    }
                )

                ContextMenuItem(
                    text = stringResource(R.string.clear_preview),
                    onClick = {
                        viewModel.clearPreviewForArchive(context, archive)
                        contextMenuState.hide()
                    }
                )
                ContextMenuItem(
                    text = stringResource(R.string.regenerate_preview),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.regeneratePreviewForArchive(context, archive)
                        }
                        contextMenuState.hide()
                    }
                )
                ContextMenuItem(
                    text = stringResource(R.string.clear_progress),
                    onClick = {
                        viewModel.clearProgressForArchive(context, archive)
                        contextMenuState.hide()
                    }
                )
            }
        }
    }
}