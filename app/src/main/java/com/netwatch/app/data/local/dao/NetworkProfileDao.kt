package com.netwatch.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netwatch.app.data.local.entity.NetworkProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: NetworkProfileEntity)

    @Query("SELECT * FROM network_profiles ORDER BY lastSeenAtMs DESC")
    fun observeAll(): Flow<List<NetworkProfileEntity>>
}
