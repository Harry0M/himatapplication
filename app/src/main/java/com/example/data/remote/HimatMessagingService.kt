package com.example.data.remote

import com.example.util.AppNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives the push sent by the `pushWorkNotification` Cloud Function.
 *
 * When the app is in the background Android shows the message itself from the `notification` block,
 * so this class only has to handle the foreground case and token refreshes. Both paths reuse the
 * announcement id as the notification tag, so a message that also arrives through the in-app
 * Realtime Database listener replaces it instead of showing twice.
 */
class HimatMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notification = message.notification
        val title = notification?.title ?: data["title"] ?: "Himat Textile"
        val body = notification?.body ?: data["body"].orEmpty()
        if (body.isBlank()) return
        val tag = data["noteId"].orEmpty().ifBlank { message.messageId ?: body.hashCode().toString() }
        AppNotifications.show(applicationContext, tag, title, body)
    }

    /**
     * Firebase issued this device a new token. The signed-in user is not known here, so the token is
     * only remembered; [FcmTokenRegistrar] publishes it with the employee id and role the next time
     * the app runs.
     */
    override fun onNewToken(token: String) {
        FcmTokenRegistrar.rememberPendingToken(applicationContext, token)
    }
}
