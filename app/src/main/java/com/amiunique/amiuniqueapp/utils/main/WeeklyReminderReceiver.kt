package com.amiunique.amiuniqueapp.utils.main

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amiunique.amiuniqueapp.R

class WeeklyReminderReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent){
        // 1) Android 13+ runtime permission check
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return  // user didn't allow notifications
        }

        // 2) App-level notifications disabled?
        val nmCompat = NotificationManagerCompat.from(context)
        if (!nmCompat.areNotificationsEnabled()) return

        // 3) Channel blocked? (Android O+)
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = nm.getNotificationChannel(NotifHelper.CHANNEL_ID)
            if (ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE) return
        }

        // 4) Ensure channel exists (safe to call repeatedly)
        NotifHelper.ensureChannel(context)

        // 5) Build content intent to open the app
        val openAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val contentPI = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6) Build and post the notification
        val notif = NotificationCompat.Builder(context, NotifHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(context.getString(R.string.fingerprint_reminder_title))
            .setContentText(context.getString(R.string.fingerprint_reminder_text))
            .setContentIntent(contentPI)
            .setAutoCancel(true)
            .build()

        try {
            nmCompat.notify(1001, notif)
        } catch (se: SecurityException) {
            // Defensive: some OEMs are picky; just swallow/log.
        }

    }
}