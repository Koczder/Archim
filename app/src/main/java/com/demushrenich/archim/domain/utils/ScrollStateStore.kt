package com.demushrenich.archim.domain.utils

data class ScrollStateData(
    val index: Int,
    val offset: Int
)

private val scrollMap = mutableMapOf<String, ScrollStateData>()

fun getSavedScroll(key: String): ScrollStateData =
    scrollMap[key] ?: ScrollStateData(0, 0)

fun saveScroll(key: String, index: Int, offset: Int) {
    scrollMap[key] = ScrollStateData(index, offset)
}
