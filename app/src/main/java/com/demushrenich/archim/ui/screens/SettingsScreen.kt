package com.demushrenich.archim.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demushrenich.archim.R
import com.demushrenich.archim.domain.ArchiveOpenMode
import com.demushrenich.archim.domain.ReadingDirection
import com.demushrenich.archim.domain.Language
import com.demushrenich.archim.domain.PreviewGenerationMode
import com.demushrenich.archim.domain.PreviewLoadingMode
import com.demushrenich.archim.domain.CornerStyle
import com.demushrenich.archim.domain.AddDirectoryButtonPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    currentDirection: ReadingDirection,
    onDirectionChange: (ReadingDirection) -> Unit,
    currentPreviewMode: PreviewGenerationMode,
    onPreviewModeChange: (PreviewGenerationMode) -> Unit,
    currentLoadingMode: PreviewLoadingMode,
    onLoadingModeChange: (PreviewLoadingMode) -> Unit,
    currentArchiveCornerStyle: CornerStyle,
    onArchiveCornerStyleChange: (CornerStyle) -> Unit,
    currentImageCornerStyle: CornerStyle,
    onImageCornerStyleChange: (CornerStyle) -> Unit,
    currentArchiveOpenMode: ArchiveOpenMode,
    onArchiveOpenModeChange: (ArchiveOpenMode) -> Unit,
    currentAddDirectoryButtonPosition: AddDirectoryButtonPosition,
    onAddDirectoryButtonPositionChange: (AddDirectoryButtonPosition) -> Unit,
    onCleanupOrphanedPreviews: () -> Unit,
    onBackClick: () -> Unit
) {

    androidx.activity.compose.BackHandler(enabled = true) {
        onBackClick()
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_general)) {
                    LanguageSection(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(R.string.settings_group_viewer)) {
                    DirectionSection(
                        currentDirection = currentDirection,
                        onDirectionChange = onDirectionChange
                    )
                }
            }

            item {
                SettingsGroup(title = stringResource(R.string.settings_group_archives)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PreviewGenerationSection(
                            currentMode = currentPreviewMode,
                            onModeChange = onPreviewModeChange
                        )
                        PreviewLoadingSection(
                            currentMode = currentLoadingMode,
                            onModeChange = onLoadingModeChange
                        )
                        ArchiveCornerStyleSection(
                            currentStyle = currentArchiveCornerStyle,
                            onStyleChange = onArchiveCornerStyleChange
                        )
                        ArchiveOpenModeSection(
                            currentMode = currentArchiveOpenMode,
                            onModeChange = onArchiveOpenModeChange
                        )
                        AddDirectoryButtonPositionSection(
                            currentPosition = currentAddDirectoryButtonPosition,
                            onPositionChange = onAddDirectoryButtonPositionChange
                        )
                    }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_grid)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ImageCornerStyleSection(
                            currentStyle = currentImageCornerStyle,
                            onStyleChange = onImageCornerStyleChange
                        )
                        CleanupSection(
                            onCleanupClick = onCleanupOrphanedPreviews
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSection(
    currentLanguage: Language,
    onLanguageChange: (Language) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getLanguageDisplayName(currentLanguage),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Language.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(getLanguageDisplayName(language)) },
                        onClick = {
                            onLanguageChange(language)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSection(
    currentDirection: ReadingDirection,
    onDirectionChange: (ReadingDirection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_reading_direction),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = when (currentDirection) {
                    ReadingDirection.LEFT_TO_RIGHT -> stringResource(R.string.reading_direction_ltr)
                    ReadingDirection.RIGHT_TO_LEFT -> stringResource(R.string.reading_direction_rtl)
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReadingDirection.entries.forEach { dir ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (dir) {
                                    ReadingDirection.LEFT_TO_RIGHT -> stringResource(R.string.reading_direction_ltr)
                                    ReadingDirection.RIGHT_TO_LEFT -> stringResource(R.string.reading_direction_rtl)
                                }
                            )
                        },
                        onClick = {
                            onDirectionChange(dir)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewGenerationSection(
    currentMode: PreviewGenerationMode,
    onModeChange: (PreviewGenerationMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_preview_generation),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getPreviewModeDisplayName(currentMode),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PreviewGenerationMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(getPreviewModeDisplayName(mode)) },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewLoadingSection(
    currentMode: PreviewLoadingMode,
    onModeChange: (PreviewLoadingMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_preview_loading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getPreviewLoadingModeDisplayName(currentMode),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PreviewLoadingMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(getPreviewLoadingModeDisplayName(mode)) },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveCornerStyleSection(
    currentStyle: CornerStyle,
    onStyleChange: (CornerStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_archive_corners),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getCornerStyleDisplayName(currentStyle),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                CornerStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(getCornerStyleDisplayName(style)) },
                        onClick = {
                            onStyleChange(style)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageCornerStyleSection(
    currentStyle: CornerStyle,
    onStyleChange: (CornerStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_grid_corners),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getCornerStyleDisplayName(currentStyle),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                CornerStyle.entries.forEach { style ->
                    DropdownMenuItem(
                        text = { Text(getCornerStyleDisplayName(style)) },
                        onClick = {
                            onStyleChange(style)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveOpenModeSection(
    currentMode: ArchiveOpenMode,
    onModeChange: (ArchiveOpenMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_archive_open_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = when (currentMode) {
                    ArchiveOpenMode.GRID -> stringResource(R.string.archive_open_mode_grid)
                    ArchiveOpenMode.CONTINUE -> stringResource(R.string.archive_open_mode_continue)
                },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ArchiveOpenMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                when (mode) {
                                    ArchiveOpenMode.GRID -> stringResource(R.string.archive_open_mode_grid)
                                    ArchiveOpenMode.CONTINUE -> stringResource(R.string.archive_open_mode_continue)
                                }
                            )
                        },
                        onClick = {
                            onModeChange(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDirectoryButtonPositionSection(
    currentPosition: AddDirectoryButtonPosition,
    onPositionChange: (AddDirectoryButtonPosition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_add_directory_button_position),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = getAddDirectoryButtonPositionDisplayName(currentPosition),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AddDirectoryButtonPosition.entries.forEach { position ->
                    DropdownMenuItem(
                        text = { Text(getAddDirectoryButtonPositionDisplayName(position)) },
                        onClick = {
                            onPositionChange(position)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanupSection(
    onCleanupClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_cleanup_cache),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = onCleanupClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.settings_cleanup_button))
        }
    }
}

@Composable
private fun getLanguageDisplayName(language: Language): String {
    return when (language) {
        Language.SYSTEM -> stringResource(R.string.language_system)
        Language.ENGLISH -> stringResource(R.string.language_english)
        Language.RUSSIAN -> stringResource(R.string.language_russian)
        Language.INTERSLAVIC -> stringResource(R.string.language_interslavic)
    }
}

@Composable
private fun getPreviewModeDisplayName(mode: PreviewGenerationMode): String {
    return when (mode) {
        PreviewGenerationMode.DIALOG -> stringResource(R.string.preview_mode_dialog)
        PreviewGenerationMode.AUTO -> stringResource(R.string.preview_mode_auto)
        PreviewGenerationMode.MANUAL -> stringResource(R.string.preview_mode_manual)
    }
}

@Composable
private fun getPreviewLoadingModeDisplayName(mode: PreviewLoadingMode): String {
    return when (mode) {
        PreviewLoadingMode.FULL -> stringResource(R.string.preview_loading_mode_full)
        PreviewLoadingMode.DYNAMIC -> stringResource(R.string.preview_loading_mode_dynamic)
        PreviewLoadingMode.DYNAMIC_UNLOAD -> stringResource(R.string.preview_loading_mode_dynamic_unload)
    }
}

@Composable
private fun getCornerStyleDisplayName(style: CornerStyle): String {
    return when (style) {
        CornerStyle.ROUNDED -> stringResource(R.string.corner_style_rounded)
        CornerStyle.SQUARE -> stringResource(R.string.corner_style_square)
    }
}

@Composable
private fun getAddDirectoryButtonPositionDisplayName(position: AddDirectoryButtonPosition): String {
    return when (position) {
        AddDirectoryButtonPosition.TOP -> stringResource(R.string.add_directory_button_position_top)
        AddDirectoryButtonPosition.BOTTOM -> stringResource(R.string.add_directory_button_position_bottom)
        AddDirectoryButtonPosition.LEFT -> stringResource(R.string.add_directory_button_position_left)
        AddDirectoryButtonPosition.RIGHT -> stringResource(R.string.add_directory_button_position_right)
        AddDirectoryButtonPosition.BOTTOM_SIDE -> stringResource(R.string.add_directory_button_position_bottom_side)
        AddDirectoryButtonPosition.BNBOVERLAY -> stringResource(R.string.add_directory_button_position_bottom_overlay)

    }
}