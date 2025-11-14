package com.demushrenich.archim.domain

import java.util.UUID

data class ImageItem(
    val filePath: String? = null,
    val data: ByteArray? = null,
    val fileName: String,
    val creationTime: Long = 0L,
    val archivePath: String = "",
    val isFolder: Boolean = false
) {
    val id: String = generateArchivePathId()

    private fun generateArchivePathId(): String {
        return when {
            archivePath.isNotEmpty() && fileName.isNotEmpty() -> {
                if (archivePath == "/" || archivePath == "." || archivePath.isEmpty()) {
                    fileName
                } else {
                    val normalizedPath = if (archivePath.startsWith("/")) {
                        archivePath.substring(1)
                    } else {
                        archivePath
                    }
                    "$normalizedPath/$fileName/$creationTime"
                }
            }
            else -> fileName
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageItem

        if (id != other.id) return false
        if (filePath != other.filePath) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (fileName != other.fileName) return false
        if (creationTime != other.creationTime) return false
        if (archivePath != other.archivePath) return false
        if (isFolder != other.isFolder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + fileName.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + archivePath.hashCode()
        result = 31 * result + isFolder.hashCode()
        return result
    }
}
