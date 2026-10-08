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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gametasks.ui.FutureButton
import com.example.gametasks.ui.theme.PurpleButton
import androidx.annotation.RequiresApi
import com.example.gametasks.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddGameScreen(
    viewModel: GameViewModel,
    gameId: Int?,
    onSaved: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle(initialValue = emptyList())
    val platforms by viewModel.platforms.collectAsStateWithLifecycle(initialValue = emptyList())
    val isReduceMotion by viewModel.isReduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val installedApps = viewModel.installedApps
    val context = LocalContext.current

    var showPermissionRequestDialog by remember { mutableStateOf(false) }
    var showIconSourceDialog by remember { mutableStateOf(false) }
    var showAppIconPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // API 19以上必須だがminSdk 26なので安全
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.imageUriInput = it.toString()
            viewModel.packageNameInput = null
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
                title = { Text(if (gameId == null) stringResource(R.string.title_add_game) else stringResource(R.string.title_edit_game), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            // アイコン表示部分
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val iconModel = remember(viewModel.imageUriInput, viewModel.packageNameInput) {
                    viewModel.imageUriInput ?: viewModel.packageNameInput?.let { "package:$it" }
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showIconSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (iconModel != null) {
                        val request = remember(iconModel) {
                            coil.request.ImageRequest.Builder(context)
                                .data(iconModel)
                                .crossfade(false)
                                .allowHardware(false)
                                .build()
                        }
                        val painter = rememberAsyncImagePainter(model = request)
                        
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(if (iconModel.startsWith("package:")) 8.dp else 0.dp),
                            contentScale = ContentScale.Fit
                        )

                        // 読み込み中やエラー時のオーバーレイ
                        when (painter.state) {
                            is coil.compose.AsyncImagePainter.State.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            is coil.compose.AsyncImagePainter.State.Error -> {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            else -> {}
                        }
                    } else {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.label_select_icon), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            // ゲーム名入力
            Column {
                Text(stringResource(R.string.label_game_name), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.titleInput,
                    onValueChange = { viewModel.titleInput = it },
                    placeholder = { Text(stringResource(R.string.placeholder_game_name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // 連携アプリ選択 (プルダウン)
            Column {
                Text(stringResource(R.string.label_linked_app), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                val selectedApp by remember(viewModel.packageNameInput, installedApps) {
                    derivedStateOf {
                        installedApps.find { it.packageName == viewModel.packageNameInput }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedApp?.name ?: stringResource(R.string.no_app_selected),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        leadingIcon = selectedApp?.let {
                            {
                                val request = remember(it.iconUri) {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(it.iconUri)
                                        .size(64)
                                        .crossfade(false)
                                        .allowHardware(false)
                                        .build()
                                }
                                Image(
                                    painter = rememberAsyncImagePainter(model = request),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_app_selected)) },
                            onClick = {
                                viewModel.packageNameInput = null
                                expanded = false
                            }
                        )
                        installedApps.forEach { app ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val request = remember(app.iconUri) {
                                            coil.request.ImageRequest.Builder(context)
                                                .data(app.iconUri)
                                                .size(64)
                                                .crossfade(false)
                                                .allowHardware(false)
                                                .build()
                                        }
                                        Image(
                                            painter = rememberAsyncImagePainter(model = request),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(app.name, maxLines = 1)
                                    }
                                },
                                onClick = {
                                    viewModel.packageNameInput = app.packageName
                                    viewModel.imageUriInput = null
                                    if (viewModel.titleInput.isBlank()) {
                                        viewModel.titleInput = app.name
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ジャンル選択
            Column {
                Text(stringResource(R.string.label_genre_multiple), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.label_platform_multiple), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.label_update_time), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
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
                            Text(stringResource(R.string.action_ok), color = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.label_reminder), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (viewModel.reminderEnabledInput) stringResource(R.string.label_on) else stringResource(R.string.label_off), color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = viewModel.reminderEnabledInput,
                        onCheckedChange = { checked ->
                            if (checked && !notificationsEnabled) {
                                showPermissionRequestDialog = true
                            } else {
                                viewModel.reminderEnabledInput = checked
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // メモ
            Column {
                Text(stringResource(R.string.label_memo), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.memoInput,
                    onValueChange = { viewModel.memoInput = it },
                    placeholder = { Text(stringResource(R.string.placeholder_memo), color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                Text(stringResource(R.string.action_save), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // アイコンソース選択ダイアログ
    if (showIconSourceDialog) {
        AlertDialog(
            onDismissRequest = { showIconSourceDialog = false },
            title = { Text(stringResource(R.string.label_select_icon), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_from_gallery)) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            launcher.launch(arrayOf("image/*"))
                            showIconSourceDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_from_apps)) },
                        leadingContent = { Icon(Icons.Default.Apps, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAppIconPickerDialog = true
                            showIconSourceDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showIconSourceDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // アプリアイコン選択ダイアログ
    if (showAppIconPickerDialog) {
        var appSearchQuery by remember { mutableStateOf("") }
        val filteredApps = remember(appSearchQuery, installedApps) {
            if (appSearchQuery.isBlank()) installedApps
            else installedApps.filter { it.name.contains(appSearchQuery, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showAppIconPickerDialog = false },
            title = { Text(stringResource(R.string.label_select_app_icon), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(filteredApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.name) },
                                leadingContent = {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data("package:${app.packageName}")
                                                .size(64)
                                                .crossfade(false)
                                                .allowHardware(false)
                                                .build()
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.packageNameInput = app.packageName
                                    viewModel.imageUriInput = null
                                    if (viewModel.titleInput.isBlank()) {
                                        viewModel.titleInput = app.name
                                    }
                                    showAppIconPickerDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppIconPickerDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showPermissionRequestDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRequestDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onRequestPermission()
                    showPermissionRequestDialog = false
                }) {
                    Text(stringResource(R.string.action_allow), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onOpenSettings()
                    showPermissionRequestDialog = false
                }) {
                    Text(stringResource(R.string.action_open_settings), color = MaterialTheme.colorScheme.secondary)
                }
            },
            title = { Text(stringResource(R.string.dialog_permission_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.dialog_permission_text))
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
