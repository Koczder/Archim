package com.demushrenich.archim.domain

enum class PreviewGenerationMode(val code: String) {
    DIALOG("dialog"),
    AUTO("auto"),
    MANUAL("manual");

    companion object {
        fun fromCode(code: String): PreviewGenerationMode {
            return entries.find { it.code == code } ?: DIALOG
        }
    }
}


