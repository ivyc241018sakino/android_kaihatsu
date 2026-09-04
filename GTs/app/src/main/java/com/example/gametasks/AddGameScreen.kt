package com.example.gametasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gametasks.ui.theme.DarkRed
import com.example.gametasks.ui.theme.GoldAccent
import com.example.gametasks.ui.theme.PurpleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    viewModel: GameViewModel,
    gameId: Int?,
    onSaved: () -> Unit
) {
    val genres by viewModel.genres.collectAsState(initial = emptyList())
    val platforms by viewModel.platforms.collectAsState(initial = emptyList())

    LaunchedEffect(gameId) {
        if (gameId != null) {
            viewModel.loadGameData(gameId)
        } else {
            viewModel.clearInputs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gameId == null) "ゲームを追加" else "ゲームを編集", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // アイコン選択セクション
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .clickable { /* 画像選択処理 */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("アイコンを選択", color = Color.Gray, fontSize = 14.sp)
            }

            // ゲーム名
            Column {
                Text("ゲーム名", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.titleInput,
                    onValueChange = { viewModel.titleInput = it },
                    placeholder = { Text("(例) 原神", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = GoldAccent
                    )
                )
            }

            // ジャンル選択 (複数選択可のUI)
            Column {
                Text("ジャンル", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        FilterChip(
                            selected = viewModel.selectedGenreId == genre.genreId,
                            onClick = {
                                viewModel.selectedGenreId = if (viewModel.selectedGenreId == genre.genreId) null else genre.genreId
                            },
                            label = { Text(genre.genreName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // プラットフォーム選択
            Column {
                Text("プラットフォーム", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platforms.forEach { platform ->
                        FilterChip(
                            selected = viewModel.selectedPlatformId == platform.platformId,
                            onClick = {
                                viewModel.selectedPlatformId = if (viewModel.selectedPlatformId == platform.platformId) null else platform.platformId
                            },
                            label = { Text(platform.platformName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 更新時間
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("更新時間", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%02d:%02d ⏰", viewModel.updateHourInput, viewModel.updateMinuteInput),
                    color = GoldAccent,
                    fontSize = 18.sp
                )
            }

            // リマインダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("リマインダー", color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (viewModel.reminderEnabledInput) "ON" else "OFF", color = GoldAccent, modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = viewModel.reminderEnabledInput,
                        onCheckedChange = { viewModel.reminderEnabledInput = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = GoldAccent.copy(alpha = 0.5f))
                    )
                }
            }

            // メモ
            Column {
                Text("メモ (任意)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.memoInput,
                    onValueChange = { viewModel.memoInput = it },
                    placeholder = { Text("デイリー任務の内容など...", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = GoldAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存・削除ボタン
            Button(
                onClick = { viewModel.saveGame(gameId) { onSaved() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
            ) {
                Text("保存する", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (gameId != null) {
                OutlinedButton(
                    onClick = { viewModel.deleteGame(gameId) { onSaved() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = borderStroke(1.dp, DarkRed)
                ) {
                    Text("削除する", color = Color(0xFFCF6679), fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
