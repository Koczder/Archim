package com.demushrenich.archim.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.demushrenich.archim.data.*
import com.demushrenich.archim.ui.viewmodel.MainViewModel

@Composable
fun DirectoriesPageScreen(
    viewModel: MainViewModel,
    uiState: AppUiState,
    onAddDirectory: () -> Unit,
    context: ComponentActivity
) {
    val currentLevel = viewModel.navigationState.getCurrentLevel()

    when {
        viewModel.navigationState.currentLevel == 0 -> {
            DirectoryListScreen(
                directories = currentLevel?.folders ?: emptyList(),
                newDirectoryUris = viewModel.getNewRootDirectoryUris(),
                onAddClick = onAddDirectory,
                onOpenDirectory = { item: DirectoryItem, isNewDirectory: Boolean ->
                    viewModel.openDirectory(context, item, isNewDirectory)
                },
                onDeleteDirectory = { item: DirectoryItem ->
                    viewModel.deleteDirectory(context, item)
                }
            )
        }
        else -> {
            currentLevel?.let { level ->
                DirectoryContentScreen(
                    currentDirName = level.displayName,
                    folders = level.folders,
                    archives = level.archives,
                    currentDirUri = level.uri,
                    isLoading = level.isLoading,
                    error = level.error,
                    navigationState = viewModel.navigationState,
                    isNewDirectory = level.isNewDirectory,
                    archiveCornerStyle = uiState.settings.archiveCornerStyle,
                    onBack = { viewModel.navigationState.navigateBack() },
                    onOpenFolder = { folder: DirectoryItem ->
                        viewModel.openSubdirectory(context, folder)
                    },
                    onOpenArchive = { archive: ArchiveInfo ->
                        val uri = archive.filePath.toUri()
                        viewModel.handleArchivePicked(context, uri)
                    },
                    onUpdateArchives = { updatedArchives: List<ArchiveInfo> ->
                        viewModel.navigationState.updateCurrentLevel(archives = updatedArchives)
                    },
                    onNewDirectoryPreviewHandled = {
                        viewModel.removeNewDirectoryUri(level.uri)
                    },
                    viewModel = viewModel.getDirectoryContentViewModel()
                )
            }
        }
    }
}