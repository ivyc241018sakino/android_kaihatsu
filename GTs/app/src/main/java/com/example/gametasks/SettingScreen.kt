package com.example.gametasks

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gametasks.ui.theme.DarkRed
import com.example.gametasks.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: GameViewModel
) {
    val genres by viewModel.genres.collectAsState(initial = emptyList())
    val platforms by viewModel.platforms.collectAsState(initial = emptyList())
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isReduceMotion by viewModel.isReduceMotion.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AchievementSection(viewModel = viewModel, isReduceMotion = isReduceMotion)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                Text("通知設定", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                var notificationsMasterEnabled by remember { mutableStateOf(true) }
                SettingItemWithSwitch("通知を受け取る", notificationsMasterEnabled) {
                    notificationsMasterEnabled = it
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                Text("マスターデータ管理", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            // ジャンル管理
            item {
                MasterDataManagementSection(
                    title = "ジャンルの管理",
                    items = genres.map { it.genreName },
                    onAdd = { viewModel.addGenre(it) },
                    onDelete = { name -> 
                        genres.find { it.genreName == name }?.let { viewModel.deleteGenre(it) }
                    }
                )
            }

            // プラットフォーム管理
            item {
                MasterDataManagementSection(
                    title = "プラットフォームの管理",
                    items = platforms.map { it.platformName },
                    onAdd = { viewModel.addPlatform(it) },
                    onDelete = { name ->
                        platforms.find { it.platformName == name }?.let { viewModel.deletePlatform(it) }
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingItemWithSwitch(
                    title = if (isDarkMode) "ダークモード: ON" else "ダークモード: OFF",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                SettingItemWithSwitch(
                    title = if (isReduceMotion) "動きを減らす: ON" else "動きを減らす: OFF",
                    checked = isReduceMotion,
                    onCheckedChange = { viewModel.toggleReduceMotion(it) }
                )

                SettingItemWithArrow("バックアップ / 復元", "") {
                    showBackupDialog = true
                }
                
                SettingItemWithArrow("アプリについて", "") {
                    showAboutDialog = true
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("閉じる", color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text("アプリについて", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "このアプリは、主にスマホゲームのデイリー消化のための補助アプリです。\nご自身で、ジャンルや、内容を追加しながら、使いやすい自分だけのアプリにしましょう。",
                    lineHeight = 24.sp
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false },
            onShowSnackbar = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }
}

@Composable
fun BackupRestoreDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    var backupCode by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("バックアップ / 復元", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("バックアップコードの発行", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.generateBackupCode { code ->
                                backupCode = code
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("コードを生成")
                    }
                    if (backupCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupCode,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(backupCode))
                                    onShowSnackbar("コピーしました")
                                }) {
                                    Text("コピー")
                                }
                            }
                        )
                    }
                }

                HorizontalDivider()

                Column {
                    Text("コードから復元", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        placeholder = { Text("コードを貼り付け") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.restoreFromCode(inputCode) { success ->
                                if (success) {
                                    onShowSnackbar("復元が完了しました")
                                    onDismiss()
                                } else {
                                    onShowSnackbar("無効なコードです")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputCode.isNotEmpty()
                    ) {
                        Text("復元を実行")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun SettingItemWithSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingItemWithArrow(title: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AchievementSection(viewModel: GameViewModel, isReduceMotion: Boolean) {
    val todayRate by viewModel.todayAchievement.collectAsState(initial = 0.0)
    
    // 達成率のアニメーション
    val animatedRate by animateFloatAsState(
        targetValue = todayRate.toFloat(),
        animationSpec = if (isReduceMotion) snap() else tween(1000, easing = FastOutSlowInEasing),
        label = "achievementRate"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("今日の達成度", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = String.format(java.util.Locale.getDefault(), "%.0f%%", animatedRate * 100),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRate)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "※本日のタスクの完了率を表示しています。",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MasterDataManagementSection(
    title: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        // 追加フィールド
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("新規追加...", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            TextButton(onClick = {
                if (textInput.isNotBlank()) {
                    onAdd(textInput)
                    textInput = ""
                }
            }) {
                Text("追加", color = MaterialTheme.colorScheme.secondary)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // リスト
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = DarkRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
