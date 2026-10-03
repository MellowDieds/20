package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.EyeCareViewModel
import com.example.ui.components.TopGentleAlertBanner
import com.example.ui.screens.AnimatedExerciseScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    DASHBOARD("Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home, "nav_dashboard"),
    EXERCISE("Egzersiz", Icons.Filled.PlayCircleOutline, Icons.Outlined.PlayCircleOutline, "nav_exercise"),
    HISTORY("Geçmiş", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
    SETTINGS("Ayarlar", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: EyeCareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto start service on launch if enabled
        val app = application as EyeCareApplication
        if (app.preferences.isServiceEnabled.value) {
            viewModel.startService()
        }

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val openExercise = intent.getBooleanExtra(EXTRA_OPEN_EXERCISE, false) ||
                intent.action == ACTION_OPEN_EXERCISE
        if (openExercise) {
            viewModel.triggerBreakNow()
            viewModel.consumeExerciseNavigation()
        }
    }

    companion object {
        const val ACTION_OPEN_EXERCISE = "com.aistudio.eyecare.ACTION_OPEN_EXERCISE"
        const val EXTRA_OPEN_EXERCISE = "extra_open_exercise"
    }
}

@Composable
fun MainContent(viewModel: EyeCareViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateToExercise by viewModel.navigateToExercise.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    // Listen to background service requests to navigate to exercise
    LaunchedEffect(navigateToExercise) {
        if (navigateToExercise) {
            currentScreen = AppScreen.EXERCISE
            viewModel.consumeExerciseNavigation()
        }
    }

    // Handle system back navigation to return to Dashboard
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.EXERCISE) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    AppScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(screen.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen content
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onStartService = { viewModel.startService() },
                        onStopService = { viewModel.stopService() },
                        onTriggerBreakNow = { viewModel.triggerBreakNow() },
                        onOpenExercise = { currentScreen = AppScreen.EXERCISE },
                        onResetTimer = { viewModel.resetTimer() },
                        onToggleTestMode = { viewModel.toggleTestMode(it) }
                    )
                }
                AppScreen.EXERCISE -> {
                    AnimatedExerciseScreen(
                        onComplete = { duration, routine ->
                            viewModel.completeBreak(duration, routine)
                            currentScreen = AppScreen.DASHBOARD
                        },
                        onClose = {
                            currentScreen = AppScreen.DASHBOARD
                        }
                    )
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(
                        uiState = uiState,
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        onSetIntervalMinutes = { viewModel.setIntervalMinutes(it) },
                        onSetSnoozeMinutes = { viewModel.setSnoozeMinutes(it) },
                        onSetMaxSnoozes = { viewModel.setMaxSnoozes(it) },
                        onToggleTestMode = { viewModel.toggleTestMode(it) },
                        onToggleSound = { viewModel.setSoundEnabled(it) },
                        onToggleResetOnScreenOff = { viewModel.setResetOnScreenOff(it) },
                        onCheckForUpdates = { viewModel.checkForUpdates() },
                        onDismissUpdateResult = { viewModel.dismissUpdateResult() },
                        onSetGithubRepo = { viewModel.setGithubRepo(it) }
                    )
                }
            }

            // Top Gentle In-App Alert Banner (appears gracefully when time is up)
            TopGentleAlertBanner(
                isVisible = uiState.isBreakAlertActive && currentScreen != AppScreen.EXERCISE,
                currentSnoozeCount = uiState.currentSnoozeCount,
                maxSnoozes = uiState.maxSnoozes,
                onSnooze = { viewModel.snoozeBreak() },
                onStartExercise = {
                    viewModel.dismissAlert()
                    currentScreen = AppScreen.EXERCISE
                },
                onDismiss = { viewModel.dismissAlert() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )
        }
    }
}
