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

    @Query("DELETE FROM games WHERE game_id IN (:gameIds)")
    suspend fun deleteGamesByIds(gameIds: List<Int>)

    @Transaction
    suspend fun deleteGamesWithRefs(gameIds: List<Int>) {
        gameIds.forEach { id ->
            deleteGenresForGame(id)
            deletePlatformsForGame(id)
        }
        deleteGamesByIds(gameIds)
    }

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

    // ③ デイリー完了状態の切り替え（Booleanに変更）
    @Query("UPDATE games SET completed = :completed, updated_at = :timestamp WHERE game_id = :gameId")
    suspend fun updateIndividualCompletionStatus(gameId: Int, completed: Boolean, timestamp: Long)

    @Query("UPDATE games SET completed = :completed, updated_at = :timestamp WHERE game_id IN (:gameIds)")
    suspend fun updateCompletionStatusForGames(gameIds: List<Int>, completed: Boolean, timestamp: Long)

    // ④ 毎日0:00の自動リセット用：すべてのゲームを未完了（0）にする
    @Query("UPDATE games SET completed = 0, updated_at = :timestamp")
    suspend fun resetAllGamesCompletion(timestamp: Long)

    // バッチ更新用の最適化クエリ
    @Query("UPDATE games SET completed = 0, updated_at = :timestamp WHERE completed = 1 AND updated_at < :resetTime")
    suspend fun resetCompletedGamesBefore(resetTime: Long, timestamp: Long)

    // --- マスターデータ（ジャンル・プラットフォーム）の取得・登録 ---

    @Query("SELECT * FROM genres ORDER BY genre_id ASC")
    fun getAllGenres(): Flow<List<GenreEntity>>

    @Query("SELECT * FROM platforms ORDER BY platform_id ASC")
    fun getAllPlatforms(): Flow<List<PlatformEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenres(genres: List<GenreEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlatforms(platforms: List<PlatformEntity>)

    @Delete
    suspend fun deleteGenre(genre: GenreEntity)

    @Delete
    suspend fun deletePlatform(platform: PlatformEntity)

    @Query("SELECT COUNT(*) FROM genres")
    suspend fun getGenreCount(): Int

    @Query("SELECT COUNT(*) FROM platforms")
    suspend fun getPlatformCount(): Int

    // --- 重複整理用のマージ処理 ---
    @Query("UPDATE OR IGNORE game_genres SET genre_id = :keptId WHERE genre_id = :duplicateId")
    suspend fun mergeGenreLinks(keptId: Int, duplicateId: Int)

    @Query("DELETE FROM game_genres WHERE genre_id = :duplicateId")
    suspend fun deleteGenreLinks(duplicateId: Int)

    @Query("UPDATE OR IGNORE game_platforms SET platform_id = :keptId WHERE platform_id = :duplicateId")
    suspend fun mergePlatformLinks(keptId: Int, duplicateId: Int)

    @Query("DELETE FROM game_platforms WHERE platform_id = :duplicateId")
    suspend fun deletePlatformLinks(duplicateId: Int)

    @Transaction
    suspend fun mergeGenres(keptId: Int, duplicateId: Int) {
        mergeGenreLinks(keptId, duplicateId)
        deleteGenreLinks(duplicateId)
    }

    @Transaction
    suspend fun mergePlatforms(keptId: Int, duplicateId: Int) {
        mergePlatformLinks(keptId, duplicateId)
        deletePlatformLinks(duplicateId)
    }

    // --- 達成度関連 ---
    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int

    @Query("SELECT COUNT(*) FROM games WHERE completed = 1")
    suspend fun getCompletedGameCount(): Int

    @Transaction
    suspend fun saveGameWithDetails(
        game: GameEntity,
        genreIds: Set<Int>,
        platformIds: Set<Int>
    ): Long {
        val gameId = if (game.gameId == 0) {
            insertGame(game).toInt()
        } else {
            updateGame(game)
            game.gameId
        }

        deleteGenresForGame(gameId)
        deletePlatformsForGame(gameId)

        genreIds.forEach { genreId ->
            insertGameGenreCrossRef(GameGenreCrossRef(gameId = gameId, genreId = genreId))
        }

        platformIds.forEach { platformId ->
            insertGamePlatformCrossRef(GamePlatformCrossRef(gameId = gameId, platformId = platformId))
        }

        return gameId.toLong()
    }
}