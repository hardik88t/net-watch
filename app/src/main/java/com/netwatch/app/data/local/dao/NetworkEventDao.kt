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

    /** Observe timeline entries ordered newest-first, excluding marked exceptions. */
    @Query("SELECT * FROM network_events WHERE isException = 0 ORDER BY timestampMs DESC LIMIT :limit")
    fun observeTimeline(limit: Int): Flow<List<NetworkEventEntity>>

    /** Observe ALL entries including exceptions (used for admin/debug views). */
    @Query("SELECT * FROM network_events ORDER BY timestampMs DESC LIMIT :limit")
    fun observeTimelineAll(limit: Int): Flow<List<NetworkEventEntity>>

    @Query("UPDATE network_events SET isException = :isException WHERE id = :id")
    suspend fun setException(id: Long, isException: Boolean)

    @Query("DELETE FROM network_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
