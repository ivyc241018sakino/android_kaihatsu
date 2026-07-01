package com.example.gametasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    gameList: List<GameWithDetails>,
    onCheckedChange: (GameEntity, Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onGameClick: (Int) -> Unit // ⭕️ タップされたゲームのIDを通知する処理を追加
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Tasks", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToRegister) {
                Icon(Icons.Default.Add, contentDescription = "ゲームを追加")
            }
        }
    ) { paddingValues ->
        if (gameList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("登録されたゲームはありません。\n右下のボタンから追加してください。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(gameList) { item ->
                    GameTaskCard(
                        item = item,
                        onCheckedChange = { isChecked ->
                            onCheckedChange(item.game, isChecked)
                        },
                        modifier = Modifier.clickable {
                            // ⭕️ カード全体をタップした時にゲームIDを渡す
                            onGameClick(item.game.game_id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameTaskCard(
    item: GameWithDetails,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier // ⭕️ clickableを受け取るために引数を追加
) {
    val game = item.game

    Card(
        modifier = modifier.fillMaxWidth(), // ⭕️ modifierを適用
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // ⭕️ 本物のハード名（platform_name）をバッジに表示！
                    if (item.platforms.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(item.platforms.first().platform_name, fontSize = 12.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = String.format("⏰ %02d:%02d", game.update_hour, game.update_minute),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!game.memo.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.memo,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = game.completed == 1,
                onCheckedChange = onCheckedChange
            )
        }
    }
}