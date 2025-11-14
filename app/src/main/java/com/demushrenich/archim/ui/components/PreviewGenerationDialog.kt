package com.demushrenich.archim.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.demushrenich.archim.R

@Composable
fun PreviewGenerationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { stringResource(R.string.preview_generation_dialog_title) },
            text = {
                stringResource(R.string.preview_generation_dialog_message)
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    stringResource(R.string.yes)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    stringResource(R.string.no)
                }
            }
        )
    }
}