package com.demushrenich.archim.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.demushrenich.archim.R
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.domain.AddDirectoryButtonPosition
import com.demushrenich.archim.ui.dialogs.DeleteDirectoryDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DirectoryListScreen(
    directories: List<DirectoryItem>,
    newDirectoryUris: Set<String> = emptySet(),
    addButtonPosition: AddDirectoryButtonPosition = AddDirectoryButtonPosition.TOP,
    onAddClick: () -> Unit,
    onOpenDirectory: (DirectoryItem, Boolean) -> Unit,
    onDeleteDirectory: (DirectoryItem) -> Unit,
    onReorder: (List<DirectoryItem>) -> Unit = {}
) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    var directoryToDelete by remember { mutableStateOf<DirectoryItem?>(null) }

    val itemHeights = remember { mutableStateMapOf<String, Int>() }
    var draggedUri by remember { mutableStateOf<String?>(null) }
    var draggedOffsetY by remember { mutableStateOf(0f) }

    var localDirectories by remember { mutableStateOf(directories) }
    LaunchedEffect(directories) {
        if (draggedUri == null) {
            localDirectories = directories
        }
    }

    val stableNewUris by remember(newDirectoryUris) { derivedStateOf { newDirectoryUris } }

    val dateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    }

    if (showDeleteDialog && directoryToDelete != null) {
        DeleteDirectoryDialog(
            directory = directoryToDelete!!,
            onDismiss = {
                showDeleteDialog = false
                directoryToDelete = null
            },
            onConfirmDelete = {
                directoryToDelete?.let { directory ->
                    onDeleteDirectory(directory)
                    showDeleteDialog = false
                    directoryToDelete = null
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (addButtonPosition == AddDirectoryButtonPosition.TOP) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(stringResource(R.string.add_directory))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = localDirectories,
                    key = { _, item -> item.uri },
                    contentType = { _, _ -> "directory_item" }
                ) { _, item ->
                    val isNewDirectory = remember(item.uri, stableNewUris) {
                        stableNewUris.contains(item.uri)
                    }
                    val dateText = remember(item.lastModified) {
                        if (item.lastModified > 0)
                            dateFormatter.format(Date(item.lastModified))
                        else "—"
                    }
                    val isDragged = draggedUri == item.uri

                    DirectoryItemCard(
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragged) draggedOffsetY else 0f
                            }
                            .alpha(if (isDragged) 0.9f else 1f)
                            .onGloballyPositioned { coords ->
                                itemHeights[item.uri] = coords.size.height
                            },
                        item = item,
                        isNewDirectory = isNewDirectory,
                        dateText = dateText,
                        onOpenDirectory = onOpenDirectory,
                        onDeleteClick = {
                            directoryToDelete = item
                            showDeleteDialog = true
                        },
                        dragHandleModifier = Modifier.pointerInput(item.uri) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedUri = item.uri
                                    draggedOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggedUri = null
                                    draggedOffsetY = 0f
                                    onReorder(localDirectories)
                                },
                                onDragCancel = {
                                    draggedUri = null
                                    draggedOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentUri = draggedUri ?: return@detectDragGestures
                                    draggedOffsetY += dragAmount.y

                                    val currentIndex = localDirectories.indexOfFirst { it.uri == currentUri }
                                    if (currentIndex == -1) return@detectDragGestures
                                    val currentHeight = itemHeights[currentUri] ?: return@detectDragGestures

                                    if (draggedOffsetY > currentHeight / 2 && currentIndex < localDirectories.lastIndex) {
                                        localDirectories = localDirectories.toMutableList().apply {
                                            add(currentIndex + 1, removeAt(currentIndex))
                                        }
                                        draggedOffsetY -= currentHeight
                                    } else if (draggedOffsetY < -currentHeight / 2 && currentIndex > 0) {
                                        localDirectories = localDirectories.toMutableList().apply {
                                            add(currentIndex - 1, removeAt(currentIndex))
                                        }
                                        draggedOffsetY += currentHeight
                                    }
                                }
                            )
                        }
                    )
                }
            }

            if (addButtonPosition == AddDirectoryButtonPosition.BOTTOM) {
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(stringResource(R.string.add_directory))
                }
            }
        }

        if (addButtonPosition == AddDirectoryButtonPosition.RIGHT) {
            SideAddDirectoryButton(
                onAddClick = onAddClick,
                isRight = true,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (addButtonPosition == AddDirectoryButtonPosition.LEFT) {
            SideAddDirectoryButton(
                onAddClick = onAddClick,
                isRight = false,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        if (addButtonPosition == AddDirectoryButtonPosition.BOTTOM_SIDE) {
            SideAddDirectoryButton(
                onAddClick = onAddClick,
                edge = SideButtonEdge.BOTTOM,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SideAddDirectoryButton(
    onAddClick: () -> Unit,
    isRight: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = if (isRight) {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Surface(
        modifier = modifier
            .padding(vertical = 32.dp)
            .size(width = 40.dp, height = 64.dp)
            .clickable(onClick = onAddClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_directory),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private enum class SideButtonEdge { LEFT, RIGHT, BOTTOM }

@Composable
private fun SideAddDirectoryButton(
    onAddClick: () -> Unit,
    edge: SideButtonEdge,
    modifier: Modifier = Modifier
) {
    val shape = when (edge) {
        SideButtonEdge.RIGHT -> RoundedCornerShape(
            topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp
        )
        SideButtonEdge.LEFT -> RoundedCornerShape(
            topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp
        )
        SideButtonEdge.BOTTOM -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp
        )
    }

    val sizeModifier = if (edge == SideButtonEdge.BOTTOM) {
        Modifier
            .padding(horizontal = 32.dp)
            .size(width = 64.dp, height = 40.dp)
    } else {
        Modifier
            .padding(vertical = 32.dp)
            .size(width = 40.dp, height = 64.dp)
    }

    Surface(
        modifier = modifier
            .then(sizeModifier)
            .clickable(onClick = onAddClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_directory),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun DirectoryItemCard(
    item: DirectoryItem,
    isNewDirectory: Boolean,
    dateText: String,
    onOpenDirectory: (DirectoryItem, Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = if (isNewDirectory) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.reorder_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .then(dragHandleModifier)
            )

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 12.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenDirectory(item, isNewDirectory) }
                    .padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isNewDirectory) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.new_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.date_label, dateText),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}