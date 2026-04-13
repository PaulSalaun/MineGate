package com.saunaltech.mindgate.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saunaltech.mindgate.app.service.AppBlockerService

@Composable
fun OverlayScreen(packageName: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("MindGate", fontSize = 32.sp, color = Color.White)
            Text(
                text = "Tu veux ouvrir $packageName",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA)
            )
            Text("Quiz à venir ici...", fontSize = 14.sp, color = Color(0xFF888888))

            Button(onClick = {
                AppBlockerService.unlockedSessions.add(packageName)
                onDismiss()
            }) {
                Text("Laisser passer (test)")
            }
        }
    }
}