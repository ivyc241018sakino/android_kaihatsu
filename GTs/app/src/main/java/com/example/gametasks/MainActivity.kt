package com.example.gametasks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gametasks.ui.theme.GameTasksTheme
import android.content.res.Configuration
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        gameViewModel.setNotificationsEnabled(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupDailyResetWork()
        checkNotificationPermission()

        setContent {
            val isDarkMode by gameViewModel.isDarkMode.collectAsStateWithLifecycle()
            val currentLanguage by gameViewModel.currentLanguage.collectAsStateWithLifecycle()

            val activity = this
            val context = remember(currentLanguage) {
                val locale = Locale(currentLanguage)
                Locale.setDefault(locale)
                val configuration = Configuration(resources.configuration)
                configuration.setLocale(locale)
                createConfigurationContext(configuration)
            }

            CompositionLocalProvider(
                LocalContext provides context,
                LocalActivityResultRegistryOwner provides activity,
                LocalOnBackPressedDispatcherOwner provides activity
            ) {
                GameTasksTheme(darkTheme = isDarkMode) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainAppScreen(
                            viewModel = gameViewModel,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            gameViewModel.refreshGameStatuses()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in refreshGameStatuses", e)
        }
        syncNotificationPermission()
    }

    private fun syncNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        
        val currentSetting = gameViewModel.notificationsEnabled.value
        
        // 状態が異なる場合のみ更新（軽量化）
        if (isGranted != currentSetting) {
            gameViewModel.setNotificationsEnabled(isGranted)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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

        val dailyResetRequest = PeriodicWorkRequestBuilder<ResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyResetWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyResetRequest
        )
    }
}

@Composable
fun MainAppScreen(
    viewModel: GameViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val gameList by viewModel.allGames.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        bottomBar = {
            if (currentRoute?.startsWith("add_game") == false) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.tab_all)) },
                        label = { Text(stringResource(R.string.tab_all)) },
                        selected = currentRoute == Screen.Home.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
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
                        icon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search)) },
                        label = { Text(stringResource(R.string.action_search)) },
                        selected = currentRoute == Screen.Search.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
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
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_settings)) },
                        label = { Text(stringResource(R.string.title_settings)) },
                        selected = currentRoute == Screen.Setting.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
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
                        viewModel.toggleCompletion(game.gameId, isChecked)
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.AddGame.createRoute())
                    },
                    onGameClick = { gameId ->
                        navController.navigate(Screen.AddGame.createRoute(gameId))
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onGameClick = { gameId ->
                        navController.navigate(Screen.AddGame.createRoute(gameId))
                    },
                    onCheckedChange = { game, isChecked ->
                        viewModel.toggleCompletion(game.gameId, isChecked)
                    }
                )
            }

            composable(Screen.Setting.route) {
                SettingScreen(
                    viewModel = viewModel,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }

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
                    },
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}
