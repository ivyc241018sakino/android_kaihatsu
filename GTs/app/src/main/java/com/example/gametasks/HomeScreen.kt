package com.example.gametasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameWithDetails
import com.example.gametasks.ui.theme.GoldAccent
import com.example.gametasks.ui.theme.PurpleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    gameList: List<GameWithDetails>,
    onCheckedChange: (GameEntity, Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onGameClick: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("すべて", "未完了", "完了")

    val filteredList = when (selectedTab) {
        1 -> gameList.filter { !it.game.completed }
        2 -> gameList.filter { it.game.completed }
        else -> gameList
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Daily Tasks",
                            color = GoldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // 簡易検索バー風の表示
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("検索 (ゲーム名・ジャンル・プラットフォーム)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = GoldAccent
                    ),
                    readOnly = true // ホームでは表示のみ
                )

                // タブ風フィルタ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        Column(
                            modifier = Modifier
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                color = if (selectedTab == index) GoldAccent else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedTab == index) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(40.dp)
                                        .background(GoldAccent)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRegister,
                containerColor = PurpleButton,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "ゲームを追加")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("タスクはありません", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredList) { item ->
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

@Composable
fun GameTaskCard(
    item: GameWithDetails,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val game = item.game

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ゲームアイコン代わりのプレースホルダ
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(game.title.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = item.genres.joinToString(" / ") { it.genreName },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${String.format("%02d:%02d", game.updateHour, game.updateMinute)}",
                        fontSize = 12.sp,
                        color = GoldAccent
                    )
                }
            }

            // カスタムチェックボックス（丸いデザイン）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (game.completed) GoldAccent else Color.Transparent)
                    .border(2.dp, if (game.completed) GoldAccent else Color.Gray, CircleShape)
                    .clickable { onCheckedChange(!game.completed) },
                contentAlignment = Alignment.Center
            ) {
                if (game.completed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.background
                    )
                }
            }
        }
    }
}
