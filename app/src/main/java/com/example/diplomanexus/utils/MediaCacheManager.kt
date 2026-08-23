package com.example.diplomanexus.utils

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.DefaultHttpDataSource
import java.io.File

object MediaCacheManager {
    private var simpleCache: SimpleCache? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getInstance(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheSize: Long = 100 * 1024 * 1024 // 100MB max cache
                val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
                val databaseProvider = StandaloneDatabaseProvider(context)
                val cacheDir = File(context.cacheDir, "media3_cache")
                val cache = SimpleCache(cacheDir, evictor, databaseProvider)
                simpleCache = cache
                cache
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getInstance(context)
        val upstreamFactory = DefaultHttpDataSource.Factory()
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
