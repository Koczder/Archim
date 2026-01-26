package com.demushrenich.archim.ui.compose

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.demushrenich.archim.R
import com.demushrenich.archim.data.AppUiState
import kotlinx.coroutines.launch

@Composable
fun shouldShowBottomBar(uiState: AppUiState): Boolean {
    return !uiState.loading.isLoading &&
            uiState.imageView.selectedImageIndex == null &&
            uiState.imageView.images.isEmpty() &&
            !uiState.settings.showSettings
}

@Composable
fun BottomNavigationBar(
    uiState: AppUiState,
    pagerState: PagerState
) {
    NavigationBar {
        val scope = rememberCoroutineScope()

        NavigationBarItem(
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            label = { Text(stringResource(R.string.open_archive)) },
            selected = pagerState.currentPage == 0,
            onClick = {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
            label = { Text(stringResource(R.string.directories)) },
            selected = pagerState.currentPage == 1,
            onClick = {
                scope.launch { pagerState.animateScrollToPage(1) }
            }
        )
    }
}