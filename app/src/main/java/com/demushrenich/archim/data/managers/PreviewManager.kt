package com.demushrenich.archim.data.managers

import android.content.Context
import android.content.SharedPreferences
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.demushrenich.archim.data.PreviewInfo
import com.demushrenich.archim.data.ReadingProgress
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PreviewManager {
    private const val PREVIEW_PREFS = "preview_metadata"
    private const val PREVIEWS_KEY = "previews"
    private const val TAG = "PreviewManager"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREVIEW_PREFS, Context.MODE_PRIVATE)
    }

    private fun getArchiveKey(fileName: String, fileSize: Long): String {
        return "${fileName}_${fileSize}".hashCode().toString()
    }

    fun getPreviewPath(context: Context, fileName: String? = null, fileSize: Long? = null): String? {
        if (fileName == null || fileSize == null || fileSize <= 0) {
            return null
        }

        val prefs = getPrefs(context)
        val previewsJson = prefs.getString(PREVIEWS_KEY, null)

        if (previewsJson == null) {
            return null
        }

        return try {
            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: Map<String, PreviewInfo> = Gson().fromJson(previewsJson, type)
            val key = getArchiveKey(fileName, fileSize)
            val previewInfo = previews[key]

            if (previewInfo != null) {
                val previewFile = File(previewInfo.previewPath)
                if (previewFile.exists() && previewFile.length() > 0) {
                    return previewInfo.previewPath
                } else {
                    removeInvalidPreview(context, key)
                    return null
                }
            } else {
                Log.d(TAG, "getPreviewPath: not found in map for $fileName (key=$key)")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "getPreviewPath: error parsing prefs", e)
            null
        }
    }

    fun saveReadingProgressForPreview(
        context: Context,
        archiveUri: String,
        fileName: String,
        fileSize: Long,
        currentIndex: Int,
        totalImages: Int
    ) {
        if (fileSize <= 0) {
            return
        }

        val prefs = getPrefs(context)
        val previewsJson = prefs.getString(PREVIEWS_KEY, "{}")
        try {
            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: MutableMap<String, PreviewInfo> = Gson().fromJson(previewsJson, type) ?: mutableMapOf()
            val key = getArchiveKey(fileName, fileSize)
            val existingPreview = previews[key]

            val readingProgress = ReadingProgress(
                currentIndex = currentIndex,
                totalImages = totalImages,
                lastReadTimestamp = System.currentTimeMillis()
            )

            val updatedPreview = existingPreview?.copy(readingProgress = readingProgress)
                ?: PreviewInfo(
                    archiveUri = archiveUri,
                    previewPath = "",
                    timestamp = System.currentTimeMillis(),
                    readingProgress = readingProgress
                )

            previews[key] = updatedPreview
            val updatedJson = Gson().toJson(previews)
            prefs.edit { putString(PREVIEWS_KEY, updatedJson) }

            Log.d(TAG, "saveReadingProgressForPreview: saved $currentIndex/$totalImages for $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "saveReadingProgressForPreview: error", e)
        }
    }

    fun getReadingProgressForPreview(
        context: Context,
        fileName: String? = null,
        fileSize: Long? = null
    ): ReadingProgress? {
        if (fileName == null || fileSize == null || fileSize <= 0) {
            return null
        }

        val prefs = getPrefs(context)
        val previewsJson = prefs.getString(PREVIEWS_KEY, null) ?: return null

        return try {
            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: Map<String, PreviewInfo> = Gson().fromJson(previewsJson, type)

            val key = getArchiveKey(fileName, fileSize)
            val previewInfo = previews[key]

            previewInfo?.readingProgress
        } catch (e: Exception) {
            Log.e(TAG, "getReadingProgressForPreview: error", e)
            null
        }
    }

    fun removePreviewAndProgressByUri(context: Context, archiveUri: String) {
        val prefs = getPrefs(context)
        val previewsJson = prefs.getString(PREVIEWS_KEY, "{}")

        fun normalizeUri(uri: String): String {
            return try {
                val parsed = uri.toUri()
                DocumentsContract.getDocumentId(parsed)
            } catch (_: Exception) {
                uri
            }
        }

        try {
            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: MutableMap<String, PreviewInfo> = Gson().fromJson(previewsJson, type) ?: mutableMapOf()
            val normTarget = normalizeUri(archiveUri)

            val keysToRemove = previews.filterValues {
                normalizeUri(it.archiveUri) == normTarget
            }.keys.toList()

            keysToRemove.forEach { key ->
                val previewInfo = previews[key]
                if (previewInfo != null && previewInfo.previewPath.isNotEmpty()) {
                    try {
                        val previewFile = File(previewInfo.previewPath)
                        if (previewFile.exists()) {
                            previewFile.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "removePreviewAndProgressByUri: couldn't delete file", e)
                    }
                }
                previews.remove(key)
            }

            val updatedJson = Gson().toJson(previews)
            prefs.edit { putString(PREVIEWS_KEY, updatedJson) }

            Log.d(TAG, "removePreviewAndProgressByUri: removed ${keysToRemove.size} entries")
        } catch (e: Exception) {
            Log.e(TAG, "removePreviewAndProgressByUri: error", e)
        }
    }

    private fun removeInvalidPreview(context: Context, key: String) {
        try {
            val prefs = getPrefs(context)
            val previewsJson = prefs.getString(PREVIEWS_KEY, "{}")
            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: MutableMap<String, PreviewInfo> = Gson().fromJson(previewsJson, type) ?: mutableMapOf()

            val previewInfo = previews[key]
            if (previewInfo != null && previewInfo.previewPath.isNotEmpty()) {
                try {
                    val previewFile = File(previewInfo.previewPath)
                    if (previewFile.exists()) {
                        previewFile.delete()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "removeInvalidPreview: couldn't delete file", e)
                }
            }

            previews.remove(key)
            val updatedJson = Gson().toJson(previews)
            prefs.edit { putString(PREVIEWS_KEY, updatedJson) }
        } catch (e: Exception) {
            Log.e(TAG, "removeInvalidPreview: error", e)
        }
    }

    private fun getPreviewsMap(prefs: SharedPreferences): MutableMap<String, PreviewInfo> {
        val json = prefs.getString(PREVIEWS_KEY, null)
        if (json.isNullOrBlank()) return mutableMapOf()

        return try {
            val type = object : TypeToken<MutableMap<String, PreviewInfo>>() {}.type
            Gson().fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            Log.e(TAG, "getPreviewsMap: error", e)
            mutableMapOf()
        }
    }

    private fun updatePrefsSafely(prefs: SharedPreferences, map: MutableMap<String, PreviewInfo>) {
        val json = Gson().toJson(map)
        prefs.edit(commit = true) {
            putString(PREVIEWS_KEY, json)
        }
    }

    fun savePreviewPath(
        context: Context,
        archiveUri: String,
        previewPath: String,
        fileName: String,
        fileSize: Long
    ) {
        if (fileSize <= 0) return

        try {
            val prefs = getPrefs(context)
            val previews = getPreviewsMap(prefs)
            val key = getArchiveKey(fileName, fileSize)

            val updatedPreview = previews[key]?.copy(
                previewPath = previewPath,
                timestamp = System.currentTimeMillis()
            ) ?: PreviewInfo(
                archiveUri = archiveUri,
                previewPath = previewPath,
                timestamp = System.currentTimeMillis()
            )

            previews[key] = updatedPreview
            updatePrefsSafely(prefs, previews)

            Log.d(TAG, "savePreviewPath: saved preview for $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "savePreviewPath: error", e)
        }
    }

    fun cleanupOrphanedPreviews(context: Context): Int {
        var deletedCount = 0
        try {
            val prefs = getPrefs(context)
            val previewsJson = prefs.getString(PREVIEWS_KEY, "{}")

            val type = object : TypeToken<Map<String, PreviewInfo>>() {}.type
            val previews: Map<String, PreviewInfo> = Gson().fromJson(previewsJson, type) ?: emptyMap()

            val registeredFileNames = previews.values
                .mapNotNull { previewInfo ->
                    if (previewInfo.previewPath.isNotEmpty()) {
                        File(previewInfo.previewPath).name
                    } else null
                }
                .toSet()

            val previewDir = File(context.filesDir, "previews")
            if (!previewDir.exists()) {
                return 0
            }

            val allFiles = previewDir.listFiles() ?: emptyArray()

            allFiles.forEach { file ->
                if (file.isFile && !registeredFileNames.contains(file.name)) {
                    try {
                        if (file.delete()) {
                            deletedCount++
                            Log.d(TAG, "cleanupOrphanedPreviews: deleted orphaned file ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "cleanupOrphanedPreviews: couldn't delete file ${file.name}", e)
                    }
                }
            }

            Log.d(TAG, "cleanupOrphanedPreviews: deleted $deletedCount orphaned files")
        } catch (e: Exception) {
            Log.e(TAG, "cleanupOrphanedPreviews: error", e)
        }
        return deletedCount
    }
}