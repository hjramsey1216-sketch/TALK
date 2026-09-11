package com.example.imessageclone.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUid: String? get() = auth.currentUser?.uid

    suspend fun upsertCurrentUser(user: AppUser) {
        val uid = currentUid ?: return
        firestore.collection("users").document(uid).set(user.copy(uid = uid)).await()
    }

    /** Live stream of messages in a chat, newest last. */
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val reg = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val msgs = snap?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { reg.remove() }
    }

    fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val uid = currentUid
        if (uid == null) { close(); return@callbackFlow }
        val reg = firestore.collection("users").document(uid)
            .collection("chats")
            .orderBy("lastTimestamp")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val chats = snap?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: emptyList()
                trySend(chats.reversed())
            }
        awaitClose { reg.remove() }
    }

    /** Sends an in-app message (only valid for chat.isAppChat == true). */
    suspend fun sendAppMessage(chat: Chat, text: String) {
        val uid = currentUid ?: return
        val message = Message(
            chatId = chat.chatId,
            senderUid = uid,
            text = text,
            deliveryChannel = DeliveryChannel.APP
        )
        val messagesRef = firestore.collection("chats").document(chat.chatId).collection("messages")
        messagesRef.add(message).await()

        // BUG FIX: this used to write only {chatId, lastMessage, lastTimestamp}.
        // Chat also has contactName/contactPhone/isAppChat/otherUserUid, and
        // those were never saved anywhere — so ChatListScreen would read back
        // a Chat object with those fields at their defaults ("" / false),
        // showing a blank contact name and mislabeling every chat as SMS.
        // merge() means this is safe to call on every message without
        // clobbering anything.
        val summary = chat.copy(lastMessage = text, lastTimestamp = message.timestamp)
        firestore.collection("users").document(uid)
            .collection("chats").document(chat.chatId).set(summary, com.google.firebase.firestore.SetOptions.merge()).await()
        chat.otherUserUid?.let { other ->
            // BUG FIX: this used to mirror `summary` as-is to the other person's
            // chat list — but `summary.contactName` is the sender's own local
            // label for the recipient (e.g. "Mom"), which is meaningless from
            // the recipient's side. From their side, "otherUserUid" is the
            // sender, and the name shown should describe the sender instead.
            val mirrorForOther = summary.copy(
                contactName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "New message",
                contactPhone = null,
                otherUserUid = uid
            )
            firestore.collection("users").document(other)
                .collection("chats").document(chat.chatId).set(mirrorForOther, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    /** Records an outgoing SMS-fallback message locally so it shows up in the same thread UI. */
    suspend fun logSmsMessage(chat: Chat, text: String) {
        val uid = currentUid ?: return
        val message = Message(
            chatId = chat.chatId,
            senderUid = "local-sms",
            text = text,
            deliveryChannel = DeliveryChannel.SMS
        )
        firestore.collection("chats").document(chat.chatId).collection("messages").add(message).await()

        // Same fix as above: persist full chat metadata, not just a partial summary.
        val summary = chat.copy(lastMessage = text, lastTimestamp = message.timestamp)
        firestore.collection("users").document(uid)
            .collection("chats").document(chat.chatId).set(summary, com.google.firebase.firestore.SetOptions.merge()).await()
    }
}
