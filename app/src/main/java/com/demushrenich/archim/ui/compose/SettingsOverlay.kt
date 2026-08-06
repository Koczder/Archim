package com.demushrenich.archim.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.ui.screens.SettingsScreen
import com.demushrenich.archim.ui.viewmodel.MainViewModel

@Composable
fun SettingsOverlay(
    viewModel: MainViewModel,
    uiState: AppUiState
) {
    val context = LocalContext.current

    SettingsScreen(
        currentLanguage = uiState.settings.currentLanguage,
        onLanguageChange = { viewModel.changeLanguage(it) },
        currentDirection = uiState.settings.readingDirection,
        onDirectionChange = { viewModel.changeReadingDirection(it) },
        currentPreviewMode = uiState.settings.previewGenerationMode,
        onPreviewModeChange = { viewModel.changePreviewGenerationMode(it) },
        currentLoadingMode = uiState.settings.previewLoadingMode,
        onLoadingModeChange = { viewModel.changePreviewLoadingMode(it) },
        currentArchiveCornerStyle = uiState.settings.archiveCornerStyle,
        onArchiveCornerStyleChange = { viewModel.changeArchiveCornerStyle(it) },
        currentImageCornerStyle = uiState.settings.imageCornerStyle,
        onImageCornerStyleChange = { viewModel.changeImageCornerStyle(it) },
        currentArchiveOpenMode = uiState.settings.archiveOpenMode,
        onArchiveOpenModeChange = { viewModel.changeArchiveOpenMode(it) },
        currentAddDirectoryButtonPosition = uiState.settings.addDirectoryButtonPosition,
        onAddDirectoryButtonPositionChange = { viewModel.changeAddDirectoryButtonPosition(it) },
        onCleanupOrphanedPreviews = { viewModel.cleanupOrphanedPreviews() },
        onBackClick = { viewModel.hideSettings() }
    )
}