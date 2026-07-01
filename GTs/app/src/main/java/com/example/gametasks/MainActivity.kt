package com.example.gametasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gametasks.data.GameEntity
import com.example.gametasks.ui.theme.GameTasksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameTasksTheme {
                val gameViewModel: GameViewModel = viewModel()
                MainAppScreen(viewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val gameList by viewModel.allGames.collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            // 💡 登録・詳細画面を開いているときはナビゲーションバーを隠す
            // 先頭部分が一致しているかで判定します
            if (currentRoute?.startsWith("add_game") == false) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "ホーム") },
                        label = { Text("ホーム") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            if (currentRoute != Screen.Home.route) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "検索") },
                        label = { Text("検索") },
                        selected = currentRoute == Screen.Search.route,
                        onClick = {
                            if (currentRoute != Screen.Search.route) {
                                navController.navigate(Screen.Search.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "設定") },
                        label = { Text("設定") },
                        selected = currentRoute == Screen.Setting.route,
                        onClick = {
                            if (currentRoute != Screen.Setting.route) {
                                navController.navigate(Screen.Setting.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    gameList = gameList,
                    onCheckedChange = { game, isChecked ->
                        val updatedGame = game.copy(completed = if (isChecked) 1 else 0)
                        viewModel.updateGame(updatedGame)
                    },
                    onNavigateToRegister = {
                        // ⭕️ 新規登録として画面を開く（引数なし）
                        navController.navigate(Screen.AddGame.createRoute())
                    },
                    onGameClick = { gameId ->
                        // ⭕️ 編集・詳細表示として画面を開く（ゲームIDを引数に渡す）
                        navController.navigate(Screen.AddGame.createRoute(gameId))
                    }
                )
            }

            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Setting.route) { SettingScreen() }

            // ⭕️ 引数（gameId）を受け取れるようにcomposableの設定を修正
            composable(
                route = Screen.AddGame.route,
                arguments = listOf(navArgument("gameId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val gameIdString = backStackEntry.arguments?.getString("gameId")
                val gameId = gameIdString?.toIntOrNull()

                AddGameScreen(
                    viewModel = viewModel,
                    gameId = gameId, // ⭕️ ここで gameId を AddGameScreen に渡します！
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}