package com.demushrenich.archim.ui.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.R
import com.demushrenich.archim.data.DirectoryItem
import com.demushrenich.archim.data.managers.DirectoryManager
import com.demushrenich.archim.data.DirectoryArchivesInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeleteDirectoryDialog(
    directory: DirectoryItem,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var archivesInfo by remember { mutableStateOf(DirectoryArchivesInfo(0, 0, 0)) }
    var deletePreviewsAndProgress by remember { mutableStateOf(true) }
    var isCalculating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(directory) {
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
            Log.e("DeleteDirectoryDialog", "Error calculating archives info", e)
        } finally {
            isCalculating = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting && !isCalculating) {
                onDismiss()
            }
        },
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
                                onCheckedChange = { deletePreviewsAndProgress = it },
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
                onClick = {
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
                                onConfirmDelete()
                            }
                        } catch (e: Exception) {
                            Log.e("DeleteDirectoryDialog", "Error deleting directory data", e)
                            isDeleting = false
                        }
                    }
                },
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
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting && !isCalculating
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}