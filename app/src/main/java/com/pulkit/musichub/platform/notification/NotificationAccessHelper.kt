package com.pulkit.musichub.platform.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationAccessHelper {

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun getNotificationAccessSettingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        // Check if intent resolves, otherwise fallback
        if (intent.resolveActivity(context.packageManager) == null) {
            return Intent(Settings.ACTION_SETTINGS)
        }
        return intent
    }

    fun getListenerComponentName(context: Context): ComponentName {
        return ComponentName(context, MusicSessionListenerService::class.java)
    }
}
