package com.example.gametasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gametasks.data.GameEntity
import com.example.gametasks.ui.FutureButton
import com.example.gametasks.ui.theme.PurpleButton

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
    val selectedGenreIds by viewModel.filterGenreIds.collectAsState()
    val selectedPlatformIds by viewModel.filterPlatformIds.collectAsState()
    val selectedCompleted by viewModel.filterCompleted.collectAsState()
    val isReduceMotion by viewModel.isReduceMotion.collectAsState()

    val showResults by viewModel.showSearchResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("検索・フィルター", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (showResults) viewModel.showSearchResults.value = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (!showResults) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearSearchFilters() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("リセット", color = MaterialTheme.colorScheme.onBackground)
                        }
                        FutureButton(
                            onClick = { viewModel.showSearchResults.value = true },
                            isReduceMotion = isReduceMotion,
                            modifier = Modifier.weight(1f).height(50.dp),
                            containerColor = PurpleButton,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("検索する", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (showResults) {
            if (filteredGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("該当するゲームがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredGames) { item ->
                        GameTaskCard(
                            item = item,
                            isReduceMotion = isReduceMotion,
                            onCheckedChange = { isChecked: Boolean ->
                                onCheckedChange(item.game, isChecked)
                            },
                            modifier = Modifier.clickable {
                                onGameClick(item.game.gameId)
                            }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("検索キーワードを入力", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )

                FilterSection(
                    title = "ジャンル",
                    items = genres,
                    selectedIds = selectedGenreIds,
                    onItemClick = { id -> 
                        val newSet = if (selectedGenreIds.contains(id)) selectedGenreIds - id else selectedGenreIds + id
                        viewModel.filterGenreIds.value = newSet
                    },
                    onClearAll = { viewModel.filterGenreIds.value = emptySet() },
                    labelProvider = { it.genreName },
                    idProvider = { it.genreId }
                )

                FilterSection(
                    title = "プラットフォーム",
                    items = platforms,
                    selectedIds = selectedPlatformIds,
                    onItemClick = { id -> 
                        val newSet = if (selectedPlatformIds.contains(id)) selectedPlatformIds - id else selectedPlatformIds + id
                        viewModel.filterPlatformIds.value = newSet
                    },
                    onClearAll = { viewModel.filterPlatformIds.value = emptySet() },
                    labelProvider = { it.platformName },
                    idProvider = { it.platformId }
                )

                Column {
                    Text("完了状態", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip("すべて", selectedCompleted == null) { viewModel.filterCompleted.value = null }
                        StatusChip("未完了", selectedCompleted == false) { viewModel.filterCompleted.value = false }
                        StatusChip("完了", selectedCompleted == true) { viewModel.filterCompleted.value = true }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilterSection(
    title: String,
    items: List<T>,
    selectedIds: Set<Int>,
    onItemClick: (Int) -> Unit,
    onClearAll: () -> Unit,
    labelProvider: (T) -> String,
    idProvider: (T) -> Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Text(
                "すべて",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onClearAll() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val id = idProvider(item)
                val label = labelProvider(item)
                FilterChip(
                    selected = selectedIds.contains(id),
                    onClick = { onItemClick(id) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(40.dp).width(80.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
