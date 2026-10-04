package com.haoze.keynote.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.haoze.keynote.MainActivity

object NotificationHelper {
    private const val CHANNEL_SCHEDULE = "schedule_reminder"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val scheduleChannel = NotificationChannel(
            CHANNEL_SCHEDULE, "日程提醒", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "日程事件提醒" }
        manager.createNotificationChannel(scheduleChannel)
    }

    fun showScheduleReminder(context: Context, scheduleId: Long, title: String, details: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(details.ifBlank { "你有日程安排" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(details.ifBlank { "你有日程安排" }))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(scheduleId.toInt(), notification)
    }
}
