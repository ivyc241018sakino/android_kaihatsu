package com.example.gametasks

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gametasks.data.AppDatabase

class ResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ResetWorker", "自動リセット処理を開始します")
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val gameDao = database.gameDao()

            val currentTime = System.currentTimeMillis()
            gameDao.resetAllGamesCompletion(currentTime)

            Log.d("ResetWorker", "すべてのタスクの完了状態をリセットしました")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ResetWorker", "リセット処理中にエラーが発生しました", e)
            return Result.failure()
        }
    }
}