package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val game_id: Int = 0,
    val title: String,
    val memo: String,
    val update_hour: Int,
    val update_minute: Int,
    val reminder_enabled: Int, // 0: OFF, 1: ON
    val completed: Int,        // 0: 未完了, 1: 完了
    val image_uri: String?,    // アイコン画像URI（未設定時はnull）
    val created_at: Long,      // 作成日時（タイムスタンプ）
    val updated_at: Long       // 更新日時（タイムスタンプ）
)