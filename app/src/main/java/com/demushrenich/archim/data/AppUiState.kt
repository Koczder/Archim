package com.demushrenich.archim.data

import android.net.Uri
import com.demushrenich.archim.domain.ArchiveOpenMode
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.Language
import com.demushrenich.archim.domain.PreviewGenerationMode
import com.demushrenich.archim.domain.ReadingDirection
import com.demushrenich.archim.domain.SortType
import com.demushrenich.archim.domain.BackgroundMode
import com.demushrenich.archim.domain.CornerStyle

data class ImageViewState(
    val images: List<ImageItem> = emptyList(),
    val selectedImageIndex: Int? = null,
    val sortType: SortType = SortType.NAME_ASC
)

data class ArchiveState(
    val currentArchiveUri: Uri? = null,
    val showPasswordDialog: Boolean = false,
    val passwordError: Boolean = false,
    val passwordErrorMessage: String = ""
)

data class LoadingState(
    val isLoading: Boolean = false,
    val extractionProgress: Float = 0f,
    val currentFileName: String = "",
    val errorMessage: String? = null
)

data class AppSettings(
    val currentLanguage: Language = Language.SYSTEM,
    val previewGenerationMode: PreviewGenerationMode = PreviewGenerationMode.DIALOG,
    val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val backgroundMode: BackgroundMode = BackgroundMode.SYSTEM,
    val archiveCornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val imageCornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val archiveOpenMode: ArchiveOpenMode = ArchiveOpenMode.GRID,
    val showSettings: Boolean = false
)

data class AppUiState(
    val imageView: ImageViewState = ImageViewState(),
    val archive: ArchiveState = ArchiveState(),
    val loading: LoadingState = LoadingState(),
    val settings: AppSettings = AppSettings()
)


fun AppUiState.updateImageView(block: ImageViewState.() -> ImageViewState): AppUiState {
    return copy(imageView = imageView.block())
}

fun AppUiState.updateArchive(block: ArchiveState.() -> ArchiveState): AppUiState {
    return copy(archive = archive.block())
}

fun AppUiState.updateLoading(block: LoadingState.() -> LoadingState): AppUiState {
    return copy(loading = loading.block())
}

fun AppUiState.updateSettings(block: AppSettings.() -> AppSettings): AppUiState {
    return copy(settings = settings.block())
}