package com.gx.sleep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gx.sleep.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getById(id: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE status = 'RUNNING' LIMIT 1")
    suspend fun getRunningSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE status = 'RUNNING' LIMIT 1")
    fun getRunningSessionFlow(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSession(): Flow<SleepSessionEntity?>

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAll()
}
