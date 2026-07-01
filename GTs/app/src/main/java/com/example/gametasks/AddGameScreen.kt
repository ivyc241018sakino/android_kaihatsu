package com.example.gametasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    viewModel: GameViewModel,
    gameId: Int?, // MainActivity から受け取ったゲームID
    onSaved: () -> Unit
) {
    // ⭕️ ここで自動連動：画面が開いた時に新規か編集かを自動で判別して読み込みます
    LaunchedEffect(gameId) {
        if (gameId != null) {
            viewModel.loadGameData(gameId) // 既存のデータを自動セット
        } else {
            viewModel.clearInputs() // 新規の場合は入力欄を空っぽに
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (gameId == null) "ゲームを追加" else "ゲームの編集・詳細") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // ⭕️ ここで自動連動：保存時に gameId を渡して、新規か上書きかを判断させます
                viewModel.saveGame(gameId) { onSaved() }
            }) {
                Icon(Icons.Default.Save, contentDescription = "保存")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ゲーム名入力
            OutlinedTextField(
                value = viewModel.titleInput,
                onValueChange = { viewModel.titleInput = it },
                label = { Text("ゲーム名") },
                modifier = Modifier.fillMaxWidth()
            )

            // メモ欄
            OutlinedTextField(
                value = viewModel.memoInput,
                onValueChange = { viewModel.memoInput = it },
                label = { Text("メモ（デイリー任務の内容など）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // 更新時間設定
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.updateHourInput.toString(),
                    onValueChange = { viewModel.updateHourInput = it.toIntOrNull() ?: 0 },
                    label = { Text("時") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = viewModel.updateMinuteInput.toString(),
                    onValueChange = { viewModel.updateMinuteInput = it.toIntOrNull() ?: 0 },
                    label = { Text("分") },
                    modifier = Modifier.weight(1f)
                )
            }

            // リマインダー設定
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("リマインダー通知を有効にする")
                Switch(
                    checked = viewModel.reminderEnabledInput,
                    onCheckedChange = { viewModel.reminderEnabledInput = it }
                )
            }
        }
    }
}