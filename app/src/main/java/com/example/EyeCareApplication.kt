package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.BreakRepository

class EyeCareApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { BreakRepository(database.breakDao()) }
    val preferences by lazy { UserPreferences.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Silent Ongoing Service Channel (low importance, no popup, no sound)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Göz Koruması Durumu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "20-20-20 kuralı arka plan ekran süresi takip bildirimi"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // 2. Alert Reminder Channel (high importance for heads-up, but quiet/peaceful)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "20-20-20 Mola Hatırlatıcıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "20 dakikalık mola zamanı geldiğinde nazikçe uyarır"
                setShowBadge(true)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "eye_care_service_channel"
        const val CHANNEL_ALERT_ID = "eye_care_alert_channel"
        const val NOTIFICATION_SERVICE_ID = 20201
        const val NOTIFICATION_ALERT_ID = 20202

        lateinit var instance: EyeCareApplication
            private set
    }
}
