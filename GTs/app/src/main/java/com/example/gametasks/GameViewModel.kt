package com.example.gametasks

import android.app.Application
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val gameDao = AppDatabase.getDatabase(application).gameDao()

    val allGames: Flow<List<GameWithDetails>> = gameDao.getAllGamesWithDetails()
    val genres: Flow<List<GenreEntity>> = gameDao.getAllGenres()
    val platforms: Flow<List<PlatformEntity>> = gameDao.getAllPlatforms()

    // --- 検索・フィルター用ステート ---
    val searchQuery = MutableStateFlow("")
    val filterGenreId = MutableStateFlow<Int?>(null)
    val filterPlatformId = MutableStateFlow<Int?>(null)
    val filterCompleted = MutableStateFlow<Boolean?>(null) // null: 全て, true: 完了のみ, false: 未完了のみ

    // 検索・フィルター適用済みのゲーム一覧
    val filteredGames: StateFlow<List<GameWithDetails>> = combine(
        allGames,
        searchQuery,
        filterGenreId,
        filterPlatformId,
        filterCompleted
    ) { games, query, genreId, platformId, completed ->
        games.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.game.title.contains(query, ignoreCase = true) ||
                    item.game.memo.contains(query, ignoreCase = true)
            val matchesGenre = genreId == null || item.genres.any { it.genreId == genreId }
            val matchesPlatform = platformId == null || item.platforms.any { it.platformId == platformId }
            val matchesCompleted = completed == null || item.game.completed == completed

            matchesQuery && matchesGenre && matchesPlatform && matchesCompleted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 登録・編集フォーム状態 ---
    var titleInput by mutableStateOf("")
    var memoInput by mutableStateOf("")
    var updateHourInput by mutableStateOf(0)
    var updateMinuteInput by mutableStateOf(0)
    var reminderEnabledInput by mutableStateOf(false)
    var selectedGenreId by mutableStateOf<Int?>(null)
    var selectedPlatformId by mutableStateOf<Int?>(null)

    fun clearInputs() {
        titleInput = ""
        memoInput = ""
        updateHourInput = 0
        updateMinuteInput = 0
        reminderEnabledInput = false
        selectedGenreId = null
        selectedPlatformId = null
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
                selectedGenreId = gameWithDetails.genres.firstOrNull()?.genreId
                selectedPlatformId = gameWithDetails.platforms.firstOrNull()?.platformId
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
                    imageUri = null,
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
                    imageUri = existingDetails?.game?.imageUri,
                    createdAt = createdAtTime,
                    updatedAt = currentTime
                )
                gameDao.updateGame(updatedGame)
                gameId
            }

            gameDao.deleteGenresForGame(targetGameId)
            gameDao.deletePlatformsForGame(targetGameId)

            selectedGenreId?.let { genreId ->
                gameDao.insertGameGenreCrossRef(
                    GameGenreCrossRef(gameId = targetGameId, genreId = genreId)
                )
            }

            selectedPlatformId?.let { platformId ->
                gameDao.insertGamePlatformCrossRef(
                    GamePlatformCrossRef(gameId = targetGameId, platformId = platformId)
                )
            }

            clearInputs()
            onSuccess()
        }
    }

    // ゲーム削除処理
    fun deleteGame(gameId: Int, onDeleteComplete: () -> Unit) {
        viewModelScope.launch {
            gameDao.deleteGenresForGame(gameId)
            gameDao.deletePlatformsForGame(gameId)
            val gameDetails = gameDao.getGameWithDetailsById(gameId)
            if (gameDetails != null) {
                gameDao.deleteGame(gameDetails.game)
            }
            clearInputs()
            onDeleteComplete()
        }
    }

    fun updateGame(game: GameEntity) {
        viewModelScope.launch {
            gameDao.updateGame(game)
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
        filterGenreId.value = null
        filterPlatformId.value = null
        filterCompleted.value = null
    }
}