package com.example.gametasks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gametasks.data.AppDatabase
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameGenreCrossRef
import com.example.gametasks.data.GamePlatformCrossRef
import com.example.gametasks.data.GameWithDetails
import com.example.gametasks.data.GenreEntity
import com.example.gametasks.data.PlatformEntity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class GameBackupItem(
    val title: String,
    val memo: String,
    val updateHour: Int,
    val updateMinute: Int,
    val reminderEnabled: Boolean,
    val completed: Boolean,
    val imageUri: String?,
    val packageName: String?,
    val genres: List<String>,
    val platforms: List<String>
)

data class BackupData(
    val genres: List<String>,
    val platforms: List<String>,
    val games: List<GameBackupItem>
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()
    private val alarmHelper = AlarmHelper(application)
    private val dataStore = application.dataStore

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val REDUCE_MOTION_KEY = booleanPreferencesKey("reduce_motion")
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    val allGames: Flow<List<GameWithDetails>> = gameDao.getAllGamesWithDetails()
    val genres: Flow<List<GenreEntity>> = gameDao.getAllGenres()
    val platforms: Flow<List<PlatformEntity>> = gameDao.getAllPlatforms()

    // --- 検索・フィルター用ステート ---
    val searchQuery = MutableStateFlow("")
    val homeSearchQuery = MutableStateFlow("")
    val filterGenreIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterPlatformIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterCompleted = MutableStateFlow<Boolean?>(null) // null: 全て, true: 完了のみ, false: 未完了のみ

    // 検索画面の表示状態 (false: フィルタ入力, true: 結果表示)
    val showSearchResults = MutableStateFlow(false)

    // テーマ設定 (true: ダークモード, false: ライトモード)
    val isDarkMode: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 動きを減らす設定
    val isReduceMotion: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[REDUCE_MOTION_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 通知設定
    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // 言語設定
    val currentLanguage: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[LANGUAGE_KEY] ?: "ja" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "ja")

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = enabled
            }
        }
    }

    fun toggleReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[REDUCE_MOTION_KEY] = enabled
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = languageCode
            }
        }
    }

    // 検索・フィルター適用済みのゲーム一覧
    val filteredGames: StateFlow<List<GameWithDetails>> = combine(
        allGames,
        searchQuery,
        filterGenreIds,
        filterPlatformIds,
        filterCompleted
    ) { games, query, genreIds, platformIds, completed ->
        games.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.game.title.contains(query, ignoreCase = true) ||
                    item.game.memo.contains(query, ignoreCase = true) ||
                    item.genres.any { it.genreName.contains(query, ignoreCase = true) } ||
                    item.platforms.any { it.platformName.contains(query, ignoreCase = true) }
            val matchesGenre = genreIds.isEmpty() || item.genres.any { it.genreId in genreIds }
            val matchesPlatform = platformIds.isEmpty() || item.platforms.any { it.platformId in platformIds }
            val matchesCompleted = completed == null || item.game.completed == completed

            matchesQuery && matchesGenre && matchesPlatform && matchesCompleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 達成度計算用 ---
    val todayAchievement: StateFlow<Double> = allGames.map { games ->
        if (games.isEmpty()) 0.0
        else {
            val completedCount = games.count { it.game.completed }
            completedCount.toDouble() / games.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- 登録・編集フォーム状態 ---
    var titleInput by mutableStateOf("")
    var memoInput by mutableStateOf("")
    var updateHourInput by mutableStateOf(0)
    var updateMinuteInput by mutableStateOf(0)
    var reminderEnabledInput by mutableStateOf(false)
    var imageUriInput by mutableStateOf<String?>(null)
    var packageNameInput by mutableStateOf<String?>(null)
    var selectedGenreIds by mutableStateOf(setOf<Int>())
    var selectedPlatformIds by mutableStateOf(setOf<Int>())

    // --- インストール済みアプリ取得用 ---
    data class AppInfo(val name: String, val packageName: String, val iconUri: String)
    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())

    fun loadInstalledApps(context: Context) {
        if (installedApps.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedInfos = try {
                pm.queryIntentActivities(mainIntent, 0)
            } catch (e: Exception) {
                emptyList()
            }
            
            val apps = resolvedInfos.map { 
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    iconUri = "package:${it.activityInfo.packageName}"
                )
            }.distinctBy { it.packageName }.sortedBy { it.name }
            
            withContext(Dispatchers.Main) {
                installedApps = apps
            }
        }
    }

    fun launchGameApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "アプリが見つかりません", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "アプリの起動に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleGenreSelection(genreId: Int) {
        selectedGenreIds = if (selectedGenreIds.contains(genreId)) {
            selectedGenreIds - genreId
        } else {
            selectedGenreIds + genreId
        }
    }

    fun togglePlatformSelection(platformId: Int) {
        selectedPlatformIds = if (selectedPlatformIds.contains(platformId)) {
            selectedPlatformIds - platformId
        } else {
            selectedPlatformIds + platformId
        }
    }

    fun clearInputs() {
        titleInput = ""
        memoInput = ""
        updateHourInput = 0
        updateMinuteInput = 0
        reminderEnabledInput = false
        imageUriInput = null
        packageNameInput = null
        selectedGenreIds = emptySet()
        selectedPlatformIds = emptySet()
    }

    fun loadGameData(gameId: Int) {
        viewModelScope.launch {
            val gameWithDetails = gameDao.getGameWithDetailsById(gameId)
            if (gameWithDetails != null) {
                val game = gameWithDetails.game
                titleInput = game.title
                memoInput = game.memo
                updateHourInput = game.updateHour
                updateMinuteInput = game.updateMinute
                reminderEnabledInput = game.reminderEnabled
                imageUriInput = game.imageUri
                packageNameInput = game.packageName
                selectedGenreIds = gameWithDetails.genres.map { it.genreId }.toSet()
                selectedPlatformIds = gameWithDetails.platforms.map { it.platformId }.toSet()
            }
        }
    }

    fun saveGame(gameId: Int?, onSuccess: () -> Unit) {
        if (titleInput.isBlank()) return

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            val gameEntity = if (gameId == null) {
                GameEntity(
                    title = titleInput,
                    memo = memoInput,
                    updateHour = updateHourInput,
                    updateMinute = updateMinuteInput,
                    reminderEnabled = reminderEnabledInput,
                    completed = false,
                    imageUri = imageUriInput,
                    packageName = packageNameInput,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            } else {
                val existingDetails = gameDao.getGameWithDetailsById(gameId)
                val completedStatus = existingDetails?.game?.completed ?: false
                val createdAtTime = existingDetails?.game?.createdAt ?: currentTime

                GameEntity(
                    gameId = gameId,
                    title = titleInput,
                    memo = memoInput,
                    updateHour = updateHourInput,
                    updateMinute = updateMinuteInput,
                    reminderEnabled = reminderEnabledInput,
                    completed = completedStatus,
                    imageUri = imageUriInput,
                    packageName = packageNameInput,
                    createdAt = createdAtTime,
                    updatedAt = currentTime
                )
            }

            val targetGameId = gameDao.saveGameWithDetails(
                game = gameEntity,
                genreIds = selectedGenreIds,
                platformIds = selectedPlatformIds
            ).toInt()

            // 通知のスケジュール設定
            if (reminderEnabledInput) {
                alarmHelper.scheduleReminder(targetGameId, titleInput, updateHourInput, updateMinuteInput)
            } else {
                alarmHelper.cancelReminder(targetGameId)
            }

            // ホーム画面の検索結果をリセット（保存後に表示が消えないように）
            homeSearchQuery.value = ""

            clearInputs()
            onSuccess()
        }
    }

    // ゲーム削除処理
    fun deleteGame(gameId: Int, onDeleteComplete: () -> Unit) {
        viewModelScope.launch {
            alarmHelper.cancelReminder(gameId)
            gameDao.deleteGamesWithRefs(listOf(gameId))
            clearInputs()
            onDeleteComplete()
        }
    }

    // 複数ゲーム削除処理
    fun deleteGames(gameIds: Set<Int>) {
        viewModelScope.launch {
            val idList = gameIds.toList()
            idList.forEach { alarmHelper.cancelReminder(it) }
            gameDao.deleteGamesWithRefs(idList)
        }
    }

    init {
        refreshGameStatuses()
        cleanUpDuplicates()
    }

    private fun cleanUpDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ジャンルの重複整理
                val allGenres = gameDao.getAllGenres().first()
                val genreMap = mutableMapOf<String, Int>() // normalizedName -> keptId
                
                allGenres.forEach { genre ->
                    val normalizedName = genre.genreName.trim().lowercase()
                    val keptId = genreMap[normalizedName]
                    if (keptId != null) {
                        // 重複発見：リンクを付け替えてから削除
                        gameDao.mergeGenreLinks(keptId, genre.genreId)
                        gameDao.deleteGenreLinks(genre.genreId)
                        gameDao.deleteGenre(genre)
                    } else {
                        genreMap[normalizedName] = genre.genreId
                    }
                }

                // プラットフォームの重複整理
                val allPlatforms = gameDao.getAllPlatforms().first()
                val platformMap = mutableMapOf<String, Int>()
                
                allPlatforms.forEach { platform ->
                    val normalizedName = platform.platformName.trim().lowercase()
                    val keptId = platformMap[normalizedName]
                    if (keptId != null) {
                        gameDao.mergePlatformLinks(keptId, platform.platformId)
                        gameDao.deletePlatformLinks(platform.platformId)
                        gameDao.deletePlatform(platform)
                    } else {
                        platformMap[normalizedName] = platform.platformId
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshGameStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // RoomはTransaction内での処理を推奨
                val currentTime = System.currentTimeMillis()
                val now = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
                
                // すべての完了済みゲームを一括取得
                val games = gameDao.getAllGamesWithDetails().first().filter { it.game.completed }
                if (games.isEmpty()) return@launch

                val idsToReset = mutableListOf<Int>()
                
                games.forEach { item ->
                    val game = item.game
                    val reset = java.util.Calendar.getInstance().apply {
                        timeInMillis = currentTime
                        set(java.util.Calendar.HOUR_OF_DAY, game.updateHour)
                        set(java.util.Calendar.MINUTE, game.updateMinute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    
                    if (now.before(reset)) {
                        reset.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    }

                    if (game.updatedAt < reset.timeInMillis) {
                        idsToReset.add(game.gameId)
                    }
                }

                if (idsToReset.isNotEmpty()) {
                    gameDao.updateCompletionStatusForGames(idsToReset, false, currentTime)
                }
            } catch (e: Exception) {
                // クラッシュ回避のためのログ出力（本番環境では適切なログライブラリを使用）
                android.util.Log.e("GameViewModel", "Error refreshing statuses", e)
            }
        }
    }

    fun updateGame(game: GameEntity) {
        viewModelScope.launch {
            val updatedGame = game.copy(updatedAt = System.currentTimeMillis())
            gameDao.updateGame(updatedGame)
        }
    }

    fun toggleCompletion(gameId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            gameDao.updateIndividualCompletionStatus(gameId, isChecked, System.currentTimeMillis())
        }
    }

    // マスターデータ追加
    fun addGenre(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            gameDao.insertGenres(listOf(GenreEntity(genreName = trimmedName)))
        }
    }

    fun addPlatform(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            gameDao.insertPlatforms(listOf(PlatformEntity(platformName = trimmedName)))
        }
    }

    fun deleteGenre(genre: GenreEntity) {
        viewModelScope.launch {
            gameDao.deleteGenre(genre)
        }
    }

    fun deletePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            gameDao.deletePlatform(platform)
        }
    }

    fun clearSearchFilters() {
        searchQuery.value = ""
        filterGenreIds.value = emptySet()
        filterPlatformIds.value = emptySet()
        filterCompleted.value = null
        showSearchResults.value = false
    }

    // --- バックアップ・復元機能 ---
    fun generateBackupCode(onGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val currentGenres = genres.first().map { it.genreName }
            val currentPlatforms = platforms.first().map { it.platformName }
            val currentGames = allGames.first()

            val gameBackups = currentGames.map { item ->
                GameBackupItem(
                    title = item.game.title,
                    memo = item.game.memo,
                    updateHour = item.game.updateHour,
                    updateMinute = item.game.updateMinute,
                    reminderEnabled = item.game.reminderEnabled,
                    completed = item.game.completed,
                    imageUri = item.game.imageUri,
                    packageName = item.game.packageName,
                    genres = item.genres.map { it.genreName },
                    platforms = item.platforms.map { it.platformName }
                )
            }

            val backup = BackupData(currentGenres, currentPlatforms, gameBackups)
            val json = Gson().toJson(backup)
            val code = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
            onGenerated(code)
        }
    }

    fun restoreFromCode(code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = String(Base64.decode(code, Base64.NO_WRAP))
                val type = object : TypeToken<BackupData>() {}.type
                val backup: BackupData = Gson().fromJson(json, type)

                // 1. ジャンルとプラットフォームの復元 (重複は無視)
                val existingGenres = genres.first().map { it.genreName.trim().lowercase() }
                val newGenres = backup.genres
                    .map { it.trim() }
                    .filter { it.lowercase() !in existingGenres }
                    .distinctBy { it.lowercase() }
                    .map { GenreEntity(genreName = it) }
                if (newGenres.isNotEmpty()) gameDao.insertGenres(newGenres)

                val existingPlatforms = platforms.first().map { it.platformName.trim().lowercase() }
                val newPlatforms = backup.platforms
                    .map { it.trim() }
                    .filter { it.lowercase() !in existingPlatforms }
                    .distinctBy { it.lowercase() }
                    .map { PlatformEntity(platformName = it) }
                if (newPlatforms.isNotEmpty()) gameDao.insertPlatforms(newPlatforms)

                // ID解決用の最新マップを取得 (トリム・小文字でマッチング)
                val allGenresList = genres.first()
                val genreMap = allGenresList.associate { it.genreName.trim().lowercase() to it.genreId }
                val allPlatformsList = platforms.first()
                val platformMap = allPlatformsList.associate { it.platformName.trim().lowercase() to it.platformId }

                // 2. ゲームの復元
                val existingGameTitles = allGames.first().map { it.game.title.trim().lowercase() }
                
                backup.games.forEach { gameBackup ->
                    // 同じタイトルのゲームが既にある場合はスキップ（重複防止）
                    if (gameBackup.title.trim().lowercase() in existingGameTitles) return@forEach

                    val newGame = GameEntity(
                        title = gameBackup.title,
                        memo = gameBackup.memo,
                        updateHour = gameBackup.updateHour,
                        updateMinute = gameBackup.updateMinute,
                        reminderEnabled = gameBackup.reminderEnabled,
                        completed = gameBackup.completed,
                        imageUri = gameBackup.imageUri,
                        packageName = gameBackup.packageName
                    )
                    val gameId = gameDao.insertGame(newGame).toInt()

                    // 中間テーブル（ジャンル・プラットフォーム）の紐付けを復元
                    gameBackup.genres.forEach { name ->
                        genreMap[name]?.let { id ->
                            gameDao.insertGameGenreCrossRef(GameGenreCrossRef(gameId = gameId, genreId = id))
                        }
                    }
                    gameBackup.platforms.forEach { name ->
                        platformMap[name]?.let { id ->
                            gameDao.insertGamePlatformCrossRef(GamePlatformCrossRef(gameId = gameId, platformId = id))
                        }
                    }

                    // リマインダーの再設定
                    if (newGame.reminderEnabled) {
                        alarmHelper.scheduleReminder(gameId, newGame.title, newGame.updateHour, newGame.updateMinute)
                    }
                }

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
