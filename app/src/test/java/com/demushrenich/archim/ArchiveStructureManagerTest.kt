package com.demushrenich.archim

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.demushrenich.archim.data.ArchiveLevelData
import com.demushrenich.archim.data.ArchiveNavigationState
import com.demushrenich.archim.data.ArchiveStructure
import com.demushrenich.archim.data.managers.ArchiveStructureManager
import com.demushrenich.archim.domain.ImageItem
import com.demushrenich.archim.domain.SortType
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ArchiveStructureManagerTest {

    private lateinit var context: Context
    private val testFileName = "test_archive.zip"
    private val testFileSize = 1024L
    private val testArchiveUri = "content://test/archive.zip"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cleanupTestFiles()
    }

    @After
    fun tearDown() {
        cleanupTestFiles()
    }

    private fun cleanupTestFiles() {
        val structuresDir = File(context.filesDir, "archive_structures")
        if (structuresDir.exists()) {
            structuresDir.listFiles()?.forEach { it.delete() }
        }
    }

    @Test
    fun saveArchiveStructure_creates_file_in_correct_directory() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        val structuresDir = File(context.filesDir, "archive_structures")
        assertTrue(structuresDir.exists())
        assertTrue(structuresDir.listFiles()?.isNotEmpty() == true)
    }

    @Test
    fun loadArchiveStructure_returns_null_for_nonexistent_file() {
        val structure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = "nonexistent.zip",
            fileSize = 9999L
        )

        assertNull(structure)
    }

    @Test
    fun loadArchiveStructure_returns_saved_structure() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        assertNotNull(loadedStructure)
        assertEquals(testFileName, loadedStructure?.fileName)
        assertEquals(testFileSize, loadedStructure?.fileSize)
        assertTrue(loadedStructure?.levels?.isNotEmpty() == true)
    }

    @Test
    fun updateLastImageId_updates_structure_correctly() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        ArchiveStructureManager.updateLastImageId(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize,
            lastImageId = "image_123"
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        assertEquals("image_123", loadedStructure?.lastImageId)
    }

    @Test
    fun updateLevelImageIds_updates_correct_level() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        val newImageIds = listOf("img_a", "img_b", "img_c")

        ArchiveStructureManager.updateLevelImageIds(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize,
            levelPath = "",
            imageIds = newImageIds
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        val level = loadedStructure?.levels?.find { it.path == "" }
        assertNotNull(level)
        assertEquals(newImageIds, level?.imageIds)
    }

    @Test
    fun updateLevelReadCount_updates_correct_level() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        ArchiveStructureManager.updateLevelReadCount(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize,
            levelPath = "",
            readCount = 42
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        val level = loadedStructure?.levels?.find { it.path == "" }
        assertNotNull(level)
        assertEquals(42, level?.readCount)
    }

    @Test
    fun updateLastImageIdLevel_updates_correct_level() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        ArchiveStructureManager.updateLastImageIdLevel(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize,
            levelPath = "",
            lastImageIdLevel = "last_img_456"
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        val level = loadedStructure?.levels?.find { it.path == "" }
        assertNotNull(level)
        assertEquals("last_img_456", level?.lastImageIdLevel)
    }

    @Test
    fun deleteArchiveStructure_removes_file() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState
        )

        ArchiveStructureManager.deleteArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        assertNull(loadedStructure)
    }

    @Test
    fun saveArchiveStructure_with_zero_file_size_does_not_save() {
        val navState = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = 0L,
            archiveNavState = navState
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = 0L
        )

        assertNull(loadedStructure)
    }

    @Test
    fun saveArchiveStructure_merges_with_existing_structure() {
        val navState1 = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState1,
            currentSortType = SortType.DATE_ASC,
            currentLevelPath = ""
        )

        val navState2 = createTestNavigationState()

        ArchiveStructureManager.saveArchiveStructure(
            context = context,
            archiveUri = testArchiveUri,
            fileName = testFileName,
            fileSize = testFileSize,
            archiveNavState = navState2,
            currentSortType = SortType.NAME_DESC,
            currentLevelPath = ""
        )

        val loadedStructure = ArchiveStructureManager.loadArchiveStructure(
            context = context,
            fileName = testFileName,
            fileSize = testFileSize
        )

        val level = loadedStructure?.levels?.find { it.path == "" }
        assertNotNull(level)
        assertEquals(SortType.NAME_DESC, level?.sortType)
    }

    private fun createTestNavigationState(): ArchiveNavigationState {
        val testImages = listOf(
            ImageItem(fileName = "img1.jpg", archivePath = "", creationTime = 1000L, isFolder = false),
            ImageItem(fileName = "img2.jpg", archivePath = "", creationTime = 2000L, isFolder = false),
            ImageItem(fileName = "folder1", archivePath = "folder1", creationTime = 3000L, isFolder = true),
            ImageItem(fileName = "img3.jpg", archivePath = "folder1", creationTime = 4000L, isFolder = false)
        )
        val navState = ArchiveNavigationState(testImages)
        navState.setRootLevel()
        return navState
    }
}