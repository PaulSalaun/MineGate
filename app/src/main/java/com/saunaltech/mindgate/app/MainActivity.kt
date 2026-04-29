package com.saunaltech.mindgate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.saunaltech.mindgate.app.navigation.MindGateNavigation
import com.saunaltech.mindgate.app.service.SyncWorker

private val MgDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2D2B6E),
    background = Color(0xFF0F0E1A),
    surface = Color(0xFF1E1D30),
    surfaceVariant = Color(0xFF252438),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0x99FFFFFF),
    outline = Color(0xFF2A2840),
    outlineVariant = Color(0xFF2A2840),
    error = Color(0xFFEF4444),
    onError = Color.White,
    secondary = Color(0xFFFACC15),
    onSecondary = Color(0xFF1A1500),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedule(this)
        setContent {
            MaterialTheme(colorScheme = MgDarkColorScheme) {
                MindGateNavigation()
            }
        }
    }
}