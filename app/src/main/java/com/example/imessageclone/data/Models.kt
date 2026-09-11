package com.example.imessageclone.data

/**
 * A registered app user. Stored in Firestore at users/{uid}.
 * Phone number is stored normalized (E.164, e.g. +15551234567) purely as a
 * lookup key so other users can be matched from their contacts book —
 * it is never shown as "your identity", the account is the uid.
 */
data class AppUser(
    val uid: String = "",
    val displayName: String = "",
    val phoneNumber: String? = null, // optional: only used for discovery, not login
    val email: String? = null,
    val avatarUrl: String? = null,
    val bubbleColorHex: String = "#0B84FE", // customization: sent-bubble color
    val theme: String = "system" // "light" | "dark" | "system"
)

/**
 * Represents either kind of conversation. `isAppChat` decides which pipe
 * outgoing messages travel through.
 */
data class Chat(
    val chatId: String = "",
    val contactName: String = "",
    val contactPhone: String? = null,
    val otherUserUid: String? = null, // set only when isAppChat == true
    val isAppChat: Boolean = false,
    val lastMessage: String = "",
    val lastTimestamp: Long = 0L
)

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderUid: String = "", // "local-sms" for outgoing SMS-fallback messages
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryChannel: DeliveryChannel = DeliveryChannel.APP
)

enum class DeliveryChannel { APP, SMS }
