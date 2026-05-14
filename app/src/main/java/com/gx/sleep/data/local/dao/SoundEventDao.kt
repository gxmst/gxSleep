package com.gx.sleep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gx.sleep.data.local.entity.SoundEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(events: List<SoundEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SoundEventEntity): Long

    @Query("SELECT * FROM sound_events WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun getBySessionId(sessionId: Long): Flow<List<SoundEventEntity>>

    @Query("SELECT * FROM sound_events WHERE sessionId = :sessionId ORDER BY startTime ASC")
    suspend fun getBySessionIdList(sessionId: Long): List<SoundEventEntity>

    @Query("SELECT * FROM sound_events WHERE id = :id")
    suspend fun getById(id: Long): SoundEventEntity?

    @Query("SELECT * FROM sound_events WHERE sessionId = :sessionId AND type = :type ORDER BY startTime ASC")
    suspend fun getByType(sessionId: Long, type: String): List<SoundEventEntity>

    @Query("SELECT COUNT(*) FROM sound_events WHERE sessionId = :sessionId")
    suspend fun getCountBySession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM sound_events WHERE sessionId = :sessionId AND type = :type")
    suspend fun getCountBySessionAndType(sessionId: Long, type: String): Int

    @Query("UPDATE sound_events SET audioClipPath = :audioClipPath WHERE id = :eventId")
    suspend fun updateAudioClipPath(eventId: Long, audioClipPath: String)

    @Query("DELETE FROM sound_events WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("DELETE FROM sound_events")
    suspend fun deleteAll()
}
