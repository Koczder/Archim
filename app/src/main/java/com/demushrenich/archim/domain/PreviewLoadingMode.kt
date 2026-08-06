package com.demushrenich.archim.domain

enum class PreviewLoadingMode(val code: String) {
    FULL("full"),
    DYNAMIC("dynamic"),
    DYNAMIC_UNLOAD("dynamic_unload");

    companion object {
        fun fromCode(code: String): PreviewLoadingMode =
            entries.find { it.code == code } ?: DYNAMIC_UNLOAD
    }
}