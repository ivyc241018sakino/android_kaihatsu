package com.example.gametasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val gameId = intent.getIntExtra("gameId", 0)
        val title = intent.getStringExtra("title") ?: "ゲームタスクの通知"
        val message = intent.getStringExtra("message") ?: "今日のタスクを確認しましょう！"

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(gameId, title, message)
    }
}
