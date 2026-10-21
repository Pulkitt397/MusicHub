package com.pulkit.musichub.platform.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicSessionListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        onServiceConnectedCallback?.invoke()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        onServiceDisconnectedCallback?.invoke()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Privacy rule (Section 10.1): Do not mirror or record notifications into storage
        // Active media sessions are discovered through MediaSessionManager.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op for privacy
    }

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        var onServiceConnectedCallback: (() -> Unit)? = null
        var onServiceDisconnectedCallback: (() -> Unit)? = null
    }
}
