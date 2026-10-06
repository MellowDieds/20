package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.EyeCareApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BreakActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val app = context.applicationContext as EyeCareApplication

        when (intent.action) {
            ACTION_DONE -> {
                // User tapped "Yaptım" directly from the notification
                EyeCareStateHolder.handleBreakCompleted()
                app.preferences.saveElapsedSeconds(0)
                app.preferences.saveAlertActive(false)
                app.preferences.saveSnoozeCount(0)

                CoroutineScope(Dispatchers.IO).launch {
                    app.repository.recordCompletedBreak(20, "20-20-20 (Hızlı)")
                }

                Toast.makeText(context, "Tebrikler! Mola tamamlandı 🌿", Toast.LENGTH_SHORT).show()
                notificationManager.cancel(EyeCareApplication.NOTIFICATION_ALERT_ID)
            }
            ACTION_SNOOZE -> {
                val app = context.applicationContext as EyeCareApplication
                val prefs = app.preferences
                val maxSnoozes = prefs.maxSnoozes.value
                val currentSnoozes = EyeCareStateHolder.currentSnoozeCount.value
                val snoozeMinutes = prefs.snoozeMinutes.value

                if (maxSnoozes in 1..currentSnoozes) {
                    Toast.makeText(
                        context,
                        "Erteleme sınırına ulaşıldı ($maxSnoozes/$maxSnoozes). Lütfen gözlerinizi dinlendirin!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val snoozeSeconds = snoozeMinutes * 60
                    EyeCareStateHolder.handleSnooze(snoozeSeconds)

                    CoroutineScope(Dispatchers.IO).launch {
                        app.repository.recordSnooze(snoozeMinutes)
                    }

                    Toast.makeText(
                        context,
                        "Mola $snoozeMinutes dakika ertelendi (${currentSnoozes + 1}/$maxSnoozes)",
                        Toast.LENGTH_SHORT
                    ).show()

                    notificationManager.cancel(EyeCareApplication.NOTIFICATION_ALERT_ID)
                }
            }
            ACTION_DISMISS -> {
                EyeCareStateHolder.setBreakAlertActive(false)
                notificationManager.cancel(EyeCareApplication.NOTIFICATION_ALERT_ID)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.aistudio.eyecare.ACTION_DONE"
        const val ACTION_SNOOZE = "com.aistudio.eyecare.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.aistudio.eyecare.ACTION_DISMISS"
    }
}
