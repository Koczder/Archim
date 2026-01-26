package com.demushrenich.archim.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.R
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.ui.dialogs.DeleteDirectoryDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DirectoryListScreen(
    directories: List<DirectoryItem>,
    newDirectoryUris: Set<String> = emptySet(),
    onAddClick: () -> Unit,
    onOpenDirectory: (DirectoryItem, Boolean) -> Unit,
    onDeleteDirectory: (DirectoryItem) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var directoryToDelete by remember { mutableStateOf<DirectoryItem?>(null) }

    val stableDirectories by remember(directories) { derivedStateOf { directories } }
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = stableDirectories,
                key = { it.uri },
                contentType = { "directory_item" }
            ) { item ->
                val isNewDirectory = remember(item.uri, stableNewUris) {
                    stableNewUris.contains(item.uri)
                }
                val dateText = remember(item.lastModified) {
                    if (item.lastModified > 0)
                        dateFormatter.format(Date(item.lastModified))
                    else "—"
                }
                DirectoryItemCard(
                    item = item,
                    isNewDirectory = isNewDirectory,
                    dateText = dateText,
                    onOpenDirectory = onOpenDirectory,
                    onDeleteClick = {
                        directoryToDelete = item
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
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