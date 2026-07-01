package com.example.gametasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // --- ゲーム（タスク）の追加・編集・削除 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    // --- 中間テーブルの操作（紐づけの登録・削除） ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameGenreCrossRef(crossRef: GameGenreCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlatformCrossRef(crossRef: GamePlatformCrossRef)

    @Query("DELETE FROM game_genres WHERE game_id = :gameId")
    suspend fun deleteGenresForGame(gameId: Int)

    @Query("DELETE FROM game_platforms WHERE game_id = :gameId")
    suspend fun deletePlatformsForGame(gameId: Int)

    // --- データの取得（画面用） ---

    // ① ホーム画面用：すべてのゲームを詳細付きで取得（Flowを使ってリアルタイム同期）
    @Transaction
    @Query("SELECT * FROM games ORDER BY update_hour ASC, update_minute ASC")
    fun getAllGamesWithDetails(): Flow<List<GameWithDetails>>

    // ② 詳細画面用：特定のゲームを1件取得
    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun getGameWithDetailsById(gameId: Int): GameWithDetails?

    // ③ デイリー完了状態の切り替え（一発で更新する用）
    @Query("UPDATE games SET completed = :completed, updated_at = :timestamp WHERE game_id = :gameId")
    suspend fun updateIndividualCompletionStatus(gameId: Int, completed: Int, timestamp: Long)

    // ④ 毎日0:00の自動リセット用：すべてのゲームを未完了（0）にする
    @Query("UPDATE games SET completed = 0, updated_at = :timestamp")
    suspend fun resetAllGamesCompletion(timestamp: Long)

    // --- マスターデータ（ジャンル・プラットフォーム）の初期登録用 ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlatforms(platforms: List<PlatformEntity>)
}