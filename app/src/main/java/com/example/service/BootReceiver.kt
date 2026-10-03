package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.EyeCareApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? EyeCareApplication ?: return
            val autoStart = app.preferences.autoStartOnBoot.value
            val isEnabled = app.preferences.isServiceEnabled.value

            if (autoStart && isEnabled) {
                val serviceIntent = Intent(context, EyeCareService::class.java).apply {
                    action = EyeCareService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
