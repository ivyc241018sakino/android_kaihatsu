package com.example.gametasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gametasks.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            val pendingResult = goAsync() // 処理完了までReceiverを生かしておく
            val alarmHelper = AlarmHelper(context)
            val database = AppDatabase.getDatabase(context)
            val gameDao = database.gameDao()

            scope.launch {
                try {
                    val games = gameDao.getAllGamesWithDetails().first()
                    // 通知有効かつ「まだ完了していない」タスクのみリマインダーを再セット
                    games.filter { it.game.reminderEnabled && !it.game.completed }.forEach { item ->
                        alarmHelper.scheduleReminder(
                            item.game.gameId,
                            item.game.title,
                            item.game.updateHour,
                            item.game.updateMinute
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish() // 処理終了を通知
                }
            }
        }
    }
}
