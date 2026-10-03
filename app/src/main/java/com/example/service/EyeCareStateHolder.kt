package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EyeCareStateHolder {
    // Timer & Status
    private val _currentElapsedSeconds = MutableStateFlow(0)
    val currentElapsedSeconds: StateFlow<Int> = _currentElapsedSeconds.asStateFlow()

    private val _targetDurationSeconds = MutableStateFlow(1200) // 20 minutes default
    val targetDurationSeconds: StateFlow<Int> = _targetDurationSeconds.asStateFlow()

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isBreakAlertActive = MutableStateFlow(false)
    val isBreakAlertActive: StateFlow<Boolean> = _isBreakAlertActive.asStateFlow()

    private val _currentSnoozeCount = MutableStateFlow(0)
    val currentSnoozeCount: StateFlow<Int> = _currentSnoozeCount.asStateFlow()

    private val _totalScreenTimeTodaySeconds = MutableStateFlow(0)
    val totalScreenTimeTodaySeconds: StateFlow<Int> = _totalScreenTimeTodaySeconds.asStateFlow()

    // Nav request to Exercise screen (e.g. from notification "Göster / Yap")
    private val _navigateToExercise = MutableStateFlow(false)
    val navigateToExercise: StateFlow<Boolean> = _navigateToExercise.asStateFlow()

    fun updateElapsedSeconds(seconds: Int) {
        _currentElapsedSeconds.value = seconds
    }

    fun setTargetDurationSeconds(seconds: Int) {
        _targetDurationSeconds.value = seconds
    }

    fun setScreenOn(screenOn: Boolean) {
        _isScreenOn.value = screenOn
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun setBreakAlertActive(active: Boolean) {
        _isBreakAlertActive.value = active
    }

    fun incrementScreenTimeToday() {
        _totalScreenTimeTodaySeconds.value += 1
    }

    fun triggerExerciseNavigation() {
        _isBreakAlertActive.value = false
        _navigateToExercise.value = true
    }

    fun consumeExerciseNavigation() {
        _navigateToExercise.value = false
    }

    fun handleSnooze(snoozeSeconds: Int) {
        _isBreakAlertActive.value = false
        _currentSnoozeCount.value += 1
        // Reset countdown so that break triggers in snoozeSeconds
        val target = _targetDurationSeconds.value
        _currentElapsedSeconds.value = maxOf(0, target - snoozeSeconds)
    }

    fun handleBreakCompleted() {
        _currentElapsedSeconds.value = 0
        _currentSnoozeCount.value = 0
        _isBreakAlertActive.value = false
    }

    fun resetTimer() {
        _currentElapsedSeconds.value = 0
        _currentSnoozeCount.value = 0
        _isBreakAlertActive.value = false
    }
}
