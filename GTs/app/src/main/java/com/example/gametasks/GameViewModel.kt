package com.example.gametasks

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gametasks.data.AppDatabase
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()

    // データベース内のすべてのゲームを詳細（プラットフォーム等）付きでリアルタイムに監視するFlow
    val allGames: Flow<List<GameWithDetails>> = gameDao.getAllGamesWithDetails()

    // --- 登録画面の入力フォーム用データ（状態） ---
    var titleInput by mutableStateOf("")
    var memoInput by mutableStateOf("")
    var updateHourInput by mutableStateOf(0)
    var updateMinuteInput by mutableStateOf(0)
    var reminderEnabledInput by mutableStateOf(false)

    // 入力フォームを一括リセットする関数
    fun clearInputs() {
        titleInput = ""
        memoInput = ""
        updateHourInput = 0
        updateMinuteInput = 0
        reminderEnabledInput = false
    }

    // ⭕️【追加】既存のゲームデータをIDから読み込んで入力フォームに自動セットする関数
    fun loadGameData(gameId: Int) {
        viewModelScope.launch {
            val gameWithDetails = gameDao.getGameWithDetailsById(gameId)
            if (gameWithDetails != null) {
                val game = gameWithDetails.game
                titleInput = game.title
                memoInput = game.memo ?: ""
                updateHourInput = game.update_hour
                updateMinuteInput = game.update_minute
                reminderEnabledInput = game.reminder_enabled == 1
            }
        }
    }

    // ⭕️【修正】新規保存と上書き編集の両方に対応した saveGame 関数
    fun saveGame(gameId: Int?, onSuccess: () -> Unit) {
        if (titleInput.isBlank()) return // タイトルが空なら保存しない

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            if (gameId == null) {
                // 新規登録モード
                val newGame = GameEntity(
                    title = titleInput,
                    memo = memoInput,
                    update_hour = updateHourInput,
                    update_minute = updateMinuteInput,
                    reminder_enabled = if (reminderEnabledInput) 1 else 0,
                    completed = 0, // 最初は未完了
                    image_uri = null,
                    created_at = currentTime,
                    updated_at = currentTime
                )
                gameDao.insertGame(newGame)
            } else {
                // 上書き編集モード（既存の完了状態や作成日時をキープする）
                val existingDetails = gameDao.getGameWithDetailsById(gameId)
                val completedStatus = existingDetails?.game?.completed ?: 0
                val createdAtTime = existingDetails?.game?.created_at ?: currentTime

                val updatedGame = GameEntity(
                    game_id = gameId, // IDを指定することでRoomが既存データを「上書き」します
                    title = titleInput,
                    memo = memoInput,
                    update_hour = updateHourInput,
                    update_minute = updateMinuteInput,
                    reminder_enabled = if (reminderEnabledInput) 1 else 0,
                    completed = completedStatus,
                    image_uri = existingDetails?.game?.image_uri,
                    created_at = createdAtTime,
                    updated_at = currentTime
                )
                gameDao.updateGame(updatedGame)
            }

            // 入力欄をクリアして、画面を閉じる（onSuccess）を実行
            clearInputs()
            onSuccess()
        }
    }

    // チェックボックスが押されたときに完了状態(completed)を更新する関数
    fun updateGame(game: GameEntity) {
        viewModelScope.launch {
            gameDao.updateGame(game)
        }
    }
}