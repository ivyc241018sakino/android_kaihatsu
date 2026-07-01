package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_platforms",
    primaryKeys = ["game_id", "platform_id"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["game_id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlatformEntity::class,
            parentColumns = ["platform_id"],
            childColumns = ["platform_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("platform_id")]
)
data class GamePlatformCrossRef(
    val game_id: Int,
    val platform_id: Int
)