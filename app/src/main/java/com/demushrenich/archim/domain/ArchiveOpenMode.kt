package com.demushrenich.archim.domain

enum class ArchiveOpenMode(val code: String) {
    GRID("grid"),
    CONTINUE("continue");

    companion object {
        fun fromCode(code: String): ArchiveOpenMode {
            return entries.find { it.code == code } ?: GRID
        }
    }
}