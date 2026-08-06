package com.demushrenich.archim

import com.demushrenich.archim.data.ArchiveLevelData
import com.demushrenich.archim.data.ArchiveStructure
import com.demushrenich.archim.domain.SortType
import org.junit.Assert.*
import org.junit.Test

class ArchiveStructureTest {

    @Test
    fun getTotalReadCount_returns_sum_of_all_levels() {
        val levels = listOf(
            ArchiveLevelData(path = "/level1", imageIds = listOf("1", "2"), readCount = 5),
            ArchiveLevelData(path = "/level2", imageIds = listOf("3", "4"), readCount = 3),
            ArchiveLevelData(path = "/level3", imageIds = listOf("5"), readCount = 7)
        )

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = levels
        )

        assertEquals(15, structure.getTotalReadCount())
    }

    @Test
    fun getTotalReadCount_empty_levels_returns_zero() {
        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = emptyList()
        )

        assertEquals(0, structure.getTotalReadCount())
    }

    @Test
    fun getProgressPercentage_returns_correct_float_value() {
        val levels = listOf(
            ArchiveLevelData(path = "/level1", imageIds = listOf("1", "2"), readCount = 5)
        )

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = levels
        )

        assertEquals(0.5f, structure.getProgressPercentage(), 0.01f)
    }

    @Test
    fun getProgressPercentage_zero_total_images_returns_zero() {
        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 0,
            levels = listOf(
                ArchiveLevelData(path = "/level1", imageIds = listOf("1"), readCount = 5)
            )
        )

        assertEquals(0f, structure.getProgressPercentage(), 0.01f)
    }

    @Test
    fun getProgressPercentage_coerced_to_maximum_1f() {
        val levels = listOf(
            ArchiveLevelData(path = "/level1", imageIds = listOf("1", "2"), readCount = 15)
        )

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = levels
        )

        assertEquals(1f, structure.getProgressPercentage(), 0.01f)
    }

    @Test
    fun isCompleted_returns_true_when_read_count_equals_total() {
        val levels = listOf(
            ArchiveLevelData(path = "/level1", imageIds = listOf("1", "2"), readCount = 10)
        )

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = levels
        )

        assertTrue(structure.isCompleted())
    }

    @Test
    fun isCompleted_returns_false_when_read_count_less_than_total() {
        val levels = listOf(
            ArchiveLevelData(path = "/level1", imageIds = listOf("1", "2"), readCount = 5)
        )

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = levels
        )

        assertFalse(structure.isCompleted())
    }

    @Test
    fun archiveLevelData_default_sort_type_is_NAME_ASC() {
        val levelData = ArchiveLevelData(
            path = "/test",
            imageIds = listOf("1", "2", "3")
        )

        assertEquals(SortType.NAME_ASC, levelData.sortType)
    }

    @Test
    fun archiveLevelData_default_read_count_is_zero() {
        val levelData = ArchiveLevelData(
            path = "/test",
            imageIds = listOf("1", "2", "3")
        )

        assertEquals(0, levelData.readCount)
    }

    @Test
    fun archiveLevelData_default_lastImageIdLevel_is_null() {
        val levelData = ArchiveLevelData(
            path = "/test",
            imageIds = listOf("1", "2", "3")
        )

        assertNull(levelData.lastImageIdLevel)
    }

    @Test
    fun archiveStructure_lastModified_defaults_to_current_time() {
        val beforeCreation = System.currentTimeMillis()

        val structure = ArchiveStructure(
            archiveUri = "content://test",
            fileName = "test.zip",
            fileSize = 1024L,
            totalImages = 10,
            levels = emptyList()
        )

        val afterCreation = System.currentTimeMillis()

        assertTrue(structure.lastModified >= beforeCreation)
        assertTrue(structure.lastModified <= afterCreation)
    }
}