package com.saunaltech.mindgate.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saunaltech.mindgate.app.ui.applist.AppListScreen
import com.saunaltech.mindgate.app.ui.dashboard.DashboardScreen
import com.saunaltech.mindgate.app.ui.settings.SettingsScreen
import com.saunaltech.mindgate.app.ui.setup.SetupScreen

sealed class Screen {
    object Setup : Screen()
    object AppList : Screen()
    object Settings : Screen()
    object Dashboard : Screen()
}

@Composable
fun MindGateNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Setup) }

    when (currentScreen) {
        Screen.Setup -> SetupScreen(
            onGoToAppList = { currentScreen = Screen.AppList },
            onGoToSettings = { currentScreen = Screen.Settings },
            onGoToDashboard = { currentScreen = Screen.Dashboard }
        )

        Screen.AppList -> AppListScreen(
            onBack = { currentScreen = Screen.Setup }
        )

        Screen.Settings -> SettingsScreen(
            onBack = { currentScreen = Screen.Setup }
        )

        Screen.Dashboard -> DashboardScreen(
            onBack = { currentScreen = Screen.Setup }
        )
    }
}