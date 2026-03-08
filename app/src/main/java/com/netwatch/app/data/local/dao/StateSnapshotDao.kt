package com.netwatch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netwatch.app.data.local.entity.StateSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StateSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: StateSnapshotEntity): Long

    @Query("SELECT * FROM state_snapshots ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<StateSnapshotEntity?>

    @Query("SELECT * FROM state_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StateSnapshotEntity>>

    @Query("SELECT * FROM state_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getSince(sinceMs: Long): List<StateSnapshotEntity>
}
