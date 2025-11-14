package com.demushrenich.archim.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.R
import com.demushrenich.archim.data.NavigationState

@Composable
fun NavigationBreadcrumbs(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    val breadcrumbs = navigationState.getBreadcrumbs()

    if (breadcrumbs.isNotEmpty()) {
        val listState = rememberLazyListState()

        LaunchedEffect(breadcrumbs.size) {
            listState.animateScrollToItem(breadcrumbs.lastIndex)
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            items(breadcrumbs) { (level, name) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLast = level == breadcrumbs.last().first

                    TextButton(
                        onClick = {
                            if (!isLast) navigationState.navigateToLevel(level)
                        },
                        enabled = !isLast,
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier.heightIn(min = 24.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isLast)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (!isLast) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationStatusCard(
    isLoading: Boolean = false,
    error: String? = null,
    progressMessage: String = "",
    isProcessing: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    when {
        error != null -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        isLoading -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        isProcessing && progressMessage.isNotEmpty() -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        !isProcessing && progressMessage.isNotEmpty() -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = progressMessage,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}