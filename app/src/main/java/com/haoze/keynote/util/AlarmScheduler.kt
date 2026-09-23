package com.haoze.keynote.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.haoze.keynote.receiver.AlarmReceiver

object AlarmScheduler {
    private const val ALARM_TYPE = "SCHEDULE"

    fun scheduleAlarm(context: Context, id: Long, eventTime: Long, minutesBefore: Int) {
        val alarmTime = eventTime - minutesBefore * 60_000L
        if (alarmTime <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", ALARM_TYPE)
            putExtra("id", id)
        }
        val requestCode = (ALARM_TYPE.hashCode() * 100000 + id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
    }

    fun cancelAlarm(context: Context, id: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = (ALARM_TYPE.hashCode() * 100000 + id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
