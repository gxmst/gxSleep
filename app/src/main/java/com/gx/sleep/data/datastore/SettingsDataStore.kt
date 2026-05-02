package com.gx.sleep.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AudioSaveMode(val value: String) {
    STATS_ONLY("STATS_ONLY"),
    EVENT_CLIPS("EVENT_CLIPS"),
    FULL_RECORDING("FULL_RECORDING")
}

data class AppSettings(
    val audioSaveMode: AudioSaveMode = AudioSaveMode.STATS_ONLY,
    val sensitivity: Int = 50,
    val eventPreBufferSeconds: Int = 3,
    val eventPostBufferSeconds: Int = 7,
    val sampleRatePreference: Int = 16000,
    val enableBatteryWarning: Boolean = true,
    val enableDebugMetrics: Boolean = false
)

class SettingsDataStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            audioSaveMode = AudioSaveMode.entries.find {
                it.value == prefs[KEY_AUDIO_SAVE_MODE]
            } ?: AudioSaveMode.STATS_ONLY,
            sensitivity = prefs[KEY_SENSITIVITY] ?: 50,
            eventPreBufferSeconds = prefs[KEY_EVENT_PRE_BUFFER] ?: 3,
            eventPostBufferSeconds = prefs[KEY_EVENT_POST_BUFFER] ?: 7,
            sampleRatePreference = prefs[KEY_SAMPLE_RATE_PREFERENCE] ?: 16000,
            enableBatteryWarning = prefs[KEY_ENABLE_BATTERY_WARNING] ?: true,
            enableDebugMetrics = prefs[KEY_ENABLE_DEBUG_METRICS] ?: false
        )
    }

    suspend fun updateAudioSaveMode(mode: AudioSaveMode) {
        context.settingsDataStore.edit { it[KEY_AUDIO_SAVE_MODE] = mode.value }
    }

    suspend fun updateSensitivity(value: Int) {
        context.settingsDataStore.edit { it[KEY_SENSITIVITY] = value }
    }

    suspend fun updateEventPreBuffer(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_EVENT_PRE_BUFFER] = seconds }
    }

    suspend fun updateEventPostBuffer(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_EVENT_POST_BUFFER] = seconds }
    }

    suspend fun updateSampleRatePreference(rate: Int) {
        context.settingsDataStore.edit { it[KEY_SAMPLE_RATE_PREFERENCE] = rate }
    }

    suspend fun updateBatteryWarning(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ENABLE_BATTERY_WARNING] = enabled }
    }

    suspend fun updateDebugMetrics(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ENABLE_DEBUG_METRICS] = enabled }
    }

    companion object {
        private val KEY_AUDIO_SAVE_MODE = stringPreferencesKey("audio_save_mode")
        private val KEY_SENSITIVITY = intPreferencesKey("sensitivity")
        private val KEY_EVENT_PRE_BUFFER = intPreferencesKey("event_pre_buffer")
        private val KEY_EVENT_POST_BUFFER = intPreferencesKey("event_post_buffer")
        private val KEY_SAMPLE_RATE_PREFERENCE = intPreferencesKey("sample_rate_preference")
        private val KEY_ENABLE_BATTERY_WARNING = booleanPreferencesKey("enable_battery_warning")
        private val KEY_ENABLE_DEBUG_METRICS = booleanPreferencesKey("enable_debug_metrics")
    }
}
