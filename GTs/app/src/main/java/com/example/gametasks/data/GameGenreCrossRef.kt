package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_genres",
    primaryKeys = ["game_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["game_id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["genre_id"],
            childColumns = ["genre_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("genre_id")]
)
data class GameGenreCrossRef(
    val game_id: Int,
    val genre_id: Int
)