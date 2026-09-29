package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.util.Roles
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps this device's push token in the shared `fcm_tokens` list, together with who is using it.
 *
 * The Cloud Function reads that list to decide who to notify: it skips Sub Agents and it skips the
 * person who caused the event. Storing the employee id and role here is what makes both possible.
 *
 * Records are keyed by the token itself, so one person with two phones gets two entries and a phone
 * that is handed to another salesman simply has its record rewritten.
 */
object FcmTokenRegistrar {

    private const val TAG = "FcmTokenRegistrar"
    private const val PREFS = "himat_fcm_prefs"
    private const val KEY_PENDING = "pending_token"
    private const val KEY_PUBLISHED = "published_token"
    private const val DB_URL = "https://himatsms-default-rtdb.firebaseio.com"

    /** Called from the messaging service, which has no idea who is signed in yet. */
    fun rememberPendingToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING, token)
            // A refreshed token must be published again even if the old one already was
            .remove(KEY_PUBLISHED)
            .apply()
    }

    /**
     * Publishes the current token for [employeeId] / [role].
     *
     * Sub Agents are not registered at all, so no push can ever reach them. Returns the token that
     * was written, or null when nothing was published.
     */
    suspend fun register(
        context: Context,
        employeeId: Long,
        employeeName: String,
        email: String,
        role: String
    ): String? = withContext(Dispatchers.IO) {
        if (Roles.isAgent(role)) {
            // Also clear anything registered earlier, e.g. this phone used to belong to staff
            unregister(context)
            return@withContext null
        }
        val token = currentToken(context) ?: return@withContext null
        try {
            val record = mapOf(
                "token" to token,
                "employeeId" to employeeId,
                "employeeName" to employeeName,
                "email" to email.trim().lowercase(),
                "role" to role,
                "platform" to "android",
                "updatedAt" to System.currentTimeMillis()
            )
            FirebaseDatabase.getInstance(DB_URL)
                .getReference("fcm_tokens")
                .child(token)
                .setValue(record)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PUBLISHED, token)
                .remove(KEY_PENDING)
                .apply()
            token
        } catch (e: Exception) {
            Log.w(TAG, "could not publish push token: ${e.message}")
            null
        }
    }

    /** Removes this device from the list, e.g. on sign out, so it stops receiving team pushes. */
    suspend fun unregister(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_PUBLISHED, null) ?: prefs.getString(KEY_PENDING, null)
        if (!token.isNullOrBlank()) {
            try {
                FirebaseDatabase.getInstance(DB_URL)
                    .getReference("fcm_tokens")
                    .child(token)
                    .removeValue()
            } catch (e: Exception) {
                Log.w(TAG, "could not remove push token: ${e.message}")
            }
        }
        prefs.edit().remove(KEY_PUBLISHED).remove(KEY_PENDING).apply()
    }

    /** A token remembered by the service, otherwise ask Firebase for one. */
    private suspend fun currentToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_PENDING, null)?.takeIf { it.isNotBlank() }?.let { return it }
        return withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine<String?> { cont ->
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token -> if (cont.isActive) cont.resumeWith(Result.success(token)) }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "token fetch failed: ${e.message}")
                        if (cont.isActive) cont.resumeWith(Result.success(null))
                    }
            }
        }
    }
}
