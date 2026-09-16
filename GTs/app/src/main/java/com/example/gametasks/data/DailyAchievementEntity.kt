package com.example.gametasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_achievements")
data class DailyAchievementEntity(
    @PrimaryKey
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long, // 日付の開始ミリ秒 (00:00:00)
    
    @ColumnInfo(name = "achievement_rate")
    val achievementRate: Double // 0.0 to 1.0
)
