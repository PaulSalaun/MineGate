package com.saunaltech.mindgate.app.ui.setup

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.saunaltech.mindgate.app.service.AppBlockerService
import com.saunaltech.mindgate.app.service.SyncWorker

@Composable
fun SetupScreen(
    onGoToAppList: () -> Unit,
    onGoToSettings: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MindGate", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (overlayGranted && accessibilityEnabled) "Actif" else "Inactif",
            color = if (overlayGranted && accessibilityEnabled)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(40.dp))

        PermissionRow(
            label = "Affichage par-dessus les apps",
            isGranted = overlayGranted,
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionRow(
            label = "Service d'accessibilité",
            isGranted = accessibilityEnabled,
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onGoToAppList, modifier = Modifier.fillMaxWidth()) {
            Text("Apps à bloquer")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onGoToSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Paramètres (langue & thèmes)")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onGoToDashboard, modifier = Modifier.fillMaxWidth()) {
            Text("Dashboard")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { SyncWorker.syncNow(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Synchroniser les questions")
        }
    }
}

@Composable
fun PermissionRow(label: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (!isGranted) {
            Text(
                text = "Configurer",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${AppBlockerService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(service)
}