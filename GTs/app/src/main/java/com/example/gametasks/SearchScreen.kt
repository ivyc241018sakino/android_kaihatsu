package com.example.gametasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gametasks.data.GameEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: GameViewModel,
    onGameClick: (Int) -> Unit,
    onCheckedChange: (GameEntity, Boolean) -> Unit
) {
    val filteredGames by viewModel.filteredGames.collectAsState()
    val genres by viewModel.genres.collectAsState(initial = emptyList())
    val platforms by viewModel.platforms.collectAsState(initial = emptyList())

    val query by viewModel.searchQuery.collectAsState()
    val selectedGenreId by viewModel.filterGenreId.collectAsState()
    val selectedPlatformId by viewModel.filterPlatformId.collectAsState()
    val selectedCompleted by viewModel.filterCompleted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ゲーム検索・フィルター") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 検索バー
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("タイトルやメモで検索...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "クリア")
                        }
                    }
                },
                singleLine = true
            )

            // フィルターチップ群
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ジャンル
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        FilterChip(
                            selected = selectedGenreId == genre.genreId,
                            onClick = {
                                viewModel.filterGenreId.value =
                                    if (selectedGenreId == genre.genreId) null else genre.genreId
                            },
                            label = { Text(genre.genreName) }
                        )
                    }
                }

                // ハード
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platforms.forEach { platform ->
                        FilterChip(
                            selected = selectedPlatformId == platform.platformId,
                            onClick = {
                                viewModel.filterPlatformId.value =
                                    if (selectedPlatformId == platform.platformId) null else platform.platformId
                            },
                            label = { Text(platform.platformName) }
                        )
                    }
                }

                // 完了状態 & リセット
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedCompleted == false,
                            onClick = {
                                viewModel.filterCompleted.value =
                                    if (selectedCompleted == false) null else false
                            },
                            label = { Text("未完了") }
                        )
                        FilterChip(
                            selected = selectedCompleted == true,
                            onClick = {
                                viewModel.filterCompleted.value =
                                    if (selectedCompleted == true) null else true
                            },
                            label = { Text("完了済み") }
                        )
                    }

                    TextButton(onClick = { viewModel.clearSearchFilters() }) {
                        Text("リセット")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 検索結果
            if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("条件に合うゲームが見つかりません")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredGames) { item ->
                        GameTaskCard(
                            item = item,
                            onCheckedChange = { isChecked ->
                                onCheckedChange(item.game, isChecked)
                            },
                            modifier = Modifier.clickable {
                                onGameClick(item.game.gameId)
                            }
                        )
                    }
                }
            }
        }
    }
}
