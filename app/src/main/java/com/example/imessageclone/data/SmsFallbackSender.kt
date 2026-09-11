package com.example.imessageclone.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Sends a plain SMS through the phone's own radio/SIM — no server, no
 * third-party API, no cost beyond the user's own SMS plan. This only works
 * on the physical device (won't work on wifi-only tablets with no SIM).
 *
 * Requires SEND_SMS permission (declared in the manifest) to be granted
 * at runtime before calling send().
 */
class SmsFallbackSender(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * @param destinationE164 phone number in +1XXXXXXXXXX form
     * @param body the message text. Long messages are auto-split into parts.
     */
    fun send(destinationE164: String, body: String): Result<Unit> {
        if (!hasPermission()) {
            return Result.failure(IllegalStateException("SEND_SMS permission not granted"))
        }
        return try {
            // NOTE: context.getSystemService(SmsManager::class.java) only works on
            // API 31+. minSdk here is 26, so that call returns null on API 26-30 and
            // every send silently fails. SmsManager.getDefault() works on all API levels.
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(body)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(destinationE164, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationE164, null, body, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
