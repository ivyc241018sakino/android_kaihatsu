package com.example.gametasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_platforms",
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
    indices = [
        Index("game_id"),
        Index("platform_id")
    ]
)
data class GamePlatformCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "game_id")
    val gameId: Int,

    @ColumnInfo(name = "platform_id")
    val platformId: Int
)