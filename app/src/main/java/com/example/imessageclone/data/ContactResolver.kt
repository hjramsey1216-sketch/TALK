package com.example.imessageclone.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Decides how a conversation with a given phone number should be routed:
 *  - If someone in `users` has registered that phone number -> in-app chat.
 *  - Otherwise -> SMS fallback (handled by SmsFallbackSender).
 *
 * Phone numbers must be normalized to E.164 (+countrycode...) before lookup,
 * both when a user registers theirs and when we resolve a contact's number,
 * or matches will silently fail.
 */
class ContactResolver(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun resolve(phoneNumberE164: String, contactName: String): Chat {
        val match = firestore.collection("users")
            .whereEqualTo("phoneNumber", phoneNumberE164)
            .limit(1)
            .get()
            .await()

        val doc = match.documents.firstOrNull()
        return if (doc != null) {
            val myUid = auth.currentUser?.uid.orEmpty()
            Chat(
                // BUG FIX: this used to be chatIdFor(doc.id) alone, which means
                // the two people in a conversation each computed a DIFFERENT id
                // (one keyed on the other's uid) -> two separate one-sided
                // threads instead of one shared conversation. A chat id must be
                // symmetric: the same value no matter which side computes it.
                chatId = chatIdForUsers(myUid, doc.id),
                contactName = contactName,
                contactPhone = phoneNumberE164,
                otherUserUid = doc.id,
                isAppChat = true
            )
        } else {
            Chat(
                // No Firestore counterpart exists, so there's no symmetry
                // concern here — this thread only ever lives under the
                // current user's own account.
                chatId = "chat_sms_${phoneNumberE164.hashCode()}",
                contactName = contactName,
                contactPhone = phoneNumberE164,
                otherUserUid = null,
                isAppChat = false
            )
        }
    }

    private fun chatIdForUsers(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "chat_${sorted[0]}_${sorted[1]}"
    }
}
