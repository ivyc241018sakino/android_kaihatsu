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
    version = 2,
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
                    .addCallback(AppDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.gameDao())
                    }
                }
            }

            private suspend fun populateInitialData(gameDao: GameDao) {
                // ジャンル初期データの挿入
                val initialGenres = listOf(
                    GenreEntity(genreName = "RPG"),
                    GenreEntity(genreName = "アクション"),
                    GenreEntity(genreName = "パズル"),
                    GenreEntity(genreName = "シミュレーション"),
                    GenreEntity(genreName = "戦略/ストラテジー"),
                    GenreEntity(genreName = "リズム/音ゲー"),
                    GenreEntity(genreName = "アドベンチャー")
                )
                gameDao.insertGenres(initialGenres)

                // プラットフォーム初期データの挿入
                val initialPlatforms = listOf(
                    PlatformEntity(platformName = "iOS"),
                    PlatformEntity(platformName = "Android"),
                    PlatformEntity(platformName = "Nintendo Switch"),
                    PlatformEntity(platformName = "PlayStation"),
                    PlatformEntity(platformName = "PC")
                )
                gameDao.insertPlatforms(initialPlatforms)
            }
        }
    }
}