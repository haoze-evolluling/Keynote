package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.ScheduleDatabase
import com.haoze.keynote.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduleDao = ScheduleDatabase.getDatabase(context).scheduleDao()
                scheduleDao.getAllSchedules().first().forEach { schedule ->
                    if (schedule.reminderEnabled && schedule.date > System.currentTimeMillis()) {
                        AlarmScheduler.scheduleAlarm(
                            context,
                            schedule.id, schedule.date, schedule.reminderMinutesBefore
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
