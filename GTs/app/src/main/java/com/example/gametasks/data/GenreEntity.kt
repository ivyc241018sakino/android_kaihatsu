package com.example.gametasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey(autoGenerate = true) val genre_id: Int = 0,
    val genre_name: String
)