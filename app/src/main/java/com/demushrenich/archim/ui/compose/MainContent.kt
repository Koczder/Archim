package com.demushrenich.archim.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.ui.screens.*
import com.demushrenich.archim.ui.viewmodel.MainViewModel

@Composable
fun MainContent(
    paddingValues: PaddingValues,
    viewModel: MainViewModel,
    uiState: AppUiState,
    pagerState: PagerState,
    onPickDirectory: () -> Unit,
    onPickArchive: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.currentPage == 1 && !pagerState.isScrollInProgress) {
            viewModel.loadSavedDirectoriesIfNeeded(context)
        }
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        when (val screenState = getScreenState(uiState)) {
            is ScreenState.Loading -> LoadingScreen(
                progress = screenState.progress,
                currentFileName = screenState.fileName,
                onCancel = { viewModel.cancelExtraction() }
            )

            is ScreenState.ImageViewer -> ImageViewerScreen(
                state = uiState,
                archiveNavState = viewModel.archiveNavState,
                viewerImages = viewModel.getViewerImagesList(),
                onBack = { viewModel.clearSelectedImage() },
                onIndexChange = { viewModel.setSelectedImageIndex(it) },
                readingDirection = uiState.settings.readingDirection,
                onBackgroundModeChange = { viewModel.changeBackgroundMode(it) }
            )

            is ScreenState.ImageGrid -> ImageGridScreen(
                state = uiState,
                onImageClick = { index -> viewModel.selectImage(index, context) },
                archiveNavState = viewModel.archiveNavState,
                onBack = { viewModel.navigateBackInGrid(context) },
                onSortTypeChange = { viewModel.changeSortType(it, context) },
                onMakePreview = { viewModel.makePreview(context, it) },
                onCopyImage = { viewModel.copyImage(context, it) },
                onOpenFolder = { viewModel.openFolderInArchive(context, it) },
                onContinueReadingWithNavigation = { viewModel.continueReading(it, context) },
                imageCornerStyle = uiState.settings.imageCornerStyle,
                readingDirection = uiState.settings.readingDirection
            )

            is ScreenState.MainPages -> HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ArchivePageScreen(
                        uiState = uiState,
                        onPickArchive = onPickArchive,
                        onEnterPassword = { viewModel.showPasswordDialog() },
                        onShowSettings = { viewModel.showSettings() }
                    )
                    1 -> DirectoriesPageScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAddDirectory = onPickDirectory,
                        context = context as ComponentActivity
                    )
                }
            }
        }
    }
}