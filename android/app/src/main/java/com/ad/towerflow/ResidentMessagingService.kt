package com.ad.towerflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ResidentMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        getSharedPreferences("push_device", MODE_PRIVATE).edit().putString("token", token).apply()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("towerflow_residents", "TowerFlow · رسائل السكان", NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val open = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP; putExtra("open_notifications", true); putExtra("notification_org", message.data["org_id"]) }
        val id = message.data["notification_id"]?.hashCode() ?: message.messageId?.hashCode() ?: 1
        val pending = PendingIntent.getActivity(this, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id, Notification.Builder(this, "towerflow_residents")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message.notification?.title ?: "TowerFlow")
            .setContentText(message.notification?.body ?: message.data["body"] ?: "لديك رسالة جديدة")
            .setStyle(Notification.BigTextStyle().bigText(message.notification?.body ?: message.data["body"]))
            .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(pending).setAutoCancel(true).build())
    }
}
