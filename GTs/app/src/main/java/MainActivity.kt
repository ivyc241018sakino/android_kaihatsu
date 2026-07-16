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
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ⭕️ エラー回避のためフルパスでのインポートを明示
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭕️ アプリ起動時に毎日リセットのスケジュールを登録
        setupDailyResetWork()

        setContent {
            GameTasksTheme {
                val gameViewModel: GameViewModel = viewModel()
                MainAppScreen(viewModel = gameViewModel)
            }
        }
    }

    // ⭕️ 毎日0:00に裏側でリセットを実行する関数（朝5時に変えたい場合は HOUR_OF_DAY を 5 に）
    private fun setupDailyResetWork() {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        // 24時間周期のタスクを作成
        val dailyResetRequest = PeriodicWorkRequestBuilder<ResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // 重複防止の設定でWorkManagerに登録
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyResetWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyResetRequest
        )
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
                        navController.navigate(Screen.AddGame.createRoute())
                    },
                    onGameClick = { gameId ->
                        navController.navigate(Screen.AddGame.createRoute(gameId))
                    }
                )
            }

            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Setting.route) { SettingScreen() }

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
                    gameId = gameId,
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}