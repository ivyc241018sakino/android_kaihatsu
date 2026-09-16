package com.example.gametasks

import android.app.Application
import android.content.Context
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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class GameBackupItem(
    val title: String,
    val memo: String,
    val updateHour: Int,
    val updateMinute: Int,
    val reminderEnabled: Boolean,
    val completed: Boolean,
    val imageUri: String?,
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 動きを減らす設定
    val isReduceMotion: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[REDUCE_MOTION_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    var selectedGenreIds by mutableStateOf(setOf<Int>())
    var selectedPlatformIds by mutableStateOf(setOf<Int>())

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
                selectedGenreIds = gameWithDetails.genres.map { it.genreId }.toSet()
                selectedPlatformIds = gameWithDetails.platforms.map { it.platformId }.toSet()
            }
        }
    }

    fun saveGame(gameId: Int?, onSuccess: () -> Unit) {
        if (titleInput.isBlank()) return

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()

            val targetGameId = if (gameId == null) {
                val newGame = GameEntity(
                    title = titleInput,
                    memo = memoInput,
                    updateHour = updateHourInput,
                    updateMinute = updateMinuteInput,
                    reminderEnabled = reminderEnabledInput,
                    completed = false,
                    imageUri = imageUriInput,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                gameDao.insertGame(newGame).toInt()
            } else {
                val existingDetails = gameDao.getGameWithDetailsById(gameId)
                val completedStatus = existingDetails?.game?.completed ?: false
                val createdAtTime = existingDetails?.game?.createdAt ?: currentTime

                val updatedGame = GameEntity(
                    gameId = gameId,
                    title = titleInput,
                    memo = memoInput,
                    updateHour = updateHourInput,
                    updateMinute = updateMinuteInput,
                    reminderEnabled = reminderEnabledInput,
                    completed = completedStatus,
                    imageUri = imageUriInput,
                    createdAt = createdAtTime,
                    updatedAt = currentTime
                )
                gameDao.updateGame(updatedGame)
                gameId
            }

            gameDao.deleteGenresForGame(targetGameId)
            gameDao.deletePlatformsForGame(targetGameId)

            selectedGenreIds.forEach { genreId ->
                gameDao.insertGameGenreCrossRef(
                    GameGenreCrossRef(gameId = targetGameId, genreId = genreId)
                )
            }

            selectedPlatformIds.forEach { platformId ->
                gameDao.insertGamePlatformCrossRef(
                    GamePlatformCrossRef(gameId = targetGameId, platformId = platformId)
                )
            }

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
    }

    fun refreshGameStatuses() {
        viewModelScope.launch {
            val gamesWithDetails = gameDao.getAllGamesWithDetails().first()
            val currentTime = System.currentTimeMillis()
            val now = java.util.Calendar.getInstance()
            
            gamesWithDetails.forEach { item ->
                val game = item.game
                if (game.completed) {
                    // そのゲームの直近の更新時間を計算
                    val reset = java.util.Calendar.getInstance().apply {
                        timeInMillis = currentTime
                        set(java.util.Calendar.HOUR_OF_DAY, game.updateHour)
                        set(java.util.Calendar.MINUTE, game.updateMinute)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    
                    // 現在時刻が更新時間より前なら、直近の更新は「昨日」
                    if (now.before(reset)) {
                        reset.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    }
                    
                    // もし完了ボタンが押された時間(updatedAt)が、その直近の更新時間より前なら、
                    // それは「前サイクル分」の完了なので、リセットする
                    if (game.updatedAt < reset.timeInMillis) {
                        gameDao.updateIndividualCompletionStatus(game.gameId, false, currentTime)
                    }
                }
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
        if (name.isBlank()) return
        viewModelScope.launch {
            gameDao.insertGenres(listOf(GenreEntity(genreName = name)))
        }
    }

    fun addPlatform(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            gameDao.insertPlatforms(listOf(PlatformEntity(platformName = name)))
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
                    genres = item.genres.map { it.genreName },
                    platforms = item.platforms.map { it.platformName }
                )
            }

            val backup = BackupData(currentGenres, currentPlatforms, gameBackups)
            val json = Gson().toJson(backup)
            val code = java.util.Base64.getEncoder().encodeToString(json.toByteArray())
            onGenerated(code)
        }
    }

    fun restoreFromCode(code: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = String(java.util.Base64.getDecoder().decode(code))
                val type = object : TypeToken<BackupData>() {}.type
                val backup: BackupData = Gson().fromJson(json, type)

                // 1. ジャンルとプラットフォームの復元 (重複は無視)
                val existingGenres = genres.first().map { it.genreName }
                val newGenres = backup.genres.filter { it !in existingGenres }.map { GenreEntity(genreName = it) }
                if (newGenres.isNotEmpty()) gameDao.insertGenres(newGenres)

                val existingPlatforms = platforms.first().map { it.platformName }
                val newPlatforms = backup.platforms.filter { it !in existingPlatforms }.map { PlatformEntity(platformName = it) }
                if (newPlatforms.isNotEmpty()) gameDao.insertPlatforms(newPlatforms)

                // ID解決用の最新マップを取得
                val allGenresList = genres.first()
                val genreMap = allGenresList.associate { it.genreName to it.genreId }
                val allPlatformsList = platforms.first()
                val platformMap = allPlatformsList.associate { it.platformName to it.platformId }

                // 2. ゲームの復元
                val existingGameTitles = allGames.first().map { it.game.title }
                
                backup.games.forEach { gameBackup ->
                    // 同じタイトルのゲームが既にある場合はスキップ（重複防止）
                    if (gameBackup.title in existingGameTitles) return@forEach

                    val newGame = GameEntity(
                        title = gameBackup.title,
                        memo = gameBackup.memo,
                        updateHour = gameBackup.updateHour,
                        updateMinute = gameBackup.updateMinute,
                        reminderEnabled = gameBackup.reminderEnabled,
                        completed = gameBackup.completed,
                        imageUri = gameBackup.imageUri
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
