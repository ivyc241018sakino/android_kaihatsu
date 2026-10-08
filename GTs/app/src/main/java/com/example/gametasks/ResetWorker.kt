package com.example.gametasks

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gametasks.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class ResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val gameDao = database.gameDao()
                val currentTime = System.currentTimeMillis()

                val gamesWithDetails = gameDao.getAllGamesWithDetails().first()
                val now = Calendar.getInstance().apply { timeInMillis = currentTime }
                
                database.withTransaction {
                    gamesWithDetails.forEach { item ->
                        val game = item.game
                        if (game.completed) {
                            val reset = Calendar.getInstance().apply {
                                timeInMillis = currentTime
                                set(Calendar.HOUR_OF_DAY, game.updateHour)
                                set(Calendar.MINUTE, game.updateMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            if (now.before(reset)) {
                                reset.add(Calendar.DAY_OF_YEAR, -1)
                            }
                            
                            if (game.updatedAt < reset.timeInMillis) {
                                gameDao.updateIndividualCompletionStatus(game.gameId, false, currentTime)
                            }
                        }
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}
