package com.demushrenich.archim.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.Alignment
import com.demushrenich.archim.ui.components.ContextMenu
import com.demushrenich.archim.ui.components.ContextMenuItem
import com.demushrenich.archim.ui.components.contextMenuHandler
import com.demushrenich.archim.ui.components.rememberContextMenuState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Folder
import com.demushrenich.archim.data.ArchiveNavigationState
import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import com.demushrenich.archim.R
import com.demushrenich.archim.SortingComponent
import com.demushrenich.archim.domain.utils.SortingUtils
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.ReadingDirection
import com.demushrenich.archim.domain.SortCategory
import com.demushrenich.archim.data.managers.ArchiveStructureManager
import com.demushrenich.archim.domain.CornerStyle


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImageGridScreen(
    state: AppUiState,
    onImageClick: (Int) -> Unit,
    onBack: () -> Unit,
    onSortTypeChange: (SortCategory) -> Unit,
    archiveNavState: ArchiveNavigationState?,
    onMakePreview: ((Int) -> Unit)? = null,
    onCopyImage: ((Int) -> Unit)? = null,
    onOpenFolder: ((ImageItem) -> Unit)? = null,
    onContinueReadingWithNavigation: ((Int) -> Unit)? = null,
    imageCornerStyle: CornerStyle = CornerStyle.ROUNDED,
    readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT
) {
    val context = LocalContext.current
    val levelKey = archiveNavState?.getCurrentLevel()?.path ?: "root"
    val TAG = "ImageGrid"
    val contextMenuState = rememberContextMenuState()
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val archiveStructure = remember(state.archive.currentArchiveUri) {
        val archiveUri = state.archive.currentArchiveUri
        if (archiveUri != null) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, archiveUri)
                val fileName = documentFile?.name ?: ""
                val fileSize = documentFile?.length() ?: 0L

                if (fileSize > 0) {
                    ArchiveStructureManager.loadArchiveStructure(
                        context = context,
                        fileName = fileName,
                        fileSize = fileSize
                    )
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading archive structure", e)
                null
            }
        } else null
    }

    val currentLevelPath = archiveNavState?.getCurrentLevel()?.path
    val lastImageIdLevel = remember(archiveStructure, currentLevelPath) {
        if (archiveStructure != null && currentLevelPath != null) {
            archiveStructure.levels.find { it.path == currentLevelPath }?.lastImageIdLevel
        } else null
    }

    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 1200 -> 6
        configuration.screenWidthDp >= 800 -> 4
        else -> 2
    }

    val (folders, files) = remember(state.imageView.images) {
        val fld = state.imageView.images.filter { it.isFolder }
        val fls = state.imageView.images.filter { !it.isFolder }
        Pair(fld, fls)
    }

    val sortedFiles = remember(files, state.imageView.sortType) {
        SortingUtils.sortImages(files, state.imageView.sortType)
    }

    val sortedFolders = remember(folders, state.imageView.sortType) {
        SortingUtils.sortImages(folders, state.imageView.sortType)
    }

    val sortedImages = remember(sortedFolders, sortedFiles) {
        sortedFolders + sortedFiles
    }

    val displayImages = remember(sortedImages, readingDirection, columns) {
        when (readingDirection) {
            ReadingDirection.LEFT_TO_RIGHT -> sortedImages
            ReadingDirection.RIGHT_TO_LEFT -> {
                val result = mutableListOf<ImageItem>()
                val files = sortedImages.filter { !it.isFolder }
                val folders = sortedImages.filter { it.isFolder }

                result.addAll(folders)

                files.chunked(columns).forEach { row ->
                    result.addAll(row.reversed())
                }
                result
            }
        }
    }

    val startIndex = remember(archiveStructure, displayImages) {
        if (archiveStructure != null && archiveStructure.lastImageId != null) {
            val indexInDisplay = displayImages.indexOfFirst { it.id == archiveStructure.lastImageId }
            if (indexInDisplay >= 0) indexInDisplay else 0
        } else {
            0
        }
    }

    val gridState = remember(levelKey) {
        LazyGridState(firstVisibleItemIndex = startIndex)
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            R.string.back
                        ))
                    }
                },
                actions = {
                    if (archiveStructure != null && archiveStructure.lastImageId != null && !archiveStructure.isCompleted()) {
                        TextButton(
                            onClick = {
                                val globalIndex = archiveNavState?.allImages?.indexOfFirst {
                                    it.id == archiveStructure.lastImageId
                                } ?: -1

                                Log.d(TAG, "Continue clicked, lastImageId=${archiveStructure.lastImageId}, globalIndex=$globalIndex")
                                Log.d(TAG, "Total images in nav: ${archiveNavState?.allImages?.size}")

                                if (globalIndex >= 0) {
                                    Log.d(TAG, "Invoking continueReading with globalIndex=$globalIndex")
                                    onContinueReadingWithNavigation?.invoke(globalIndex)
                                } else {
                                    Log.e(TAG, "Invalid globalIndex: $globalIndex")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.continue_read),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.continue_read))
                        }
                    }

                    TextButton(onClick = { onSortTypeChange(SortCategory.NAME) }) {
                        SortingComponent.GetSortIcon(state.imageView.sortType, SortCategory.NAME)
                    }
                    TextButton(onClick = { onSortTypeChange(SortCategory.DATE) }) {
                        SortingComponent.GetSortIcon(state.imageView.sortType, SortCategory.DATE)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            if (archiveStructure != null && archiveStructure.totalImages > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (archiveStructure.isCompleted())
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (archiveStructure.isCompleted()) stringResource(R.string.reading_complete) else stringResource(
                                    R.string.reading_progress
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${archiveStructure.getTotalReadCount()} / ${archiveStructure.totalImages}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { archiveStructure.getProgressPercentage() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerCoords = it }
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayImages, key = { _, imageItem -> imageItem.id }) { displayIndex, imageItem ->
                        val sortedIndex = sortedImages.indexOfFirst { it.id == imageItem.id }

                        if (imageItem.isFolder) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                                    .clickable { onOpenFolder?.invoke(imageItem) },
                                shape = when (imageCornerStyle) {
                                    CornerStyle.ROUNDED -> MaterialTheme.shapes.small
                                    CornerStyle.SQUARE -> RectangleShape
                                },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = stringResource(R.string.floader),
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = imageItem.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                                    .contextMenuHandler(
                                        item = sortedIndex,
                                        contextMenuState = contextMenuState,
                                        containerCoords = containerCoords,
                                        onTap = {
                                            if (!imageItem.isFolder) {
                                                val fileIndex = sortedFiles.indexOf(imageItem)
                                                if (fileIndex != -1) onImageClick(fileIndex)
                                            }
                                        }

                                    ),
                                shape = when (imageCornerStyle) {
                                    CornerStyle.ROUNDED -> MaterialTheme.shapes.small
                                    CornerStyle.SQUARE -> RectangleShape
                                },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(imageItem.data ?: imageItem.filePath)
                                                .memoryCacheKey(imageItem.id)
                                                .build()
                                        ),
                                        contentDescription = imageItem.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    val isGlobalLast = archiveStructure != null && archiveStructure.lastImageId == imageItem.id
                                    val isLevelLast = lastImageIdLevel != null && lastImageIdLevel == imageItem.id

                                    if (isGlobalLast || isLevelLast) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isGlobalLast && isLevelLast) {
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = stringResource(R.string.continue_read),
                                                        modifier = Modifier.padding(4.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            } else {
                                                if (isLevelLast) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.secondary
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = stringResource(R.string.last_image_on_level),
                                                            modifier = Modifier.padding(4.dp),
                                                            tint = MaterialTheme.colorScheme.onSecondary
                                                        )
                                                    }
                                                }

                                                if (isGlobalLast) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = stringResource(R.string.continue_read),
                                                            modifier = Modifier.padding(4.dp),
                                                            tint = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ContextMenu(state = contextMenuState) { item ->
                    val sortedIndex = item as? Int
                    if (sortedIndex != null && sortedIndex >= 0 && sortedIndex < sortedImages.size) {
                        ContextMenuItem(
                            text = stringResource(R.string.create_preview),
                            onClick = {
                                contextMenuState.hide()
                                val fileIndex = sortedFiles.indexOfFirst { it.id == sortedImages[sortedIndex].id }
                                if (fileIndex >= 0) onMakePreview?.invoke(fileIndex)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        ContextMenuItem(
                            text = stringResource(R.string.copy),
                            onClick = {
                                contextMenuState.hide()
                                val fileIndex = sortedFiles.indexOfFirst { it.id == sortedImages[sortedIndex].id }
                                if (fileIndex >= 0) onCopyImage?.invoke(fileIndex)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}