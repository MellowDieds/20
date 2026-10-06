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
import com.example.R
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
            .setSmallIcon(R.drawable.ic_notification_eye)
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

                if (isScreenInteractive) {
                    if (wakeLock?.isHeld != true) {
                        updateWakeLock(true)
                    }

                    // Only count progress if an alert is not already awaiting user action
                    if (!EyeCareStateHolder.isBreakAlertActive.value) {
                        if (deltaSeconds > 0) {
                            val current = EyeCareStateHolder.currentElapsedSeconds.value + deltaSeconds
                            EyeCareStateHolder.updateElapsedSeconds(current)
                            EyeCareStateHolder.incrementScreenTimeToday()
                            lastTickRealtime = nowRealtime

                            val isTestMode = app.preferences.isTestMode.value
                            val targetSeconds = if (isTestMode) 20 else app.preferences.intervalMinutes.value * 60
                            EyeCareStateHolder.setTargetDurationSeconds(targetSeconds)

                            if (current % 5 == 0) {
                                app.preferences.saveElapsedSeconds(current)
                            }

                            if (current >= targetSeconds) {
                                showBreakAlertNotification()
                            }
                        }
                    } else {
                        // Alert is active: keep reference time current so we don't jump ahead
                        lastTickRealtime = nowRealtime
                    }
                } else {
                    // Screen is genuinely OFF
                    lastTickRealtime = nowRealtime
                    updateWakeLock(false)
                    if (app.preferences.resetOnScreenOff.value && !EyeCareStateHolder.isBreakAlertActive.value) {
                        if (EyeCareStateHolder.currentElapsedSeconds.value > 0) {
                            EyeCareStateHolder.resetTimer()
                            app.preferences.saveElapsedSeconds(0)
                            app.preferences.saveSnoozeCount(0)
                            updateOngoingNotification()
                        }
                    }
                }

                secondCounter++
                if (secondCounter % 30 == 0) {
                    updateOngoingNotification()
                }
            }
        }
    }

    private fun showBreakAlertNotification() {
        EyeCareStateHolder.setBreakAlertActive(true)

        val app = application as EyeCareApplication
        app.preferences.saveAlertActive(true)

        // 1. "Göster" Intent -> Opens Animated Exercise Screen
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

        // 2. "Yaptım" Intent -> BroadcastReceiver to record break and clear immediately
        val doneIntent = Intent(this, BreakActionReceiver::class.java).apply {
            action = BreakActionReceiver.ACTION_DONE
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            this,
            103,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Delete Intent -> If dismissed, reset alert active
        val dismissIntent = Intent(this, BreakActionReceiver::class.java).apply {
            action = BreakActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            104,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, EyeCareApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_notification_eye)
            .setContentTitle("🌿 20-20-20 Mola Zamanı")
            .setContentText("Telefonu indirip 20 saniye 6 metre uzağa bakın.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "20 dakika ekran süresi doldu. Telefonu hafifçe indirip en az 6 metre (20 feet) uzağa bakın."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Stays permanently visible until user taps "Göster" or "Yaptım"
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(exercisePendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "Göster", exercisePendingIntent)
            .addAction(0, "Yaptım", donePendingIntent)

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
