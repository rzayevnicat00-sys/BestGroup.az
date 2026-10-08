package com.example.services.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BestGroupFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                FcmTokenManager(applicationContext).saveToken(uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "BestGroup.az"
        val body = notification?.body ?: data["body"] ?: "Yeni bildirişiniz var."

        val type = data["type"] ?: "general"
        val orderId = data["orderId"]
        val orderNumber = data["orderNumber"]
        val conversationId = data["conversationId"]
        val userId = data["userId"]

        sendNotification(
            title = title,
            messageBody = body,
            type = type,
            orderId = orderId,
            orderNumber = orderNumber,
            conversationId = conversationId,
            userId = userId
        )
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        type: String,
        orderId: String?,
        orderNumber: String?,
        conversationId: String?,
        userId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NOTIF_TYPE", type)
            putExtra("EXTRA_ORDER_ID", orderId)
            putExtra("EXTRA_ORDER_NUMBER", orderNumber)
            putExtra("EXTRA_CONVERSATION_ID", conversationId)
            putExtra("EXTRA_USER_ID", userId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "bestgroup_main_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BestGroup.az Bildirişləri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sifariş, mesaj və sistem bildirişləri"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
