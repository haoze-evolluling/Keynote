package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.AIChatDatabase
import com.haoze.keynote.data.db.BillDatabase
import com.haoze.keynote.data.db.HabitDatabase
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.ScheduleDatabase
import com.haoze.keynote.data.db.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrashCleanupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                NoteDatabase.getDatabase(context).noteDao().deleteExpiredTrashNotes(expireTime)
                BillDatabase.getDatabase(context).billDao().deleteExpiredTrashBills(expireTime)
                ScheduleDatabase.getDatabase(context).scheduleDao().deleteExpiredTrashSchedules(expireTime)
                TodoDatabase.getDatabase(context).todoDao().deleteExpiredTrashTodos(expireTime)
                HabitDatabase.getDatabase(context).habitDao().deleteExpiredHabits(expireTime)
                AIChatDatabase.getDatabase(context).aiChatDao().deleteExpiredTrashConversations(expireTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
