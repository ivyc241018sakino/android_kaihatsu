package com.example.gametasks.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "genre_id")
    val genreId: Int = 0,

    @ColumnInfo(name = "genre_name")
    val genreName: String
)