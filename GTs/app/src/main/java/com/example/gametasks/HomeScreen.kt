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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RocketLaunch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gametasks.GameViewModel
import com.example.gametasks.data.GameEntity
import com.example.gametasks.data.GameWithDetails
import com.example.gametasks.ui.AnimatedText
import com.example.gametasks.ui.FutureButton
import com.example.gametasks.R
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
    val tabLabels = listOf(
        stringResource(R.string.tab_all),
        stringResource(R.string.tab_incomplete),
        stringResource(R.string.tab_complete)
    )

    val searchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle(initialValue = "")
    val isReduceMotion by viewModel.isReduceMotion.collectAsStateWithLifecycle(initialValue = false)
    val context = LocalContext.current

    // 複数選択モードの状態
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedGameIds by remember { mutableStateOf(setOf<Int>()) }

    val filteredList by remember(gameList, selectedTab, searchQuery) {
        derivedStateOf {
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
    }

    // 入場アニメーション用の状態
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    // スクロール状態の管理
    val listState = rememberLazyListState()
    val showFab by remember {
        derivedStateOf {
            // 一80%くらい一番上にいるとき、またはスクロールしていない時のみ表示
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSelectionMode) {
                            AnimatedText(
                                text = stringResource(R.string.selection_count, selectedGameIds.size),
                                isReduceMotion = isReduceMotion,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        } else {
                            AnimatedText(
                                text = stringResource(R.string.title_daily_tasks),
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
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_deselect))
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
                                    contentDescription = stringResource(R.string.action_delete),
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
                    placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 12.sp) },
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
            AnimatedVisibility(
                visible = showFab && !isSelectionMode,
                enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                exit = scaleOut(targetScale = 0.7f) + fadeOut()
            ) {
                FutureButton(
                    onClick = onNavigateToRegister,
                    isReduceMotion = isReduceMotion,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_game_content_desc))
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
                Text(
                    stringResource(R.string.no_tasks),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.game.gameId }
                ) { item ->
                    val index = filteredList.indexOf(item)
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
                        onLaunchApp = { packageName ->
                            viewModel.launchGameApp(context, packageName)
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
    onLaunchApp: (String) -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isReduceMotion: Boolean = false
) {
    val game = item.game

    // パフォーマンス向上のため、無限アニメーションを削除または簡略化
    // 選択時のみわずかに強調する程度に留める
    val borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.shadowElevation = if (isSelected) 8f else 2f
                this.shape = RoundedCornerShape(12.dp)
                this.clip = true
            },
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 8.dp else 2.dp,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
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
                val context = LocalContext.current
                // PackageManagerの呼び出しを最適化
                val iconModel = remember(game.imageUri, game.packageName) {
                    // imageUriがあればそれを優先、なければpackageNameを渡す（Fetcherが処理する）
                    game.imageUri ?: game.packageName?.let { "package:$it" }
                }
                
                if (iconModel != null) {
                    val request = remember(iconModel) {
                        coil.request.ImageRequest.Builder(context)
                            .data(iconModel)
                            .crossfade(false)
                            .allowHardware(false)
                            .build()
                    }
                    val painter = rememberAsyncImagePainter(model = request)
                    
                    val state = painter.state
                    if (state is coil.compose.AsyncImagePainter.State.Error) {
                        Text(
                            game.title.take(1),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
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

            // アプリ起動ボタン (packageNameがある場合のみ表示)
            if (game.packageName != null) {
                Surface(
                    onClick = { onLaunchApp(game.packageName) },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RocketLaunch,
                            contentDescription = stringResource(R.string.launch_app),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
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
