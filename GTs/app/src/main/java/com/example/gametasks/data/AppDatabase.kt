package com.example.gametasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameEntity::class,
        GenreEntity::class,
        PlatformEntity::class,
        GameGenreCrossRef::class,
        GamePlatformCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "game_tasks_database"
                )
                    // 開発をラクにするため、最初はアプリ起動時に初期データ（ジャンル等）を入れられるようにする設定を後ほど行います
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}