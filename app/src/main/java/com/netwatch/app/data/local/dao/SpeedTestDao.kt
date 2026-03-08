package com.netwatch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netwatch.app.data.local.entity.SpeedTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: SpeedTestResultEntity): Long

    @Query("SELECT * FROM speed_test_results ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SpeedTestResultEntity>>

    @Query(
        """
        SELECT COALESCE(AVG(downloadMbps), 0.0)
        FROM speed_test_results
        WHERE timestampMs >= :sinceMs
        """
    )
    suspend fun averageDownloadSince(sinceMs: Long): Double

    @Query(
        """
        SELECT COALESCE(AVG(uploadMbps), 0.0)
        FROM speed_test_results
        WHERE timestampMs >= :sinceMs
        """
    )
    suspend fun averageUploadSince(sinceMs: Long): Double

    @Query(
        """
        SELECT COALESCE(AVG(latencyMs), 0.0)
        FROM speed_test_results
        WHERE timestampMs >= :sinceMs
        """
    )
    suspend fun averageLatencySince(sinceMs: Long): Double
}
