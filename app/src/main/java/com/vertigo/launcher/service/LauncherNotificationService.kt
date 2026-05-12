package com.vertigo.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.concurrent.ConcurrentHashMap

class LauncherNotificationService : NotificationListenerService() {

    companion object {
        const val ACTION_NOTIFICATION_CHANGED = "com.vertigo.launcher.NOTIFICATION_CHANGED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_COUNT = "count"
        
        private val notificationCounts = ConcurrentHashMap<String, Int>()
        
        fun getNotificationCount(packageName: String): Int {
            return notificationCounts[packageName] ?: 0
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotificationCount()
    }

    private fun updateNotificationCount() {
        val activeNotifications = activeNotifications ?: return
        
        // Clear previous counts
        notificationCounts.clear()
        
        // Count notifications per package
        for (notification in activeNotifications) {
            val packageName = notification.packageName
            notificationCounts[packageName] = notificationCounts.getOrDefault(packageName, 0) + 1
        }
        
        // Broadcast update to launcher
        val intent = Intent(ACTION_NOTIFICATION_CHANGED)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}
