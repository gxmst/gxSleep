package com.gx.sleep.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gx.sleep.GxSleepApp
import com.gx.sleep.analysis.SessionReportGenerator
import com.gx.sleep.data.local.entity.SessionStatus
import com.gx.sleep.data.repository.SleepRepository
import com.gx.sleep.domain.model.SessionReport
import com.gx.sleep.service.SleepRecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class HomeUiState(
    val isRecording: Boolean = false,
    val currentRms: Float = 0f,
    val currentDbfs: Float = -120f,
    val eventCount: Int = 0,
    val sessionId: Long? = null,
    val lastReport: SessionReport? = null,
    val recentEventCounts: List<Int> = emptyList(), // event counts for last 7 sessions
    val hasAudioPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lastSessionCrashed: Boolean = false
)

@OptIn(FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GxSleepApp
    private val repository = SleepRepository(
        app.database.sleepSessionDao(),
        app.database.soundSampleDao(),
        app.database.soundEventDao()
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRecordingState()
        observeErrors()
        loadLatestSession()
        loadRecentSessions()
        checkForCrashedSession()
    }

    private fun observeRecordingState() {
        viewModelScope.launch {
            SleepRecordingService.isRecording.collect { recording ->
                _uiState.value = _uiState.value.copy(isRecording = recording, errorMessage = null)
            }
        }
        viewModelScope.launch {
            SleepRecordingService.currentRms
                .debounce(300)
                .collect { rms ->
                    _uiState.value = _uiState.value.copy(currentRms = rms)
                }
        }
        viewModelScope.launch {
            SleepRecordingService.currentDbfs.collect { dbfs ->
                _uiState.value = _uiState.value.copy(currentDbfs = dbfs)
            }
        }
        viewModelScope.launch {
            SleepRecordingService.eventCount.collect { count ->
                _uiState.value = _uiState.value.copy(eventCount = count)
            }
        }
        viewModelScope.launch {
            SleepRecordingService.sessionId.collect { id ->
                _uiState.value = _uiState.value.copy(sessionId = id)
            }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            SleepRecordingService.error.collect { errorInfo ->
                _uiState.value = _uiState.value.copy(errorMessage = errorInfo.message)
            }
        }
    }

    private fun loadLatestSession() {
        viewModelScope.launch {
            repository.getAllSessions().collectLatest { sessions ->
                val normalSession = sessions.firstOrNull {
                    it.status != SessionStatus.RUNNING && !it.isShortSession
                }
                if (normalSession != null) {
                    try {
                        val report = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            val samples = repository.getSamplesBySession(normalSession.id)
                            val events = repository.getEventsBySessionList(normalSession.id)
                            kotlinx.coroutines.withContext(Dispatchers.Default) {
                                SessionReportGenerator.generate(
                                    sessionId = normalSession.id,
                                    startTime = normalSession.startTime,
                                    endTime = normalSession.endTime,
                                    samples = samples,
                                    events = events,
                                    baselineRms = normalSession.baselineRms
                                )
                            }
                        }
                        _uiState.value = _uiState.value.copy(lastReport = report, isLoading = false)
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    private fun loadRecentSessions() {
        viewModelScope.launch {
            repository.getAllSessions().collectLatest { sessions ->
                val recent = sessions
                    .filter { it.status == SessionStatus.COMPLETED && !it.isShortSession }
                    .take(7)
                    .reversed()
                    .map { session ->
                        repository.getEventsBySessionList(session.id).size
                    }
                _uiState.value = _uiState.value.copy(recentEventCounts = recent)
            }
        }
    }

    private fun checkForCrashedSession() {
        viewModelScope.launch {
            repository.getLatestSession().collectLatest { session ->
                if (session != null && session.status == SessionStatus.CRASHED) {
                    _uiState.value = _uiState.value.copy(lastSessionCrashed = true)
                }
            }
        }
    }

    fun dismissCrashedWarning() {
        _uiState.value = _uiState.value.copy(lastSessionCrashed = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updatePermissionState(audioGranted: Boolean, notificationGranted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasAudioPermission = audioGranted,
            hasNotificationPermission = notificationGranted
        )
    }

    fun startRecording() {
        SleepRecordingService.startService(getApplication())
    }

    fun stopRecording() {
        SleepRecordingService.stopService(getApplication())
    }
}
