package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.KeyNoteDatabase
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
                val db = KeyNoteDatabase.getDatabase(context)
                db.noteDao().deleteExpiredTrashNotes(expireTime)
                db.billDao().deleteExpiredTrashBills(expireTime)
                db.scheduleDao().deleteExpiredTrashSchedules(expireTime)
                db.todoDao().deleteExpiredTrashTodos(expireTime)
                db.habitDao().deleteExpiredHabits(expireTime)
                db.aiChatDao().deleteExpiredTrashConversations(expireTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
