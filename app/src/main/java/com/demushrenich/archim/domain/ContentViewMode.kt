package com.demushrenich.archim.domain

enum class ContentViewMode(val code: String) {
    LIST("list"),
    GRID("grid");

    companion object {
        fun fromCode(code: String): ContentViewMode {
            return entries.find { it.code == code } ?: LIST
        }
    }
}