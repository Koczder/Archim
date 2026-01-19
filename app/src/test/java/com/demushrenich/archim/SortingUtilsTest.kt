package com.demushrenich.archim

import com.demushrenich.archim.domain.utils.SortingUtils
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.SortCategory
import com.demushrenich.archim.domain.SortType
import org.junit.Assert.assertEquals
import org.junit.Test

class SortingUtilsTest {

    @Test
    fun toggleSortType_NAME_ASC_to_NAME_DESC() {
        val result = SortingUtils.toggleSortType(SortType.NAME_ASC, SortCategory.NAME)
        assertEquals(SortType.NAME_DESC, result)
    }

    @Test
    fun toggleSortType_NAME_DESC_to_NAME_ASC() {
        val result = SortingUtils.toggleSortType(SortType.NAME_DESC, SortCategory.NAME)
        assertEquals(SortType.NAME_ASC, result)
    }

    @Test
    fun toggleSortType_DATE_ASC_to_DATE_DESC() {
        val result = SortingUtils.toggleSortType(SortType.DATE_ASC, SortCategory.DATE)
        assertEquals(SortType.DATE_DESC, result)
    }

    @Test
    fun toggleSortType_DATE_DESC_to_DATE_ASC() {
        val result = SortingUtils.toggleSortType(SortType.DATE_DESC, SortCategory.DATE)
        assertEquals(SortType.DATE_ASC, result)
    }

    @Test
    fun toggleSortType_from_NAME_to_DATE_defaults_to_DATE_DESC() {
        val result = SortingUtils.toggleSortType(SortType.NAME_ASC, SortCategory.DATE)
        assertEquals(SortType.DATE_DESC, result)
    }

    @Test
    fun toggleSortType_from_DATE_to_NAME_defaults_to_NAME_ASC() {
        val result = SortingUtils.toggleSortType(SortType.DATE_ASC, SortCategory.NAME)
        assertEquals(SortType.NAME_ASC, result)
    }

    @Test
    fun sortImages_NAME_ASC_sorts_case_insensitively() {
        val images = listOf(
            ImageItem(fileName = "Z.jpg"),
            ImageItem(fileName = "a.JPG"),
            ImageItem(fileName = "B.png")
        )
        val sorted = SortingUtils.sortImages(images, SortType.NAME_ASC)
        assertEquals("a.JPG", sorted[0].fileName)
        assertEquals("B.png", sorted[1].fileName)
        assertEquals("Z.jpg", sorted[2].fileName)
    }

    @Test
    fun sortImages_NAME_DESC_reverse_order() {
        val images = listOf(
            ImageItem(fileName = "a.jpg"),
            ImageItem(fileName = "B.JPG"),
            ImageItem(fileName = "c.png")
        )
        val sorted = SortingUtils.sortImages(images, SortType.NAME_DESC)
        assertEquals("c.png", sorted[0].fileName)
        assertEquals("B.JPG", sorted[1].fileName)
        assertEquals("a.jpg", sorted[2].fileName)
    }

    @Test
    fun sortImages_DATE_ASC_orders_by_time_then_by_name() {
        val images = listOf(
            ImageItem(fileName = "no_date1.jpg", creationTime = 0L),
            ImageItem(fileName = "old.jpg", creationTime = 1000L),
            ImageItem(fileName = "new.jpg", creationTime = 3000L),
            ImageItem(fileName = "no_date2.jpg", creationTime = 0L)
        )
        val sorted = SortingUtils.sortImages(images, SortType.DATE_ASC)
        assertEquals("old.jpg", sorted[0].fileName)
        assertEquals("new.jpg", sorted[1].fileName)
        assertEquals("no_date1.jpg", sorted[2].fileName)
        assertEquals("no_date2.jpg", sorted[3].fileName)
    }

    @Test
    fun sortImages_DATE_DESC_newest_first_no_date_last() {
        val images = listOf(
            ImageItem(fileName = "no_date1.jpg", creationTime = 0L),
            ImageItem(fileName = "old.jpg", creationTime = 1000L),
            ImageItem(fileName = "new.jpg", creationTime = 3000L),
            ImageItem(fileName = "no_date2.jpg", creationTime = 0L)
        )
        val sorted = SortingUtils.sortImages(images, SortType.DATE_DESC)
        assertEquals("new.jpg", sorted[0].fileName)
        assertEquals("old.jpg", sorted[1].fileName)
        assertEquals("no_date1.jpg", sorted[2].fileName)
        assertEquals("no_date2.jpg", sorted[3].fileName)
    }

    @Test
    fun sortImages_DATE_DESC_same_time_sorted_by_name() {
        val images = listOf(
            ImageItem(fileName = "b.jpg", creationTime = 2000L),
            ImageItem(fileName = "a.jpg", creationTime = 2000L),
            ImageItem(fileName = "c.jpg", creationTime = 2000L)
        )
        val sorted = SortingUtils.sortImages(images, SortType.DATE_DESC)
        assertEquals("a.jpg", sorted[0].fileName)
        assertEquals("b.jpg", sorted[1].fileName)
        assertEquals("c.jpg", sorted[2].fileName)
    }

    @Test
    fun sortImages_empty_list_returns_empty() {
        val sorted = SortingUtils.sortImages(emptyList(), SortType.NAME_ASC)
        assertEquals(emptyList<ImageItem>(), sorted)
    }
}