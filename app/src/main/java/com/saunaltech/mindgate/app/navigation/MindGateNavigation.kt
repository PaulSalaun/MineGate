package com.saunaltech.mindgate.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saunaltech.mindgate.app.ui.apps.AppsScreen
import com.saunaltech.mindgate.app.ui.history.HistoryScreen
import com.saunaltech.mindgate.app.ui.main.MainScreen
import com.saunaltech.mindgate.app.ui.overlay.OverlayScreen
import com.saunaltech.mindgate.app.ui.settings.SettingsScreen

private val BgNav = Color(0xFF161525)
private val BgDeep = Color(0xFF0F0E1A)
private val MgPrimary = Color(0xFF4F46E5)
private val TextHint = Color(0x59FFFFFF)

private data class Tab(val label: String, val icon: String)

private val TABS = listOf(
    Tab("Accueil", "⊞"),
    Tab("Apps", "⌂"),
    Tab("Historique", "◷"),
    Tab("Paramètres", "◉")
)

@Composable
fun MindGateNavigation() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    // showFreeQuiz géré ICI — au-dessus du Scaffold pour prendre tout l'écran
    var showFreeQuiz by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = BgDeep,
            bottomBar = {
                NavigationBar(
                    containerColor = BgNav,
                    tonalElevation = 0.dp
                ) {
                    TABS.forEachIndexed { index, tab ->
                        val selected = selectedIndex == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedIndex = index },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    color = if (selected) MgPrimary else TextHint
                                )
                            },
                            icon = {
                                Text(
                                    text = tab.icon,
                                    color = if (selected) MgPrimary else TextHint,
                                    fontSize = 17.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MgPrimary,
                                unselectedIconColor = TextHint,
                                indicatorColor = MgPrimary.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedIndex) {
                    0 -> MainScreen(onFreeQuiz = { showFreeQuiz = true })
                    1 -> AppsScreen()
                    2 -> HistoryScreen()
                    3 -> SettingsScreen()
                    else -> MainScreen(onFreeQuiz = { showFreeQuiz = true })
                }
            }
        }

        // OverlayScreen rendu AU-DESSUS du Scaffold — fillMaxSize non contraint
        if (showFreeQuiz) {
            OverlayScreen(
                packageName = "__free_quiz__",
                onDismiss = { showFreeQuiz = false },
                isFreeQuiz = true   // bouton fermer = simple pop, pas de kill/homeIntent
            )
        }
    }
}