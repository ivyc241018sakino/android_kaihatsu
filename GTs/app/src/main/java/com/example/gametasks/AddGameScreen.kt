package com.example.gametasks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.gametasks.ui.FutureButton
import com.example.gametasks.ui.theme.PurpleButton
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddGameScreen(
    viewModel: GameViewModel,
    gameId: Int?,
    onSaved: () -> Unit
) {
    val genres by viewModel.genres.collectAsState(initial = emptyList())
    val platforms by viewModel.platforms.collectAsState(initial = emptyList())
    val isReduceMotion by viewModel.isReduceMotion.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.imageUriInput = it.toString()
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
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
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { launcher.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.imageUriInput != null) {
                        Image(
                            painter = rememberAsyncImagePainter(viewModel.imageUriInput),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("アイコンを選択", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            // ゲーム名
            Column {
                Text("ゲーム名", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.titleInput,
                    onValueChange = { viewModel.titleInput = it },
                    placeholder = { Text("(例) 原神", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // ジャンル選択
            Column {
                Text("ジャンル (複数選択可)", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        FilterChip(
                            selected = viewModel.selectedGenreIds.contains(genre.genreId),
                            onClick = {
                                viewModel.toggleGenreSelection(genre.genreId)
                            },
                            label = { Text(genre.genreName) },
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

            // プラットフォーム選択
            Column {
                Text("プラットフォーム (複数選択可)", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    platforms.forEach { platform ->
                        FilterChip(
                            selected = viewModel.selectedPlatformIds.contains(platform.platformId),
                            onClick = {
                                viewModel.togglePlatformSelection(platform.platformId)
                            },
                            label = { Text(platform.platformName) },
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

            // 更新時間
            var showTimePicker by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("更新時間", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d ⏰", viewModel.updateHourInput, viewModel.updateMinuteInput),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 18.sp
                )
            }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = viewModel.updateHourInput,
                    initialMinute = viewModel.updateMinuteInput,
                    is24Hour = true
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateHourInput = timePickerState.hour
                            viewModel.updateMinuteInput = timePickerState.minute
                            showTimePicker = false
                        }) {
                            Text("OK", color = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("キャンセル", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // リマインダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("リマインダー", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (viewModel.reminderEnabledInput) "ON" else "OFF", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = viewModel.reminderEnabledInput,
                        onCheckedChange = { viewModel.reminderEnabledInput = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // メモ
            Column {
                Text("メモ (任意)", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.memoInput,
                    onValueChange = { viewModel.memoInput = it },
                    placeholder = { Text("デイリー任務の内容など...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 保存ボタン
            FutureButton(
                onClick = { viewModel.saveGame(gameId) { onSaved() } },
                isReduceMotion = isReduceMotion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                containerColor = PurpleButton,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存する", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
