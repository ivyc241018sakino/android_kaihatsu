package com.example.gametasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY title ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE gameId = :id")
    suspend fun getGameById(id: Int): GameEntity?

    @Insert
    suspend fun insert(game: GameEntity)

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)
}