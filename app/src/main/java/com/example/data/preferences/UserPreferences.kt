package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("eye_care_prefs", Context.MODE_PRIVATE)

    private val _intervalMinutes = MutableStateFlow(prefs.getInt(KEY_INTERVAL_MINUTES, 20))
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()

    private val _snoozeMinutes = MutableStateFlow(prefs.getInt(KEY_SNOOZE_MINUTES, 5))
    val snoozeMinutes: StateFlow<Int> = _snoozeMinutes.asStateFlow()

    private val _maxSnoozes = MutableStateFlow(prefs.getInt(KEY_MAX_SNOOZES, 3))
    val maxSnoozes: StateFlow<Int> = _maxSnoozes.asStateFlow()

    private val _isServiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ENABLED, true))
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    private val _isTestMode = MutableStateFlow(prefs.getBoolean(KEY_TEST_MODE, false))
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, false))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _autoStartOnBoot = MutableStateFlow(prefs.getBoolean(KEY_AUTO_START, true))
    val autoStartOnBoot: StateFlow<Boolean> = _autoStartOnBoot.asStateFlow()

    fun setIntervalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_INTERVAL_MINUTES, minutes).apply()
        _intervalMinutes.value = minutes
    }

    fun setSnoozeMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SNOOZE_MINUTES, minutes).apply()
        _snoozeMinutes.value = minutes
    }

    fun setMaxSnoozes(count: Int) {
        prefs.edit().putInt(KEY_MAX_SNOOZES, count).apply()
        _maxSnoozes.value = count
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _isServiceEnabled.value = enabled
    }

    fun setTestMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TEST_MODE, enabled).apply()
        _isTestMode.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _isSoundEnabled.value = enabled
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        _autoStartOnBoot.value = enabled
    }

    fun getSavedElapsedSeconds(): Int = prefs.getInt(KEY_SAVED_ELAPSED_SECONDS, 0)

    fun saveElapsedSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_SAVED_ELAPSED_SECONDS, seconds).apply()
    }

    fun getSavedSnoozeCount(): Int = prefs.getInt(KEY_SAVED_SNOOZE_COUNT, 0)

    fun saveSnoozeCount(count: Int) {
        prefs.edit().putInt(KEY_SAVED_SNOOZE_COUNT, count).apply()
    }

    fun isSavedAlertActive(): Boolean = prefs.getBoolean(KEY_SAVED_ALERT_ACTIVE, false)

    fun saveAlertActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SAVED_ALERT_ACTIVE, active).apply()
    }

    companion object {
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_MAX_SNOOZES = "max_snoozes"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_TEST_MODE = "test_mode"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_SAVED_ELAPSED_SECONDS = "saved_elapsed_seconds"
        private const val KEY_SAVED_SNOOZE_COUNT = "saved_snooze_count"
        private const val KEY_SAVED_ALERT_ACTIVE = "saved_alert_active"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
