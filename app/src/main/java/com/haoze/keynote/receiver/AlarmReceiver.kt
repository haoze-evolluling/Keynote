package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.ScheduleDatabase
import com.haoze.keynote.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannels(context)
        val type = intent.getStringExtra("type") ?: return
        val id = intent.getLongExtra("id", -1L)
        if (id == -1L) return

        when (type) {
            "SCHEDULE" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        showScheduleReminder(context.applicationContext, id, intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> return
        }
    }

    private suspend fun showScheduleReminder(context: Context, scheduleId: Long, intent: Intent) {
        val schedule = ScheduleDatabase.getDatabase(context).scheduleDao().getScheduleById(scheduleId)
        if (schedule?.isDeleted == true) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val title = schedule?.title
            ?: intent.getStringExtra("title")
            ?: return
        val details = buildList {
            schedule?.let {
                add(dateFormat.format(Date(it.date)))
                it.location?.takeIf { location -> location.isNotBlank() }?.let { location -> add(location) }
                it.description?.takeIf { description -> description.isNotBlank() }?.let { description -> add(description) }
            } ?: intent.getStringExtra("details")?.takeIf { it.isNotBlank() }?.let { add(it) }
        }

        NotificationHelper.showScheduleReminder(context, scheduleId, title, details.joinToString(" · "))
    }
}
