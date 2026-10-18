package com.example.expensesum.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensesum.ui.about.AboutScreen
import com.example.expensesum.ui.history.HistoryScreen
import com.example.expensesum.ui.home.HomeScreen
import com.example.expensesum.ui.settings.SettingsScreen
import com.example.expensesum.ui.theme.ExpenseSumTheme
import com.example.expensesum.util.HapticHelper
import kotlinx.coroutines.launch

enum class ExpenseSumRoute(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
) {
    SPLASH("splash", "Splash"),
    HOME("home", "Home", Icons.Default.Home),
    HISTORY("history", "History", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings),
    ABOUT("about", "About", Icons.Default.Info);

    companion object {
        val bottomNavItems = listOf(HOME, HISTORY, SETTINGS, ABOUT)
    }
}

@Composable
fun ExpenseSumApp(
    viewModel: ExpenseSumViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isConfirmationDialogActive = uiState.showClearAllConfirmation ||
            uiState.pendingCurrencyChange != null ||
            uiState.pendingHistoryDeleteId != null ||
            uiState.showClearHistoryConfirmation ||
            uiState.showResetSettingsConfirmation

    val showBottomBar = currentRoute in ExpenseSumRoute.bottomNavItems.map { it.route } &&
            !isConfirmationDialogActive

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    ExpenseSumTheme(
        themeMode = uiState.settings.themeMode,
        dynamicColor = uiState.settings.dynamicColor
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        ExpenseSumRoute.bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        HapticHelper.performLightClick(
                                            view,
                                            uiState.settings.hapticFeedback
                                        )
                                        navController.navigate(item.route) {
                                            popUpTo(ExpenseSumRoute.HOME.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    item.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ExpenseSumRoute.SPLASH.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ExpenseSumRoute.SPLASH.route) {
                    SplashScreen(
                        animationsEnabled = uiState.settings.animations,
                        onSplashFinished = {
                            navController.navigate(ExpenseSumRoute.HOME.route) {
                                popUpTo(ExpenseSumRoute.SPLASH.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(ExpenseSumRoute.HOME.route) {
                    HomeScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onShowSnackbar = showSnackbar
                    )
                }

                composable(ExpenseSumRoute.HISTORY.route) {
                    HistoryScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToHome = {
                            navController.navigate(ExpenseSumRoute.HOME.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onShowSnackbar = showSnackbar
                    )
                }

                composable(ExpenseSumRoute.SETTINGS.route) {
                    SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onShowSnackbar = showSnackbar
                    )
                }

                composable(ExpenseSumRoute.ABOUT.route) {
                    AboutScreen()
                }
            }
        }
    }
}
