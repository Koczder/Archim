package com.demushrenich.archim.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.data.AppUiState
import com.demushrenich.archim.R

@Composable
fun ArchivePageScreen(
    uiState: AppUiState,
    onPickArchive: () -> Unit,
    onEnterPassword: () -> Unit,
    onShowSettings: () -> Unit
) {
    if (uiState.settings.showSettings) return

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onPickArchive) {
                Text(stringResource(R.string.pick_archive))
            }

            uiState.loading.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
                if (error.contains("password", ignoreCase = true)) {
                    Button(
                        onClick = onEnterPassword,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        stringResource(R.string.enter_password)
                    }
                }
            }
        }

        IconButton(
            onClick = onShowSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings)
            )
        }
    }
}