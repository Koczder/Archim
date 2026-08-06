package com.demushrenich.archim.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.demushrenich.archim.ui.dialogs.PasswordDialog
import com.demushrenich.archim.ui.navigation.NavigationHandler
import com.demushrenich.archim.ui.theme.PictureTestStorageTheme
import com.demushrenich.archim.ui.viewmodel.MainViewModel
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.domain.AddDirectoryButtonPosition

@Composable
fun AppContent(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = LocalContext.current
    val density = LocalDensity.current
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    val pickDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { viewModel.handleDirectoryPicked(context, it) }
        }
    )

    val pickArchiveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.handleArchivePicked(context, it) }
        }
    )

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.handleStandaloneMediaPicked(context, it) }
        }
    )

    PictureTestStorageTheme {
        NavigationHandler(
            viewModel = viewModel,
            uiState = uiState,
            pagerState = pagerState,
            context = context
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (shouldShowBottomBar(uiState)) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                bottomBarHeight = with(density) { coords.size.height.toDp() }
                            }
                        ) {
                            BottomNavigationBar(uiState, pagerState)
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                    MainContent(
                        paddingValues = paddingValues,
                        viewModel = viewModel,
                        uiState = uiState,
                        pagerState = pagerState,
                        onPickDirectory = { pickDirectoryLauncher.launch(null) },
                        onPickArchive = { pickArchiveLauncher.launch(arrayOf("*/*")) }
                    )

                    if (uiState.settings.showSettings) {
                        SettingsOverlay(viewModel, uiState)
                    }
                }
            }

            val fabSize = 56.dp

            val isCurrentLevelLoading = viewModel.navigationState.getCurrentLevel()?.isLoading == true

            val showDirectoriesFab = uiState.settings.addDirectoryButtonPosition == AddDirectoryButtonPosition.BNBOVERLAY &&
                    shouldShowBottomBar(uiState) &&
                    pagerState.currentPage == 1 &&
                    viewModel.navigationState.currentLevel == 0 &&
                    !isCurrentLevelLoading

            AnimatedVisibility(
                visible = showDirectoriesFab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = ((bottomBarHeight - fabSize) / 2).coerceAtLeast(0.dp)),
                enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                DirectoriesFab(onClick = { pickDirectoryLauncher.launch(null) })
            }
        }

        if (uiState.archive.showPasswordDialog) {
            PasswordDialog(
                onDismiss = { viewModel.dismissPasswordDialog() },
                onConfirm = { password ->
                    viewModel.confirmPassword(context, password)
                },
                isError = uiState.archive.passwordError,
                errorMessage = uiState.archive.passwordErrorMessage
            )
        }
    }
}