package com.demushrenich.archim.data.managers

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import com.demushrenich.archim.domain.Language
import com.demushrenich.archim.domain.PreviewGenerationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import com.demushrenich.archim.domain.ArchiveOpenMode
import com.demushrenich.archim.domain.ReadingDirection
import com.demushrenich.archim.domain.BackgroundMode
import com.demushrenich.archim.domain.CornerStyle
import java.util.Locale

class SettingsManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "app_settings"
        private const val PREF_LANGUAGE = "language"
        private const val PREF_DIRECTION = "reading_direction"
        private const val PREF_PREVIEW_GENERATION = "preview_generation_mode"
        private const val PREF_BACKGROUND_MODE = "background_mode"
        private const val PREF_ARCHIVE_CORNER_STYLE = "archive_corner_style"
        private const val PREF_IMAGE_CORNER_STYLE = "image_corner_style"
        private const val PREF_ARCHIVE_OPEN_MODE = "archive_open_mode"
        private const val TAG = "SettingsManager"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(getCurrentLanguage())
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _readingDirection = MutableStateFlow(getCurrentDirection())
    val readingDirection: StateFlow<ReadingDirection> = _readingDirection.asStateFlow()

    private val _previewGenerationMode = MutableStateFlow(getCurrentPreviewGenerationMode())
    val previewGenerationMode: StateFlow<PreviewGenerationMode> = _previewGenerationMode.asStateFlow()

    private val _backgroundMode = MutableStateFlow(getCurrentBackgroundMode())
    val backgroundMode: StateFlow<BackgroundMode> = _backgroundMode.asStateFlow()

    private val _archiveCornerStyle = MutableStateFlow(getCurrentArchiveCornerStyle())
    val archiveCornerStyle: StateFlow<CornerStyle> = _archiveCornerStyle.asStateFlow()

    private val _imageCornerStyle = MutableStateFlow(getCurrentImageCornerStyle())
    val imageCornerStyle: StateFlow<CornerStyle> = _imageCornerStyle.asStateFlow()

    private val _archiveOpenMode = MutableStateFlow(getCurrentArchiveOpenMode())
    val archiveOpenMode: StateFlow<ArchiveOpenMode> = _archiveOpenMode.asStateFlow()

    init {
        Log.d(TAG, "SettingsManager initialized")
        Log.d(TAG, "Current preview generation mode: ${_previewGenerationMode.value}")
        Log.d(TAG, "Current background mode: ${_backgroundMode.value}")
        Log.d(TAG, "Current archive corner style: ${_archiveCornerStyle.value}")
        Log.d(TAG, "Current image corner style: ${_imageCornerStyle.value}")
    }

    private fun getCurrentLanguage(): Language {
        val languageCode = prefs.getString(PREF_LANGUAGE, Language.SYSTEM.code) ?: Language.SYSTEM.code
        return Language.fromCode(languageCode)
    }

    private fun getCurrentDirection(): ReadingDirection {
        val dir = prefs.getString(PREF_DIRECTION, ReadingDirection.LEFT_TO_RIGHT.name)
        return ReadingDirection.valueOf(dir!!)
    }

    private fun getCurrentPreviewGenerationMode(): PreviewGenerationMode {
        val modeCode = prefs.getString(PREF_PREVIEW_GENERATION, PreviewGenerationMode.DIALOG.code)
            ?: PreviewGenerationMode.DIALOG.code
        Log.d(TAG, "getCurrentPreviewGenerationMode: saved code=$modeCode")
        val mode = PreviewGenerationMode.fromCode(modeCode)
        Log.d(TAG, "getCurrentPreviewGenerationMode: resolved mode=$mode")
        return mode
    }

    private fun getCurrentBackgroundMode(): BackgroundMode {
        val modeName = prefs.getString(PREF_BACKGROUND_MODE, BackgroundMode.SYSTEM.name)
            ?: BackgroundMode.SYSTEM.name
        Log.d(TAG, "getCurrentBackgroundMode: saved name=$modeName")
        return try {
            BackgroundMode.valueOf(modeName)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid background mode: $modeName, using SYSTEM")
            BackgroundMode.SYSTEM
        }
    }

    private fun getCurrentArchiveCornerStyle(): CornerStyle {
        val styleCode = prefs.getString(PREF_ARCHIVE_CORNER_STYLE, CornerStyle.ROUNDED.code)
            ?: CornerStyle.ROUNDED.code
        return CornerStyle.fromCode(styleCode)
    }

    private fun getCurrentImageCornerStyle(): CornerStyle {
        val styleCode = prefs.getString(PREF_IMAGE_CORNER_STYLE, CornerStyle.ROUNDED.code)
            ?: CornerStyle.ROUNDED.code
        return CornerStyle.fromCode(styleCode)
    }

    private fun getCurrentArchiveOpenMode(): ArchiveOpenMode {
        val modeCode = prefs.getString(PREF_ARCHIVE_OPEN_MODE, ArchiveOpenMode.GRID.code)
            ?: ArchiveOpenMode.GRID.code
        return ArchiveOpenMode.fromCode(modeCode)
    }

    fun setArchiveOpenMode(mode: ArchiveOpenMode) {
        Log.d(TAG, "setArchiveOpenMode: $mode")
        prefs.edit { putString(PREF_ARCHIVE_OPEN_MODE, mode.code) }
        _archiveOpenMode.value = mode
        Log.d(TAG, "ArchiveOpenMode saved and StateFlow updated")
    }

    fun setReadingDirection(direction: ReadingDirection) {
        Log.d(TAG, "setReadingDirection: $direction")
        prefs.edit { putString(PREF_DIRECTION, direction.name) }
        _readingDirection.value = direction
    }

    fun setPreviewGenerationMode(mode: PreviewGenerationMode) {
        Log.d(TAG, "setPreviewGenerationMode: $mode (code=${mode.code})")
        prefs.edit { putString(PREF_PREVIEW_GENERATION, mode.code) }
        _previewGenerationMode.value = mode
        Log.d(TAG, "PreviewGenerationMode saved and StateFlow updated")
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        Log.d(TAG, "setBackgroundMode: $mode")
        prefs.edit { putString(PREF_BACKGROUND_MODE, mode.name) }
        _backgroundMode.value = mode
        Log.d(TAG, "BackgroundMode saved and StateFlow updated")
    }

    fun setArchiveCornerStyle(style: CornerStyle) {
        Log.d(TAG, "setArchiveCornerStyle: $style")
        prefs.edit { putString(PREF_ARCHIVE_CORNER_STYLE, style.code) }
        _archiveCornerStyle.value = style
        Log.d(TAG, "ArchiveCornerStyle saved and StateFlow updated")
    }

    fun setImageCornerStyle(style: CornerStyle) {
        Log.d(TAG, "setImageCornerStyle: $style")
        prefs.edit { putString(PREF_IMAGE_CORNER_STYLE, style.code) }
        _imageCornerStyle.value = style
        Log.d(TAG, "ImageCornerStyle saved and StateFlow updated")
    }

    fun setLanguage(language: Language) {
        Log.d(TAG, "setLanguage: $language")
        prefs.edit { putString(PREF_LANGUAGE, language.code) }

        try {
            applyLanguage(language)
            _currentLanguage.value = language
        } catch (e: Exception) {
            Log.e(TAG, "setLanguage: error applying language", e)
        }
    }

    private fun applyLanguage(language: Language) {
        val chosenLocale = when (language) {
            Language.ENGLISH -> Locale.forLanguageTag("en")
            Language.RUSSIAN -> Locale.forLanguageTag("ru")
            Language.SYSTEM -> {
                val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales.get(0)
                } else {
                    @Suppress("DEPRECATION")
                    Resources.getSystem().configuration.locale
                }
                systemLocale
            }
        }
        Locale.setDefault(chosenLocale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(chosenLocale)

        val localizedContext = try {
            context.createConfigurationContext(config)
        } catch (e: Exception) {
            Log.e(TAG, "applyLanguage: createConfigurationContext threw", e)
            null
        }
        localizedContext?.let {
            Log.d(TAG, "applyLanguage: localizedContext.configuration = ${configInfo(it.resources.configuration)}")
        } ?: Log.d(TAG, "applyLanguage: localizedContext is null")

        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            Log.e(TAG, "applyLanguage: updateConfiguration failed", e)
        }
    }

    private fun configInfo(cfg: Configuration): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                cfg.locales.toLanguageTags()
            } catch (t: Throwable) {
                "locales=<error>"
            }
        } else {
            @Suppress("DEPRECATION")
            cfg.locale.toString()
        }
    }

    fun initializeLanguage() {
        val lang = getCurrentLanguage()
        try {
            applyLanguage(lang)
        } catch (e: Exception) {
            Log.e(TAG, "initializeLanguage: error", e)
        }
    }
}