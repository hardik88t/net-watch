package com.netwatch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netwatch.app.data.local.entity.NetworkEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: NetworkEventEntity): Long

    @Query("SELECT * FROM network_events ORDER BY timestampMs DESC LIMIT :limit")
    fun observeTimeline(limit: Int): Flow<List<NetworkEventEntity>>
}
