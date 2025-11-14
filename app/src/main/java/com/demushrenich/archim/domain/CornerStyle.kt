package com.demushrenich.archim.domain

enum class CornerStyle(val code: String) {
    ROUNDED("rounded"),
    SQUARE("square");

    companion object {
        fun fromCode(code: String): CornerStyle {
            return entries.find { it.code == code } ?: ROUNDED
        }
    }
}