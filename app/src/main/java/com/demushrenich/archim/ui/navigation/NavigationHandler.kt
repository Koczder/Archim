package com.demushrenich.archim.ui.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.ui.viewmodel.MainViewModel

@Composable
fun NavigationHandler(
    viewModel: MainViewModel,
    uiState: AppUiState,
    pagerState: PagerState,
    context: Context
) {
    BackHandler(
        enabled = when {
            uiState.imageView.selectedImageIndex != null -> true
            viewModel.canNavigateBackInArchive() -> true
            uiState.imageView.images.isNotEmpty() -> true
            pagerState.currentPage == 1 && viewModel.navigationState.canNavigateBack() -> true
            uiState.archive.showPasswordDialog -> true
            else -> false
        }
    ) {
        when {
            uiState.imageView.selectedImageIndex != null -> {
                viewModel.clearSelectedImage()
            }
            viewModel.canNavigateBackInArchive() -> {
                viewModel.navigateBackInGrid(context)
            }
            uiState.imageView.images.isNotEmpty() -> {
                viewModel.navigateBackInGrid(context)
            }
            uiState.archive.showPasswordDialog -> {
                viewModel.dismissPasswordDialog()
            }
            pagerState.currentPage == 1 && viewModel.navigationState.canNavigateBack() -> {
                viewModel.navigationState.navigateBack()
            }
        }
    }
}
