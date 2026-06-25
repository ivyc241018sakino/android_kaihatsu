package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(

    @PrimaryKey(autoGenerate = true)
    val gameId: Int = 0,

    val title: String,

    val memo: String = "",

    val updateHour: Int,

    val updateMinute: Int,

    val reminderEnabled: Boolean = true,

    val completed: Boolean = false,

    val imageUri: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)