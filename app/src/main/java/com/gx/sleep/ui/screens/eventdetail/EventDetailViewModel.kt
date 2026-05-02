package com.gx.sleep.ui.screens.eventdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gx.sleep.GxSleepApp
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.repository.SleepRepository
import com.gx.sleep.domain.model.SoundEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GxSleepApp
    private val repository = SleepRepository(
        app.database.sleepSessionDao(),
        app.database.soundSampleDao(),
        app.database.soundEventDao()
    )

    private val _event = MutableStateFlow<SoundEventEntity?>(null)
    val event: StateFlow<SoundEventEntity?> = _event.asStateFlow()

    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            _event.value = repository.getEventById(eventId)
        }
    }
}
