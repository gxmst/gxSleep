package com.gx.sleep.ui.screens.sessiondetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gx.sleep.GxSleepApp
import com.gx.sleep.analysis.SessionReportGenerator
import com.gx.sleep.data.repository.SleepRepository
import com.gx.sleep.domain.model.SessionReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // P3: Fetch data and generate report on background dispatcher
                val report = withContext(Dispatchers.IO) {
                    val session = repository.getSessionById(sessionId) ?: return@withContext null
                    val samples = repository.getSamplesBySession(sessionId)
                    val events = repository.getEventsBySessionList(sessionId)
                    withContext(Dispatchers.Default) {
                        SessionReportGenerator.generate(
                            sessionId = sessionId,
                            startTime = session.startTime,
                            endTime = session.endTime,
                            samples = samples,
                            events = events,
                            baselineRms = 50f
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
}
