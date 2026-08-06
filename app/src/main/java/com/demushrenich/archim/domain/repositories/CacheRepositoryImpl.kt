package com.demushrenich.archim.data.repositories

import android.content.Context
import com.demushrenich.archim.data.managers.PreviewManager
import com.demushrenich.archim.domain.repositories.CacheRepository
import com.demushrenich.archim.domain.utils.clearCacheDir
import com.demushrenich.archim.domain.utils.clearLargeArchiveCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CacheRepositoryImpl(
    private val context: Context
) : CacheRepository {

    override suspend fun cleanupOrphanedPreviews(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedCount = PreviewManager.cleanupOrphanedPreviews(context)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCacheDir(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            clearCacheDir(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearLargeArchiveCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            clearLargeArchiveCache(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}