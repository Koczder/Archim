package com.demushrenich.archim.domain.repositories

import com.demushrenich.archim.domain.*
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val currentLanguage: Flow<Language>
    val readingDirection: Flow<ReadingDirection>
    val previewGenerationMode: Flow<PreviewGenerationMode>
    val previewLoadingMode: Flow<PreviewLoadingMode>
    val backgroundMode: Flow<BackgroundMode>
    val archiveCornerStyle: Flow<CornerStyle>
    val imageCornerStyle: Flow<CornerStyle>
    val archiveOpenMode: Flow<ArchiveOpenMode>
    val addDirectoryButtonPosition: Flow<AddDirectoryButtonPosition>
    val contentViewMode: Flow<ContentViewMode>

    fun setLanguage(language: Language)
    fun setReadingDirection(direction: ReadingDirection)
    fun setPreviewGenerationMode(mode: PreviewGenerationMode)
    fun setPreviewLoadingMode(mode: PreviewLoadingMode)
    fun setBackgroundMode(mode: BackgroundMode)
    fun setArchiveCornerStyle(style: CornerStyle)
    fun setImageCornerStyle(style: CornerStyle)
    fun setArchiveOpenMode(mode: ArchiveOpenMode)
    fun setAddDirectoryButtonPosition(position: AddDirectoryButtonPosition)
    fun setContentViewMode(mode: ContentViewMode)
}