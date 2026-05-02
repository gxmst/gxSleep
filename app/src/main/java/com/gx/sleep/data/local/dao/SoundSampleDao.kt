package com.gx.sleep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gx.sleep.data.local.entity.SoundSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(samples: List<SoundSampleEntity>)

    @Query("SELECT * FROM sound_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySessionId(sessionId: Long): Flow<List<SoundSampleEntity>>

    @Query("SELECT * FROM sound_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionIdList(sessionId: Long): List<SoundSampleEntity>

    @Query("SELECT * FROM sound_samples WHERE sessionId = :sessionId AND timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getByTimeRange(sessionId: Long, start: Long, end: Long): List<SoundSampleEntity>

    @Query("DELETE FROM sound_samples WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("DELETE FROM sound_samples")
    suspend fun deleteAll()
}
