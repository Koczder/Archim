package com.demushrenich.archim.ui.compose

import com.demushrenich.archim.data.AppUiState

sealed class ScreenState {
    data class Loading(val progress: Float, val fileName: String) : ScreenState()
    object ImageViewer : ScreenState()
    object ImageGrid : ScreenState()
    object MainPages : ScreenState()
}

fun getScreenState(uiState: AppUiState): ScreenState {
    return when {
        uiState.loading.isLoading -> ScreenState.Loading(
            progress = uiState.loading.extractionProgress,
            fileName = uiState.loading.currentFileName
        )
        uiState.imageView.selectedImageIndex != null -> ScreenState.ImageViewer
        uiState.imageView.images.isNotEmpty() -> ScreenState.ImageGrid
        else -> ScreenState.MainPages
    }
}