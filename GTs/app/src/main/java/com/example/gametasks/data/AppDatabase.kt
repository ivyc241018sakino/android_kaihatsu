package com.example.gametasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GameEntity::class,
        GenreEntity::class,
        PlatformEntity::class,
        GameGenreCrossRef::class,
        GamePlatformCrossRef::class
    ],
    version = 4,
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
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(getDatabase(context).gameDao())
                }
            }

            private suspend fun populateInitialData(gameDao: GameDao) {
                // ジャンル初期データの挿入
                val initialGenres = listOf(
                    GenreEntity(genreName = "ターン制RPG"),
                    GenreEntity(genreName = "オープンワールドRPG"),
                    GenreEntity(genreName = "FPS")
                )
                gameDao.insertGenres(initialGenres)

                // プラットフォーム初期データの挿入
                val initialPlatforms = listOf(
                    PlatformEntity(platformName = "スマホ"),
                    PlatformEntity(platformName = "PC"),
                    PlatformEntity(platformName = "PS5"),
                    PlatformEntity(platformName = "SWITCH")
                )
                gameDao.insertPlatforms(initialPlatforms)
            }
        }
    }
}