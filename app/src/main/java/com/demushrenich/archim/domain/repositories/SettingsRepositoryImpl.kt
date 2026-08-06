package com.demushrenich.archim.data.repositories

import android.content.Context
import com.demushrenich.archim.data.managers.SettingsManager
import com.demushrenich.archim.domain.*
import com.demushrenich.archim.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val settingsManager = SettingsManager(context).apply {
        initializeLanguage()
    }

    override val currentLanguage: Flow<Language>
        get() = settingsManager.currentLanguage

    override val readingDirection: Flow<ReadingDirection>
        get() = settingsManager.readingDirection

    override val previewGenerationMode: Flow<PreviewGenerationMode>
        get() = settingsManager.previewGenerationMode

    override val previewLoadingMode: Flow<PreviewLoadingMode>
        get() = settingsManager.previewLoadingMode

    override val backgroundMode: Flow<BackgroundMode>
        get() = settingsManager.backgroundMode

    override val archiveCornerStyle: Flow<CornerStyle>
        get() = settingsManager.archiveCornerStyle

    override val imageCornerStyle: Flow<CornerStyle>
        get() = settingsManager.imageCornerStyle

    override val archiveOpenMode: Flow<ArchiveOpenMode>
        get() = settingsManager.archiveOpenMode

    override val addDirectoryButtonPosition: Flow<AddDirectoryButtonPosition>
        get() = settingsManager.addDirectoryButtonPosition

    override val contentViewMode: Flow<ContentViewMode>
        get() = settingsManager.contentViewMode

    override fun setLanguage(language: Language) {
        settingsManager.setLanguage(language)
    }

    override fun setReadingDirection(direction: ReadingDirection) {
        settingsManager.setReadingDirection(direction)
    }

    override fun setPreviewGenerationMode(mode: PreviewGenerationMode) {
        settingsManager.setPreviewGenerationMode(mode)
    }

    override fun setPreviewLoadingMode(mode: PreviewLoadingMode) {
        settingsManager.setPreviewLoadingMode(mode)
    }

    override fun setBackgroundMode(mode: BackgroundMode) {
        settingsManager.setBackgroundMode(mode)
    }

    override fun setArchiveCornerStyle(style: CornerStyle) {
        settingsManager.setArchiveCornerStyle(style)
    }

    override fun setImageCornerStyle(style: CornerStyle) {
        settingsManager.setImageCornerStyle(style)
    }

    override fun setArchiveOpenMode(mode: ArchiveOpenMode) {
        settingsManager.setArchiveOpenMode(mode)
    }

    override fun setAddDirectoryButtonPosition(position: AddDirectoryButtonPosition) {
        settingsManager.setAddDirectoryButtonPosition(position)
    }

    override fun setContentViewMode(mode: ContentViewMode) {
        settingsManager.setContentViewMode(mode)
    }
}