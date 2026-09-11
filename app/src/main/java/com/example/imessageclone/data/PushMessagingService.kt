package com.example.imessageclone.data

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push notifications for new in-app messages (SMS fallback messages
 * don't need this — the OS already notifies for real texts).
 * Wire actual notification-building + tapping-through-to-chat here.
 */
class PushMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO: save token to users/{uid}.fcmToken so a Cloud Function can target this device
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val chatId = message.data["chatId"] ?: return
        val body = message.data["preview"] ?: message.notification?.body ?: ""
        // TODO: build a NotificationCompat notification, tapping opens ChatScreen(chatId)
    }
}
