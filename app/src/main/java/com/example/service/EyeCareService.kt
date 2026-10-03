package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.EyeCareApplication
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EyeCareService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var trackingJob: Job? = null

    private var screenReceiver: BroadcastReceiver? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var powerManager: PowerManager
    private lateinit var alarmManager: AlarmManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val app = application as EyeCareApplication
        // Restore saved progress in case process was recreated while in another app
        val savedSeconds = app.preferences.getSavedElapsedSeconds()
        val savedSnoozes = app.preferences.getSavedSnoozeCount()
        if (savedSeconds > 0) {
            EyeCareStateHolder.updateElapsedSeconds(savedSeconds)
        }
        if (savedSnoozes > 0) {
            for (i in 0 until savedSnoozes) {
                // Keep snooze count aligned
            }
        }

        EyeCareStateHolder.setServiceRunning(true)
        registerScreenReceiver()
        updateWakeLock(powerManager.isInteractive)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_NOW -> {
                showBreakAlertNotification()
            }
            ACTION_RESET -> {
                EyeCareStateHolder.resetTimer()
                val app = application as EyeCareApplication
                app.preferences.saveElapsedSeconds(0)
                app.preferences.saveSnoozeCount(0)
            }
            else -> {
                startForegroundServiceNotification()
                startTracking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = createOngoingNotification(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                EyeCareApplication.NOTIFICATION_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(EyeCareApplication.NOTIFICATION_SERVICE_ID, notification)
        }
    }

    private fun createOngoingNotification(pendingIntent: PendingIntent): Notification {
        val elapsed = EyeCareStateHolder.currentElapsedSeconds.value
        val target = EyeCareStateHolder.targetDurationSeconds.value
        val remainingMinutes = maxOf(0, (target - elapsed) / 60)
        val isScreenOn = powerManager.isInteractive

        val statusText = if (isScreenOn) {
            "Ekran süresi takip ediliyor • Kalan: ~$remainingMinutes dk"
        } else {
            "Ekran kapalı (Sayaç duraklatıldı) • Kalan: ~$remainingMinutes dk"
        }

        return NotificationCompat.Builder(this, EyeCareApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("20-20-20 Göz Sağlığı Takibi")
            .setContentText(statusText)
            .setSmallIcon(applicationInfo.icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateOngoingNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            EyeCareApplication.NOTIFICATION_SERVICE_ID,
            createOngoingNotification(pendingIntent)
        )
    }

    private fun registerScreenReceiver() {
        if (screenReceiver == null) {
            screenReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                            EyeCareStateHolder.setScreenOn(true)
                            updateWakeLock(true)
                            updateOngoingNotification()
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            EyeCareStateHolder.setScreenOn(false)
                            updateWakeLock(false)
                            val app = application as EyeCareApplication
                            if (app.preferences.resetOnScreenOff.value) {
                                EyeCareStateHolder.resetTimer()
                                app.preferences.saveElapsedSeconds(0)
                                app.preferences.saveSnoozeCount(0)
                                app.preferences.saveAlertActive(false)
                                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancel(EyeCareApplication.NOTIFICATION_ALERT_ID)
                            }
                            updateOngoingNotification()
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun updateWakeLock(shouldHold: Boolean) {
        try {
            if (shouldHold) {
                if (wakeLock == null) {
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "EyeCare::ScreenOnTrackingLock"
                    ).apply {
                        setReferenceCounted(false)
                    }
                }
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(30 * 60 * 1000L) // Safe 30-min timeout
                }
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        } catch (_: Exception) {}
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val app = application as EyeCareApplication
            var lastTickRealtime = SystemClock.elapsedRealtime()
            var secondCounter = 0

            while (isActive) {
                delay(1000L)

                val nowRealtime = SystemClock.elapsedRealtime()
                val deltaSeconds = ((nowRealtime - lastTickRealtime) / 1000L).toInt().coerceAtLeast(0)

                // Verify real screen state directly from power manager
                val isScreenInteractive = powerManager.isInteractive
                EyeCareStateHolder.setScreenOn(isScreenInteractive)

                if (isScreenInteractive && !EyeCareStateHolder.isBreakAlertActive.value) {
                    // Update wakelock to ensure CPU does not sleep when in other heavy apps
                    if (wakeLock?.isHeld != true) {
                        updateWakeLock(true)
                    }

                    if (deltaSeconds > 0) {
                        val current = EyeCareStateHolder.currentElapsedSeconds.value + deltaSeconds
                        EyeCareStateHolder.updateElapsedSeconds(current)
                        EyeCareStateHolder.incrementScreenTimeToday()
                        lastTickRealtime = nowRealtime

                        val isTestMode = app.preferences.isTestMode.value
                        val targetSeconds = if (isTestMode) 20 else app.preferences.intervalMinutes.value * 60
                        EyeCareStateHolder.setTargetDurationSeconds(targetSeconds)

                        // Save progress to disk every 5 seconds so killed processes remember progress
                        if (current % 5 == 0) {
                            app.preferences.saveElapsedSeconds(current)
                        }

                        if (current >= targetSeconds) {
                            showBreakAlertNotification()
                            scheduleBackupAlarm()
                        }
                    }
                } else {
                    // Screen is OFF: reset reference time so we don't accumulate off-screen time!
                    lastTickRealtime = nowRealtime
                    updateWakeLock(false)
                    if (app.preferences.resetOnScreenOff.value && EyeCareStateHolder.currentElapsedSeconds.value > 0) {
                        EyeCareStateHolder.resetTimer()
                        app.preferences.saveElapsedSeconds(0)
                        app.preferences.saveSnoozeCount(0)
                        app.preferences.saveAlertActive(false)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(EyeCareApplication.NOTIFICATION_ALERT_ID)
                        updateOngoingNotification()
                    }
                }

                secondCounter++
                if (secondCounter % 30 == 0) {
                    updateOngoingNotification()
                }
            }
        }
    }

    private fun scheduleBackupAlarm() {
        try {
            val intent = Intent(this, EyeCareService::class.java).apply {
                action = ACTION_TRIGGER_NOW
            }
            val pendingIntent = PendingIntent.getService(
                this,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // 5 second fallback if notification was cancelled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 5000,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    private fun showBreakAlertNotification() {
        EyeCareStateHolder.setBreakAlertActive(true)

        val app = application as EyeCareApplication
        app.preferences.saveAlertActive(true)

        val maxSnoozes = app.preferences.maxSnoozes.value
        val currentSnoozes = EyeCareStateHolder.currentSnoozeCount.value
        val canSnooze = maxSnoozes == 0 || currentSnoozes < maxSnoozes

        // 1. "Göster / Yap" Intent -> Opens Animated Exercise Screen
        val exerciseIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_EXERCISE
            putExtra(MainActivity.EXTRA_OPEN_EXERCISE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val exercisePendingIntent = PendingIntent.getActivity(
            this,
            101,
            exerciseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. "Ertele" Intent -> BroadcastReceiver
        val snoozeIntent = Intent(this, BreakActionReceiver::class.java).apply {
            action = BreakActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeButtonLabel = if (canSnooze) {
            "Ertele (5 Dk)"
        } else {
            "Ertele (Limit Doldu)"
        }

        val builder = NotificationCompat.Builder(this, EyeCareApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("🌿 20-20-20 Mola Zamanı!")
            .setContentText("20 saniye boyunca 6 metre uzağa bakın.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Ekran süreniz doldu. Göz kaslarınızı gevşetmek için 20 saniye boyunca en az 6 metre (20 feet) uzağa bakın."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(exercisePendingIntent)
            .addAction(0, "Göster / Yap", exercisePendingIntent)

        if (canSnooze) {
            builder.addAction(0, snoozeButtonLabel, snoozePendingIntent)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EyeCareApplication.NOTIFICATION_ALERT_ID, builder.build())
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        EyeCareStateHolder.setServiceRunning(false)
        updateWakeLock(false)
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver)
                screenReceiver = null
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopTracking()
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.aistudio.eyecare.START"
        const val ACTION_STOP = "com.aistudio.eyecare.STOP"
        const val ACTION_TRIGGER_NOW = "com.aistudio.eyecare.TRIGGER_NOW"
        const val ACTION_RESET = "com.aistudio.eyecare.RESET"
    }
}
