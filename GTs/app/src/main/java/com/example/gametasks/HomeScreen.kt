package com.example.gametasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.gametasks.GameViewModel
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameWithDetails
import com.example.gametasks.ui.AnimatedText
import com.example.gametasks.ui.FutureButton
import com.example.gametasks.ui.theme.GoldAccent
import com.example.gametasks.ui.theme.PurpleButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    gameList: List<GameWithDetails>,
    onCheckedChange: (GameEntity, Boolean) -> Unit,
    onNavigateToRegister: () -> Unit,
    onGameClick: (Int) -> Unit,
    viewModel: GameViewModel // ViewModelを追加
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("すべて", "未完了", "完了")

    val searchQuery by viewModel.homeSearchQuery.collectAsState()
    val isReduceMotion by viewModel.isReduceMotion.collectAsState()

    // 複数選択モードの状態
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedGameIds by remember { mutableStateOf(setOf<Int>()) }

    val filteredList = remember(gameList, selectedTab, searchQuery) {
        val tabFiltered = when (selectedTab) {
            1 -> gameList.filter { !it.game.completed }
            2 -> gameList.filter { it.game.completed }
            else -> gameList
        }
        
        if (searchQuery.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter { item ->
                item.game.title.contains(searchQuery, ignoreCase = true) ||
                item.game.memo.contains(searchQuery, ignoreCase = true) ||
                item.genres.any { it.genreName.contains(searchQuery, ignoreCase = true) } ||
                item.platforms.any { it.platformName.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // 入場アニメーション用の状態
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSelectionMode) {
                            AnimatedText(
                                text = "${selectedGameIds.size} 件選択中",
                                isReduceMotion = isReduceMotion,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        } else {
                            AnimatedText(
                                text = "Daily Tasks",
                                isReduceMotion = isReduceMotion,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedGameIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "選択解除")
                            }
                        }
                    },
                    actions = {
                        if (isSelectionMode && selectedGameIds.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.deleteGames(selectedGameIds)
                                isSelectionMode = false
                                selectedGameIds = emptySet()
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // 簡易検索バー
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.homeSearchQuery.value = it },
                    placeholder = { Text("検索 (タイトル・ジャンル等)", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary
                    )
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
                                color = if (selectedTab == index) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedTab == index) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(40.dp)
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FutureButton(
                    onClick = onNavigateToRegister,
                    isReduceMotion = isReduceMotion,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ゲームを追加")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("タスクはありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredList.size, key = { filteredList[it].game.gameId }) { index ->
                    val item = filteredList[index]
                    val isSelected = selectedGameIds.contains(item.game.gameId)
                    
                    // 近未来的なアニメーション設定
                    val animDelay = if (isReduceMotion) 0 else index * 40
                    
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = if (isReduceMotion) snap() else tween(durationMillis = 600, delayMillis = animDelay, easing = LinearOutSlowInEasing),
                        label = "alpha"
                    )
                    
                    val scale by animateFloatAsState(
                        targetValue = if (visible) 1f else 0.85f,
                        animationSpec = if (isReduceMotion) snap() else tween(durationMillis = 500, delayMillis = animDelay, easing = FastOutSlowInEasing),
                        label = "scale"
                    )

                    val translateY by animateFloatAsState(
                        targetValue = if (visible) 0f else 40f,
                        animationSpec = if (isReduceMotion) snap() else tween(durationMillis = 600, delayMillis = animDelay, easing = LinearOutSlowInEasing),
                        label = "translateY"
                    )

                    GameTaskCard(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        isReduceMotion = isReduceMotion,
                        onCheckedChange = { isChecked ->
                            if (!isSelectionMode) {
                                onCheckedChange(item.game, isChecked)
                            }
                        },
                        modifier = Modifier
                            .graphicsLayer(
                                alpha = alpha, 
                                translationY = translateY,
                                scaleX = scale,
                                scaleY = scale
                            )
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedGameIds = if (isSelected) {
                                            selectedGameIds - item.game.gameId
                                        } else {
                                            selectedGameIds + item.game.gameId
                                        }
                                        if (selectedGameIds.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else {
                                        onGameClick(item.game.gameId)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedGameIds = setOf(item.game.gameId)
                                    }
                                }
                            )
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
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isReduceMotion: Boolean = false
) {
    val game = item.game

    // ネオン調のグロー効果アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
    val borderAlphaFinal = if (isReduceMotion) 0.8f else glowAlpha

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.shadowElevation = if (isReduceMotion) 4f else 8f * glowAlpha
                this.shape = RoundedCornerShape(12.dp)
                this.clip = true
            },
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 12.dp else 4.dp,
        border = BorderStroke(2.dp, borderColor.copy(alpha = borderAlphaFinal))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ゲームアイコン
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (game.imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(game.imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        game.title.take(1),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.genres.joinToString(" / ") { it.genreName },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.platforms.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[${item.platforms.joinToString(", ") { it.platformName }}]",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${String.format(java.util.Locale.getDefault(), "%02d:%02d", game.updateHour, game.updateMinute)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // カスタムチェックボックス（丸いデザイン）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (game.completed) MaterialTheme.colorScheme.secondary else Color.Transparent)
                    .border(2.dp, if (game.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onCheckedChange(!game.completed) },
                contentAlignment = Alignment.Center
            ) {
                if (game.completed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}
