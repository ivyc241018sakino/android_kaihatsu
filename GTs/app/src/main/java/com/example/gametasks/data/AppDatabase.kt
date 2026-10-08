package com.example.gametasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [
        GameEntity::class,
        GenreEntity::class,
        PlatformEntity::class,
        GameGenreCrossRef::class,
        GamePlatformCrossRef::class
    ],
    version = 6,
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
                    // .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // 必要に応じて追加
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDatabase()
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // データベースが開かれるたびに、データが空なら投入する（破壊的移行対策）
                seedDatabase()
            }

            private fun seedDatabase() {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = getDatabase(context).gameDao()
                    // すでにデータがある場合はスキップして二重登録を防ぐ
                    if (dao.getAllGenres().first().isEmpty()) {
                        val initialGenres = listOf(
                            GenreEntity(genreName = "ターン制RPG"),
                            GenreEntity(genreName = "オープンワールドRPG"),
                            GenreEntity(genreName = "FPS"),
                            GenreEntity(genreName = "アクション"),
                            GenreEntity(genreName = "シミュレーション")
                        )
                        dao.insertGenres(initialGenres)
                    }

                    if (dao.getAllPlatforms().first().isEmpty()) {
                        val initialPlatforms = listOf(
                            PlatformEntity(platformName = "スマホ"),
                            PlatformEntity(platformName = "PC"),
                            PlatformEntity(platformName = "PS5"),
                            PlatformEntity(platformName = "Switch"),
                            PlatformEntity(platformName = "Xbox")
                        )
                        dao.insertPlatforms(initialPlatforms)
                    }
                }
            }
        }
    }
}