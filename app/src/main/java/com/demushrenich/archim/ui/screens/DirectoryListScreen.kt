package com.demushrenich.archim.ui.screens

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.demushrenich.archim.R
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.data.managers.DirectoryManager
import com.demushrenich.archim.data.DirectoryArchivesInfo
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun DirectoryListScreen(
    directories: List<DirectoryItem>,
    newDirectoryUris: Set<String> = emptySet(),
    onAddClick: () -> Unit,
    onOpenDirectory: (DirectoryItem, Boolean) -> Unit,
    onDeleteDirectory: (DirectoryItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var directoryToDelete by remember { mutableStateOf<DirectoryItem?>(null) }
    var archivesInfo by remember { mutableStateOf(DirectoryArchivesInfo(0, 0, 0)) }
    var deletePreviewsAndProgress by remember { mutableStateOf(true) }
    var isCalculating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val stableDirectories by remember(directories) { derivedStateOf { directories } }
    val stableNewUris by remember(newDirectoryUris) { derivedStateOf { newDirectoryUris } }

    fun calculateArchivesInfo(directory: DirectoryItem) {
        coroutineScope.launch {
            delay(300L)
            isCalculating = true
            try {
                val info = withContext(Dispatchers.IO) {
                    val freshDirectories = DirectoryManager.loadSavedDirectories(context)
                    DirectoryManager.getArchivesInfoForDirectory(
                        context = context,
                        directoryUri = directory.uri,
                        allDirectories = freshDirectories
                    )
                }
                archivesInfo = info
            } catch (e: Exception) {
                archivesInfo = DirectoryArchivesInfo(0, 0, 0)
                Log.e("DirectoryListScreen", "Error calculating archives info", e)
            } finally {
                isCalculating = false
            }
        }
    }

    if (showDeleteDialog && directoryToDelete != null) {
        DeleteDirectoryDialog(
            directory = directoryToDelete!!,
            archivesInfo = archivesInfo,
            deletePreviewsAndProgress = deletePreviewsAndProgress,
            isCalculating = isCalculating,
            isDeleting = isDeleting,
            onDeletePreviewsCheckedChange = { deletePreviewsAndProgress = it },
            onDismiss = {
                if (!isDeleting && !isCalculating) {
                    showDeleteDialog = false
                    directoryToDelete = null
                    deletePreviewsAndProgress = true
                    archivesInfo = DirectoryArchivesInfo(0, 0, 0)
                }
            },
            onConfirmDelete = {
                directoryToDelete?.let { directory ->
                    coroutineScope.launch {
                        isDeleting = true
                        try {
                            withContext(Dispatchers.IO) {
                                if (deletePreviewsAndProgress && archivesInfo.archivesToDeleteCount > 0) {
                                    val freshDirectories = DirectoryManager.loadSavedDirectories(context)
                                    DirectoryManager.removeAllPreviewsAndProgressForDirectory(
                                        context = context,
                                        directoryUri = directory.uri,
                                        allDirectories = freshDirectories
                                    )
                                }
                            }
                            withContext(Dispatchers.Main) {
                                onDeleteDirectory(directory)
                                showDeleteDialog = false
                                directoryToDelete = null
                                deletePreviewsAndProgress = true
                                isDeleting = false
                                isCalculating = false
                            }
                        } catch (e: Exception) {
                            Log.e("DirectoryListScreen", "Error deleting directory data", e)
                            isDeleting = false
                        }
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(stringResource(R.string.add_directory))
        }

        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = stableDirectories,
                key = { it.uri }
            ) { item ->
                val isNewDirectory by remember(item.uri, stableNewUris) {
                    derivedStateOf { stableNewUris.contains(item.uri) }
                }
                val dateText by remember(item.lastModified) {
                    derivedStateOf {
                        if (item.lastModified > 0)
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                .format(Date(item.lastModified))
                        else "—"
                    }
                }
                DirectoryItemCard(
                    item = item,
                    isNewDirectory = isNewDirectory,
                    dateText = dateText,
                    onOpenDirectory = onOpenDirectory,
                    onDeleteClick = {
                        directoryToDelete = item
                        deletePreviewsAndProgress = true
                        archivesInfo = DirectoryArchivesInfo(0, 0, 0)
                        showDeleteDialog = true
                        calculateArchivesInfo(item)
                    }
                )
            }
        }
    }
}

@Composable
fun DeleteDirectoryDialog(
    directory: DirectoryItem,
    archivesInfo: DirectoryArchivesInfo,
    deletePreviewsAndProgress: Boolean,
    isCalculating: Boolean,
    isDeleting: Boolean,
    onDeletePreviewsCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_directory),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(stringResource(R.string.delete_directory_confirm, directory.displayName))


                if (isCalculating) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.archives_counting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isDeleting) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.deleting_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (archivesInfo.totalArchivesCount > 0) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = if (deletePreviewsAndProgress)
                                    stringResource(R.string.will_delete_previews)
                                else
                                    stringResource(R.string.keep_previews),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (deletePreviewsAndProgress)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (archivesInfo.archivesToDeleteCount > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = deletePreviewsAndProgress,
                                onCheckedChange = onDeletePreviewsCheckedChange,
                                enabled = true
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.delete_previews_checkbox),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = !isCalculating && !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.counting_in_progress), color = MaterialTheme.colorScheme.onError)
                } else if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.deleting_in_progress), color = MaterialTheme.colorScheme.onError)
                } else {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onError)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting && !isCalculating) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


@Composable
private fun DirectoryItemCard(
    item: DirectoryItem,
    isNewDirectory: Boolean,
    dateText: String,
    onOpenDirectory: (DirectoryItem, Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
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