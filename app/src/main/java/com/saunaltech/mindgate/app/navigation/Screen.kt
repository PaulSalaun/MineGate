package com.saunaltech.mindgate.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saunaltech.mindgate.app.ui.applist.AppListScreen
import com.saunaltech.mindgate.app.ui.setup.SetupScreen

sealed class Screen {
    object Setup : Screen()
    object AppList : Screen()
}

@Composable
fun MindGateNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Setup) }

    when (currentScreen) {
        Screen.Setup -> SetupScreen(
            onGoToAppList = { currentScreen = Screen.AppList }
        )

        Screen.AppList -> AppListScreen(
            onBack = { currentScreen = Screen.Setup }
        )
    }
}