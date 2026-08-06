package com.demushrenich.archim.domain.repositories

interface CacheRepository {
    suspend fun cleanupOrphanedPreviews(): Result<Int>
    suspend fun clearCacheDir(): Result<Unit>
    suspend fun clearLargeArchiveCache(): Result<Unit>
}