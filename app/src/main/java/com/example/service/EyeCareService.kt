package com.example.service

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
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tickerJob: Job? = null

    private var screenReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        EyeCareStateHolder.setServiceRunning(true)
        registerScreenReceiver()
        checkInitialScreenState()
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
            }
            else -> {
                startForegroundServiceNotification()
                startTracking()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val app = application as EyeCareApplication
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

        return NotificationCompat.Builder(this, EyeCareApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("20-20-20 Göz Sağlığı Takibi")
            .setContentText("Ekran açıkken süre sayılıyor • Sonraki mola: ~$remainingMinutes dk")
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
                        Intent.ACTION_SCREEN_ON -> {
                            EyeCareStateHolder.setScreenOn(true)
                        }
                        Intent.ACTION_SCREEN_OFF -> {
                            EyeCareStateHolder.setScreenOn(false)
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            EyeCareStateHolder.setScreenOn(true)
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

    private fun checkInitialScreenState() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        EyeCareStateHolder.setScreenOn(powerManager.isInteractive)
    }

    private fun startTracking() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val app = application as EyeCareApplication
            var secondTick = 0

            while (isActive) {
                delay(1000L)

                // Only count while screen is interactive/ON
                if (EyeCareStateHolder.isScreenOn.value && !EyeCareStateHolder.isBreakAlertActive.value) {
                    val current = EyeCareStateHolder.currentElapsedSeconds.value + 1
                    EyeCareStateHolder.updateElapsedSeconds(current)
                    EyeCareStateHolder.incrementScreenTimeToday()

                    val isTestMode = app.preferences.isTestMode.value
                    val targetSeconds = if (isTestMode) 20 else app.preferences.intervalMinutes.value * 60
                    EyeCareStateHolder.setTargetDurationSeconds(targetSeconds)

                    if (current >= targetSeconds) {
                        showBreakAlertNotification()
                    }
                }

                secondTick++
                if (secondTick % 30 == 0) {
                    updateOngoingNotification()
                }
            }
        }
    }

    private fun showBreakAlertNotification() {
        EyeCareStateHolder.setBreakAlertActive(true)

        val app = application as EyeCareApplication
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
        tickerJob?.cancel()
        EyeCareStateHolder.setServiceRunning(false)
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
