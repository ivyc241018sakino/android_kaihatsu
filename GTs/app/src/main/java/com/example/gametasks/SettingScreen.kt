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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gametasks.ui.theme.DarkRed
import com.example.gametasks.ui.theme.GoldAccent
import com.example.gametasks.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: GameViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val genres by viewModel.genres.collectAsStateWithLifecycle(initialValue = emptyList())
    val platforms by viewModel.platforms.collectAsStateWithLifecycle(initialValue = emptyList())
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isReduceMotion by viewModel.isReduceMotion.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPermissionRequestDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold) },
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
                Text(stringResource(R.string.section_notification), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                
                SettingItemWithSwitch(stringResource(R.string.setting_notifications), notificationsEnabled) { checked ->
                    if (checked) {
                        // オフからオンにする場合、権限を確認
                        showPermissionRequestDialog = true
                    } else {
                        // オンからオフにする場合はそのまま
                        viewModel.setNotificationsEnabled(false)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                Text(stringResource(R.string.section_master_data), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            // ジャンル管理
            item {
                MasterDataManagementSection(
                    title = stringResource(R.string.label_manage_genres),
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
                    title = stringResource(R.string.label_manage_platforms),
                    items = platforms.map { it.platformName },
                    onAdd = { viewModel.addPlatform(it) },
                    onDelete = { name ->
                        platforms.find { it.platformName == name }?.let { viewModel.deletePlatform(it) }
                    }
                )
            }

            item {
                val languageDisplayName = when(currentLanguage) {
                    "en" -> "English"
                    "zh" -> "中文"
                    "ko" -> "한국어"
                    else -> "日本語"
                }
                SettingItemWithArrow(stringResource(R.string.setting_language), languageDisplayName) {
                    showLanguageDialog = true
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingItemWithSwitch(
                    title = stringResource(R.string.setting_dark_mode) + ": " + (if (isDarkMode) stringResource(R.string.label_on) else stringResource(R.string.label_off)),
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                SettingItemWithSwitch(
                    title = stringResource(R.string.setting_reduce_motion) + ": " + (if (isReduceMotion) stringResource(R.string.label_on) else stringResource(R.string.label_off)),
                    checked = isReduceMotion,
                    onCheckedChange = { viewModel.toggleReduceMotion(it) }
                )

                SettingItemWithArrow(stringResource(R.string.setting_backup_restore), "") {
                    showBackupDialog = true
                }
                
                SettingItemWithArrow(stringResource(R.string.setting_about), "") {
                    showAboutDialog = true
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPermissionRequestDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRequestDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRequestDialog = false
                    onRequestPermission()
                }) {
                    Text(stringResource(R.string.action_allow), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionRequestDialog = false
                    onOpenSettings()
                }) {
                    Text(stringResource(R.string.action_open_settings), color = MaterialTheme.colorScheme.secondary)
                }
            },
            title = { Text(stringResource(R.string.dialog_permission_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.dialog_permission_text))
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_close), color = MaterialTheme.colorScheme.primary)
                }
            },
            title = { Text(stringResource(R.string.setting_about), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.about_text),
                    lineHeight = 24.sp
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.setting_language), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    val languages = listOf(
                        "ja" to "日本語",
                        "en" to "English",
                        "zh" to "中文",
                        "ko" to "한국어"
                    )
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentLanguage == code),
                                onClick = {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_backup_restore), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(stringResource(R.string.backup_issue_code), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.generateBackupCode { code ->
                                backupCode = code
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_generate_code))
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
                                    onShowSnackbar(context.getString(R.string.toast_copied))
                                }) {
                                    Text(stringResource(R.string.action_copy))
                                }
                            }
                        )
                    }
                }

                HorizontalDivider()

                Column {
                    Text(stringResource(R.string.backup_restore_from_code), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        placeholder = { Text(stringResource(R.string.placeholder_paste_code)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.restoreFromCode(inputCode) { success ->
                                if (success) {
                                    onShowSnackbar(context.getString(R.string.toast_restore_complete))
                                    onDismiss()
                                } else {
                                    onShowSnackbar(context.getString(R.string.toast_invalid_code))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputCode.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.action_execute_restore))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
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
    val todayRate by viewModel.todayAchievement.collectAsStateWithLifecycle(initialValue = 0.0)
    
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
            Text(stringResource(R.string.label_today_achievement), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
            stringResource(R.string.achievement_note),
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
                placeholder = { Text(stringResource(R.string.placeholder_add_new), fontSize = 12.sp) },
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
                Text(stringResource(R.string.action_add), color = MaterialTheme.colorScheme.secondary)
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
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = DarkRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
