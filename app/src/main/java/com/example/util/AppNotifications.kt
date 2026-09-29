package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Phone notifications for work happening elsewhere in the agency: a new trip, a salesman joining,
 * a new order. Every device writes a small record to the `notifications` node and every other
 * device turns it into a notification, so no server component is needed.
 *
 * Limitation worth knowing: this fires while the app's Firebase listener is alive (app open, or
 * recently used and still in memory). Delivery to a fully closed app needs Firebase Cloud
 * Messaging, which needs a paid plan and a server function.
 */
object AppNotifications {

    const val CHANNEL_ID = "himat_work_updates"
    private const val CHANNEL_NAME = "Trips and orders"
    private const val CHANNEL_DESC = "New trips, salesmen joining and new orders from your team"

    /** Kinds of thing we announce. Kept as plain strings so the web can write them too. */
    const val TYPE_TRIP = "trip"
    const val TYPE_JOIN = "join"
    const val TYPE_ORDER = "order"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Shows one notification. [tag] keeps repeats of the same event from stacking up, so a record
     * re-delivered by a later snapshot replaces the old notification instead of adding another.
     */
    fun show(context: Context, tag: String, title: String, body: String) {
        ensureChannel(context)
        if (!canPost(context)) return

        val open = Intent(context, Class.forName("com.example.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            tag.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.himat_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(tag, tag.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post
        }
    }
}

/**
 * One announcement, as stored under the RTDB `notifications` node.
 *
 * [actorId] is the employee who did it, so a phone never notifies its own user about their own work.
 */
data class WorkNotification(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val actorId: Long = 0,
    val actorName: String = "",
    /** Fallback identity for an owner who has no staff record, so they skip their own push. */
    val actorEmail: String = "",
    val refId: Long = 0,
    val createdAt: Long = 0
)
