package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey(autoGenerate = true) val platform_id: Int = 0,
    val platform_name: String
)