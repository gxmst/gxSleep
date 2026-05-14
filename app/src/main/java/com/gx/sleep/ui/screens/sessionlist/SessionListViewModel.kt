package com.gx.sleep.ui.screens.sessionlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gx.sleep.GxSleepApp
import com.gx.sleep.data.local.entity.SleepSessionEntity
import com.gx.sleep.data.repository.SleepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SessionListViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GxSleepApp
    private val repository = SleepRepository(
        app.database.sleepSessionDao(),
        app.database.soundSampleDao(),
        app.database.soundEventDao()
    )

    val sessions = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun deleteSession(id: Long) {
        repository.deleteSession(id)
    }

    fun refresh() {}
}
