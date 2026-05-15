package com.gx.sleep.ui.screens.sessiondetail

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gx.sleep.GxSleepApp
import com.gx.sleep.analysis.SessionReportGenerator
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.repository.SleepRepository
import com.gx.sleep.domain.model.SessionReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlaybackState(
    val playingEventId: Long = -1,
    val isPlaying: Boolean = false
)

class SessionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GxSleepApp
    private val repository = SleepRepository(
        app.database.sleepSessionDao(),
        app.database.soundSampleDao(),
        app.database.soundEventDao()
    )

    private val _report = MutableStateFlow<SessionReport?>(null)
    val report: StateFlow<SessionReport?> = _report.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _eventsWithClips = MutableStateFlow<List<SoundEventEntity>>(emptyList())
    val eventsWithClips: StateFlow<List<SoundEventEntity>> = _eventsWithClips.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val report = withContext(Dispatchers.IO) {
                    val session = repository.getSessionById(sessionId) ?: return@withContext null
                    val samples = repository.getSamplesBySession(sessionId)
                    val events = repository.getEventsBySessionList(sessionId)
                    _eventsWithClips.value = events.filter { it.audioClipPath != null }
                    withContext(Dispatchers.Default) {
                        SessionReportGenerator.generate(
                            sessionId = sessionId,
                            startTime = session.startTime,
                            endTime = session.endTime,
                            samples = samples,
                            events = events,
                            baselineRms = session.baselineRms
                        )
                    }
                }
                _report.value = report
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePlayback(eventId: Long, audioClipPath: String) {
        if (_playbackState.value.playingEventId == eventId && _playbackState.value.isPlaying) {
            stopPlayback()
            return
        }

        stopPlayback()

        val file = File(audioClipPath)
        if (!file.exists()) {
            _playbackError.value = "音频文件不存在"
            return
        }

        try {
            val mp = MediaPlayer()
            mediaPlayer = mp

            mp.setOnCompletionListener {
                try {
                    if (mediaPlayer == null) return@setOnCompletionListener
                    _playbackState.value = PlaybackState()
                } catch (_: Exception) {}
            }

            mp.setOnErrorListener { _, _, _ ->
                try {
                    if (mediaPlayer == null) return@setOnErrorListener true
                    _playbackState.value = PlaybackState()
                } catch (_: Exception) {}
                true
            }

            mp.setDataSource(audioClipPath)
            mp.prepare()
            mp.start()
            _playbackState.value = PlaybackState(playingEventId = eventId, isPlaying = true)
        } catch (e: Exception) {
            _playbackState.value = PlaybackState()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun dismissPlaybackError() {
        _playbackError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
