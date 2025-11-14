package com.demushrenich.archim.domain

enum class Language(val code: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromCode(code: String): Language {
            return Language.entries.find { it.code == code } ?: SYSTEM
        }
    }
}