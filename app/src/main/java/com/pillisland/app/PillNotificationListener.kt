package com.pillisland.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PillNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (sbn.packageName == packageName) return

        PillOverlayService.instance?.updateNotification(title, text)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
    }
}