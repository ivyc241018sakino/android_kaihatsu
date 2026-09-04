package com.example.gametasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "platform_id")
    val platformId: Int = 0,

    @ColumnInfo(name = "platform_name")
    val platformName: String
)