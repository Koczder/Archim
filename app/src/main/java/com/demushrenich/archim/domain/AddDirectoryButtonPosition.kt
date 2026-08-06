package com.demushrenich.archim.domain

enum class AddDirectoryButtonPosition(val code: String) {
    TOP("top"),
    BOTTOM("bottom"),
    LEFT("left"),
    RIGHT("right"),
    BOTTOM_SIDE("bottom_side"),

    BNBOVERLAY("bnb_overlay");

    companion object {
        fun fromCode(code: String): AddDirectoryButtonPosition =
            entries.find { it.code == code } ?: TOP
    }
}