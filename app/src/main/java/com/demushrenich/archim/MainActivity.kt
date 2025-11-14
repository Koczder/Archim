package com.demushrenich.archim

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.demushrenich.archim.ui.theme.PictureTestStorageTheme
import com.demushrenich.archim.ui.viewmodel.MainViewModel
import com.demushrenich.archim.ui.navigation.NavigationHandler
import com.demushrenich.archim.ui.screens.ArchivePageScreen
import com.demushrenich.archim.ui.screens.LoadingScreen
import com.demushrenich.archim.ui.screens.DirectoriesPageScreen
import com.demushrenich.archim.ui.screens.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.data.utils.clearCacheDir
import com.demushrenich.archim.data.utils.clearLargeArchiveCache
import com.demushrenich.archim.ui.screens.ImageGridScreen
import com.demushrenich.archim.ui.screens.ImageViewerScreen
import com.demushrenich.archim.ui.dialogs.PasswordDialog
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            clearLargeArchiveCache(this@MainActivity)
            clearCacheDir(this@MainActivity)
        }

        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val pagerState = rememberPagerState(pageCount = { 2 })

            LaunchedEffect(Unit) {
                viewModel.initializeSettings(this@MainActivity)
                viewModel.loadSavedDirectories(this@MainActivity)
            }

            val pickDirectoryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri ->
                    uri?.let { viewModel.handleDirectoryPicked(this@MainActivity, it) }
                }
            )

            val pickArchiveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let { viewModel.handleArchivePicked(this@MainActivity, it) }
                }
            )

            PictureTestStorageTheme {
                NavigationHandler(
                    viewModel = viewModel,
                    uiState = uiState,
                    pagerState = pagerState,
                    context = this
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!uiState.loading.isLoading &&
                            uiState.imageView.selectedImageIndex == null &&
                            uiState.imageView.images.isEmpty() &&
                            !uiState.settings.showSettings) {

                            BottomNavigationBar(
                                uiState = uiState,
                                pagerState = pagerState
                            )
                        }
                    }
                ) { paddingValues ->
                    val stablePickDirectoryLauncher by remember { mutableStateOf(pickDirectoryLauncher) }
                    val stablePickArchiveLauncher by remember { mutableStateOf(pickArchiveLauncher) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        MainContent(
                            paddingValues = paddingValues,
                            viewModel = viewModel,
                            uiState = uiState,
                            pagerState = pagerState,
                            pickDirectoryLauncher = { stablePickDirectoryLauncher.launch(null) },
                            pickArchiveLauncher = { stablePickArchiveLauncher.launch(arrayOf("*/*")) }
                        )

                        if (uiState.settings.showSettings) {
                            SettingsScreen(
                                currentLanguage = uiState.settings.currentLanguage,
                                onLanguageChange = { viewModel.changeLanguage(it) },
                                currentDirection = uiState.settings.readingDirection,
                                onDirectionChange = { viewModel.changeReadingDirection(it) },
                                currentPreviewMode = uiState.settings.previewGenerationMode,
                                onPreviewModeChange = { viewModel.changePreviewGenerationMode(it) },
                                currentArchiveCornerStyle = uiState.settings.archiveCornerStyle,
                                onArchiveCornerStyleChange = { viewModel.changeArchiveCornerStyle(it) },
                                currentImageCornerStyle = uiState.settings.imageCornerStyle,
                                onImageCornerStyleChange = { viewModel.changeImageCornerStyle(it) },
                                currentArchiveOpenMode = uiState.settings.archiveOpenMode,
                                onArchiveOpenModeChange = { viewModel.changeArchiveOpenMode(it) },
                                onCleanupOrphanedPreviews = { viewModel.cleanupOrphanedPreviews(this@MainActivity) },
                                onBackClick = { viewModel.hideSettings() }
                            )
                        }
                    }
                }

                if (uiState.archive.showPasswordDialog) {
                    PasswordDialog(
                        onDismiss = { viewModel.dismissPasswordDialog() },
                        onConfirm = { password ->
                            viewModel.confirmPassword(
                                this@MainActivity,
                                password
                            )
                        },
                        isError = uiState.archive.passwordError,
                        errorMessage = uiState.archive.passwordErrorMessage
                    )
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(
        uiState: AppUiState,
        pagerState: androidx.compose.foundation.pager.PagerState
    ) {
        if (!uiState.loading.isLoading &&
            uiState.imageView.selectedImageIndex == null &&
            uiState.imageView.images.isEmpty() &&
            !uiState.settings.showSettings) {

            NavigationBar {
                val scope = rememberCoroutineScope()
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text(stringResource(R.string.open_archive)) },
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text(stringResource(R.string.directories)) },
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
            }
        }
    }

    @Composable
    private fun MainContent(
        paddingValues: PaddingValues,
        viewModel: MainViewModel,
        uiState: AppUiState,
        pagerState: androidx.compose.foundation.pager.PagerState,
        pickDirectoryLauncher: () -> Unit,
        pickArchiveLauncher: () -> Unit
    ) {
        Column(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.loading.isLoading -> LoadingScreen(
                    progress = uiState.loading.extractionProgress,
                    currentFileName = uiState.loading.currentFileName,
                    onCancel = { viewModel.cancelExtraction() }
                )


                uiState.imageView.selectedImageIndex != null -> ImageViewerScreen(
                    state = uiState,
                    archiveNavState = viewModel.archiveNavState,
                    viewerImages = viewModel.getViewerImagesList(),
                    onBack = { viewModel.clearSelectedImage() },
                    onIndexChange = { viewModel.setSelectedImageIndex(it) },
                    readingDirection = uiState.settings.readingDirection,
                    onBackgroundModeChange = { mode -> viewModel.changeBackgroundMode(mode) }
                )

                uiState.imageView.images.isNotEmpty() -> ImageGridScreen(
                    state = uiState,
                    onImageClick = { index ->
                        viewModel.selectImage(
                            index,
                            this@MainActivity
                        )
                    },
                    archiveNavState = viewModel.archiveNavState,
                    onBack = { viewModel.navigateBackInGrid(this@MainActivity) },
                    onSortTypeChange = { sortCategory ->
                        viewModel.changeSortType(
                            sortCategory,
                            this@MainActivity
                        )
                    },
                    onMakePreview = { imageIndex ->
                        viewModel.makePreview(
                            this@MainActivity,
                            imageIndex
                        )
                    },
                    onCopyImage = { imageIndex ->
                        viewModel.copyImage(
                            this@MainActivity,
                            imageIndex
                        )
                    },
                    onOpenFolder = { folderItem ->
                        viewModel.openFolderInArchive(this@MainActivity, folderItem)
                    },
                    onContinueReadingWithNavigation = { globalIndex ->
                        viewModel.continueReading(globalIndex, this@MainActivity)
                    },
                    imageCornerStyle = uiState.settings.imageCornerStyle,
                    readingDirection = uiState.settings.readingDirection
                )

                else -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ArchivePageScreen(
                            uiState = uiState,
                            onPickArchive = pickArchiveLauncher,
                            onEnterPassword = { viewModel.showPasswordDialog() },
                            onShowSettings = { viewModel.showSettings() }
                        )
                        1 -> {
                            DirectoriesPageScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddDirectory = pickDirectoryLauncher,
                                context = this@MainActivity
                            )
                        }
                    }
                }
            }
        }
    }
}