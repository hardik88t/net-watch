package com.netwatch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netwatch.app.data.local.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity): Long

    @Query("SELECT * FROM annotations WHERE eventId = :eventId ORDER BY timestampMs DESC")
    fun observeForEvent(eventId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<AnnotationEntity>>
}
