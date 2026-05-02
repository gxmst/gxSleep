package com.gx.sleep.data.repository

import com.gx.sleep.data.local.dao.SleepSessionDao
import com.gx.sleep.data.local.dao.SoundEventDao
import com.gx.sleep.data.local.dao.SoundSampleDao
import com.gx.sleep.data.local.entity.SessionStatus
import com.gx.sleep.data.local.entity.SleepSessionEntity
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.local.entity.SoundSampleEntity
import kotlinx.coroutines.flow.Flow

class SleepRepository(
    private val sessionDao: SleepSessionDao,
    private val sampleDao: SoundSampleDao,
    private val eventDao: SoundEventDao
) {
    // Session operations
    fun getAllSessions(): Flow<List<SleepSessionEntity>> = sessionDao.getAllSessions()

    fun getLatestSession(): Flow<SleepSessionEntity?> = sessionDao.getLatestSession()

    fun getRunningSessionFlow(): Flow<SleepSessionEntity?> = sessionDao.getRunningSessionFlow()

    suspend fun getRunningSession(): SleepSessionEntity? = sessionDao.getRunningSession()

    suspend fun getSessionById(id: Long): SleepSessionEntity? = sessionDao.getById(id)

    fun getSessionByIdFlow(id: Long): Flow<SleepSessionEntity?> = sessionDao.getByIdFlow(id)

    suspend fun createSession(
        sampleRate: Int,
        audioSaveMode: String,
        batteryPercent: Int,
        deviceInfo: String,
        appVersion: String
    ): Long {
        val session = SleepSessionEntity(
            startTime = System.currentTimeMillis(),
            status = SessionStatus.RUNNING,
            sampleRate = sampleRate,
            audioSaveMode = audioSaveMode,
            batteryStartPercent = batteryPercent,
            deviceInfo = deviceInfo,
            appVersion = appVersion
        )
        return sessionDao.insert(session)
    }

    suspend fun completeSession(sessionId: Long, batteryPercent: Int) {
        val session = sessionDao.getById(sessionId) ?: return
        val now = System.currentTimeMillis()
        sessionDao.update(
            session.copy(
                endTime = now,
                duration = now - session.startTime,
                status = SessionStatus.COMPLETED,
                batteryEndPercent = batteryPercent
            )
        )
    }

    suspend fun markSessionCrashed(sessionId: Long) {
        val session = sessionDao.getById(sessionId) ?: return
        val now = System.currentTimeMillis()
        sessionDao.update(
            session.copy(
                endTime = now,
                duration = now - session.startTime,
                status = SessionStatus.CRASHED
            )
        )
    }

    suspend fun markRunningSessionsAsCrashed() {
        val running = sessionDao.getRunningSession()
        if (running != null) {
            markSessionCrashed(running.id)
        }
    }

    // Sample operations
    suspend fun insertSamples(samples: List<SoundSampleEntity>) {
        sampleDao.insertBatch(samples)
    }

    suspend fun getSamplesBySession(sessionId: Long): List<SoundSampleEntity> {
        return sampleDao.getBySessionIdList(sessionId)
    }

    // Event operations
    suspend fun insertEvents(events: List<SoundEventEntity>) {
        eventDao.insertBatch(events)
    }

    suspend fun insertEvent(event: SoundEventEntity): Long {
        return eventDao.insert(event)
    }

    fun getEventsBySession(sessionId: Long): Flow<List<SoundEventEntity>> {
        return eventDao.getBySessionId(sessionId)
    }

    suspend fun getEventsBySessionList(sessionId: Long): List<SoundEventEntity> {
        return eventDao.getBySessionIdList(sessionId)
    }

    suspend fun getEventById(id: Long): SoundEventEntity? {
        return eventDao.getById(id)
    }

    suspend fun getEventCountByType(sessionId: Long, type: String): Int {
        return eventDao.getCountBySessionAndType(sessionId, type)
    }

    suspend fun getEventCounts(sessionId: Long): Map<String, Int> {
        val types = listOf("SNORE_LIKE", "SPEECH_LIKE", "COUGH_LIKE", "IMPACT_NOISE", "ENVIRONMENT_NOISE", "UNKNOWN")
        return types.associateWith { eventDao.getCountBySessionAndType(sessionId, it) }
    }

    // Delete operations
    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    suspend fun deleteAllData() {
        eventDao.deleteAll()
        sampleDao.deleteAll()
        sessionDao.deleteAll()
    }
}
