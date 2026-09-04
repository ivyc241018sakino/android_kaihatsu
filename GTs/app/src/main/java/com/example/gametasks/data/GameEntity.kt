package com.example.gametasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "game_id")
    val gameId: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "memo")
    val memo: String = "",

    @ColumnInfo(name = "update_hour")
    val updateHour: Int = 0,

    @ColumnInfo(name = "update_minute")
    val updateMinute: Int = 0,

    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,

    @ColumnInfo(name = "completed")
    val completed: Boolean = false,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)