package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.EyeCareApplication
import com.example.data.db.BreakRecord
import com.example.data.update.UpdateCheckResult
import com.example.data.update.UpdateChecker
import com.example.service.EyeCareService
import com.example.service.EyeCareStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EyeCareUiState(
    val currentElapsedSeconds: Int = 0,
    val targetDurationSeconds: Int = 1200,
    val isScreenOn: Boolean = true,
    val isServiceRunning: Boolean = false,
    val isBreakAlertActive: Boolean = false,
    val currentSnoozeCount: Int = 0,
    val maxSnoozes: Int = 3,
    val intervalMinutes: Int = 20,
    val snoozeMinutes: Int = 5,
    val isTestMode: Boolean = false,
    val todayCompletedCount: Int = 0,
    val todayRecords: List<BreakRecord> = emptyList(),
    val totalScreenTimeTodaySeconds: Int = 0,
    val isSoundEnabled: Boolean = false,
    val resetOnScreenOff: Boolean = true,
    val githubRepo: String = "mellowdieds/20-20-20-Goz-Sagligi",
    val updateCheckResult: UpdateCheckResult = UpdateCheckResult.Idle,
    val isCheckingUpdate: Boolean = false
) {
    val remainingSeconds: Int
        get() = maxOf(0, targetDurationSeconds - currentElapsedSeconds)

    val progress: Float
        get() = if (targetDurationSeconds > 0) {
            (currentElapsedSeconds.toFloat() / targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemainingTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }

    val eyeStrainScore: Int
        get() {
            // Calculated score based on breaks taken relative to screen hours
            val screenHours = totalScreenTimeTodaySeconds / 3600f
            val idealBreaks = screenHours * 3 // 3 breaks per hour
            if (idealBreaks <= 0.1f) return 100
            val ratio = (todayCompletedCount / idealBreaks).coerceIn(0f, 1f)
            return (70 + (ratio * 30)).toInt()
        }
}

class EyeCareViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EyeCareApplication
    private val repository = app.repository
    private val preferences = app.preferences

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    private val _isCheckingUpdate = MutableStateFlow(false)

    private val baseUiState = combine(
        EyeCareStateHolder.currentElapsedSeconds,
        EyeCareStateHolder.targetDurationSeconds,
        EyeCareStateHolder.isScreenOn,
        EyeCareStateHolder.isServiceRunning,
        EyeCareStateHolder.isBreakAlertActive,
        EyeCareStateHolder.currentSnoozeCount,
        preferences.maxSnoozes,
        preferences.intervalMinutes,
        preferences.snoozeMinutes,
        preferences.isTestMode,
        repository.getTodayCompletedCount(),
        repository.getTodayRecords(),
        EyeCareStateHolder.totalScreenTimeTodaySeconds,
        preferences.isSoundEnabled,
        preferences.resetOnScreenOff
    ) { params ->
        val elapsed = params[0] as Int
        val target = params[1] as Int
        val screenOn = params[2] as Boolean
        val running = params[3] as Boolean
        val alertActive = params[4] as Boolean
        val snoozeCount = params[5] as Int
        val maxSnoozes = params[6] as Int
        val intervalMins = params[7] as Int
        val snoozeMins = params[8] as Int
        val testMode = params[9] as Boolean
        val completedCount = params[10] as Int
        @Suppress("UNCHECKED_CAST")
        val records = params[11] as List<BreakRecord>
        val screenTime = params[12] as Int
        val sound = params[13] as Boolean
        val resetScreenOff = params[14] as Boolean

        EyeCareUiState(
            currentElapsedSeconds = elapsed,
            targetDurationSeconds = target,
            isScreenOn = screenOn,
            isServiceRunning = running,
            isBreakAlertActive = alertActive,
            currentSnoozeCount = snoozeCount,
            maxSnoozes = maxSnoozes,
            intervalMinutes = intervalMins,
            snoozeMinutes = snoozeMins,
            isTestMode = testMode,
            todayCompletedCount = completedCount,
            todayRecords = records,
            totalScreenTimeTodaySeconds = screenTime,
            isSoundEnabled = sound,
            resetOnScreenOff = resetScreenOff
        )
    }

    val uiState: StateFlow<EyeCareUiState> = combine(
        baseUiState,
        _updateCheckResult,
        _isCheckingUpdate,
        preferences.githubRepo
    ) { base, updateResult, isChecking, repo ->
        base.copy(
            updateCheckResult = updateResult,
            isCheckingUpdate = isChecking,
            githubRepo = repo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EyeCareUiState()
    )

    val navigateToExercise = EyeCareStateHolder.navigateToExercise

    fun startService() {
        preferences.setServiceEnabled(true)
        val context = getApplication<Application>()
        val intent = Intent(context, EyeCareService::class.java).apply {
            action = EyeCareService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService() {
        preferences.setServiceEnabled(false)
        val context = getApplication<Application>()
        val intent = Intent(context, EyeCareService::class.java).apply {
            action = EyeCareService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun triggerBreakNow() {
        EyeCareStateHolder.setBreakAlertActive(true)
    }

    fun snoozeBreak() {
        val maxSnoozes = preferences.maxSnoozes.value
        val currentSnoozes = EyeCareStateHolder.currentSnoozeCount.value
        val snoozeMinutes = preferences.snoozeMinutes.value

        if (maxSnoozes == 0 || currentSnoozes < maxSnoozes) {
            val snoozeSeconds = snoozeMinutes * 60
            EyeCareStateHolder.handleSnooze(snoozeSeconds)
            viewModelScope.launch {
                repository.recordSnooze(snoozeMinutes)
            }
        }
    }

    fun dismissAlert() {
        EyeCareStateHolder.setBreakAlertActive(false)
    }

    fun completeBreak(durationSeconds: Int = 20, routineName: String = "20-20-20 Kuralı") {
        viewModelScope.launch {
            repository.recordCompletedBreak(durationSeconds, routineName)
            EyeCareStateHolder.handleBreakCompleted()
        }
    }

    fun resetTimer() {
        EyeCareStateHolder.resetTimer()
    }

    fun toggleTestMode(enabled: Boolean) {
        preferences.setTestMode(enabled)
        val targetSeconds = if (enabled) 20 else preferences.intervalMinutes.value * 60
        EyeCareStateHolder.setTargetDurationSeconds(targetSeconds)
        EyeCareStateHolder.resetTimer()
    }

    fun setIntervalMinutes(minutes: Int) {
        preferences.setIntervalMinutes(minutes)
        if (!preferences.isTestMode.value) {
            EyeCareStateHolder.setTargetDurationSeconds(minutes * 60)
        }
    }

    fun setSnoozeMinutes(minutes: Int) {
        preferences.setSnoozeMinutes(minutes)
    }

    fun setMaxSnoozes(count: Int) {
        preferences.setMaxSnoozes(count)
    }

    fun setSoundEnabled(enabled: Boolean) {
        preferences.setSoundEnabled(enabled)
    }

    fun setResetOnScreenOff(enabled: Boolean) {
        preferences.setResetOnScreenOff(enabled)
    }

    fun consumeExerciseNavigation() {
        EyeCareStateHolder.consumeExerciseNavigation()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun checkForUpdates() {
        if (_isCheckingUpdate.value) return
        _isCheckingUpdate.value = true
        _updateCheckResult.value = UpdateCheckResult.Checking
        viewModelScope.launch {
            val repo = preferences.githubRepo.value
            val result = UpdateChecker.checkForUpdates(repo)
            _updateCheckResult.value = result
            _isCheckingUpdate.value = false
        }
    }

    fun dismissUpdateResult() {
        _updateCheckResult.value = UpdateCheckResult.Idle
    }

    fun setGithubRepo(repo: String) {
        preferences.setGithubRepo(repo)
    }
}
