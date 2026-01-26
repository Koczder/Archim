package com.demushrenich.archim.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.ui.dialogs.PasswordDialog
import com.demushrenich.archim.ui.navigation.NavigationHandler
import com.demushrenich.archim.ui.theme.PictureTestStorageTheme
import com.demushrenich.archim.ui.viewmodel.MainViewModel
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun AppContent(
    viewModel: MainViewModel,
    settingsManager: SettingsManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = LocalContext.current

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

    PictureTestStorageTheme {
        NavigationHandler(
            viewModel = viewModel,
            uiState = uiState,
            pagerState = pagerState,
            context = context
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (shouldShowBottomBar(uiState)) {
                    BottomNavigationBar(uiState, pagerState)
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