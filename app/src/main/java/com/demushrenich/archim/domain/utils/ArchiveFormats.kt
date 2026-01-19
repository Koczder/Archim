package com.demushrenich.archim.domain.utils

import com.demushrenich.archim.data.ArchiveInfo
import java.util.Locale

object ArchiveFormats {
    val SUPPORTED_EXTENSIONS = setOf(
        "7z", "zip", "rar", "tar", "bz2", "xz", "lzma",
        "cab", "iso", "arj", "lzh", "chm", "cpio", "deb", "rpm",
        "wim", "xar", "z", "cbz", "cbr", "cb7"
    )
}

fun isSupportedArchive(extension: String): Boolean =
    ArchiveFormats.SUPPORTED_EXTENSIONS.contains(extension.lowercase())

fun archiveFormat(archive: ArchiveInfo): String {
    val nameExt = archive.displayName.substringAfterLast('.', "")
    if (nameExt.isNotEmpty()) {
        return nameExt.uppercase(Locale.getDefault())
    }

    return archive.filePath
        .substringAfterLast('.', "")
        .uppercase(Locale.getDefault())
        ?: "ARCHIVE"
}
