package com.example.gametasks

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gametasks.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val gameId = intent.getIntExtra("gameId", -1)
        val action = intent.action

        if (gameId != -1 && action == "ACTION_COMPLETE") {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val gameDao = database.gameDao()
                    
                    val currentTime = System.currentTimeMillis()
                    gameDao.updateIndividualCompletionStatus(gameId, true, currentTime)
                    
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(gameId)
                } catch (e: Exception) {
                    // エラーログは削除
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
