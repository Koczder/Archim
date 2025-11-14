package com.demushrenich.archim.data

data class ArchiveInfo(
    val filePath: String,
    val originalName: String,
    val displayName: String,
    val lastModified: Long,
    val fileSize: Long = 0L,
    val previewPath: String? = null,
    val readingProgress: ReadingProgress? = null
)

data class DirectoryItem(
    val uri: String,
    val displayName: String,
    val isFolder: Boolean,
    val lastModified: Long
)

data class DirectoryArchivesInfo(
    val totalArchivesCount: Int,
    val archivesToDeleteCount: Int,
    val skippedDirectoriesCount: Int
)


