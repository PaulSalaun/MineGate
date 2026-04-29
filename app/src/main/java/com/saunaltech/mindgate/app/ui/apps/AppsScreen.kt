package com.saunaltech.mindgate.app.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x1A4F46E5)
private val MgPrimaryBorder = Color(0x4D4F46E5)
private val ColorOk = Color(0xFF22C55E)
private val ColorErr = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

data class AppItem(
    val packageName: String,
    val label: String,
    var isBlocked: Boolean
)

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val blockedPackages = prefs.loadBlockedApps()

            // Récupère toutes les apps installées avec un launcher
            val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launchIntent, 0)

            apps = resolveInfos
                .map { ri ->
                    AppItem(
                        packageName = ri.activityInfo.packageName,
                        label = ri.loadLabel(pm).toString(),
                        isBlocked = ri.activityInfo.packageName in blockedPackages
                    )
                }
                .filter { it.packageName != context.packageName } // exclude self
                .sortedWith(compareByDescending<AppItem> { it.isBlocked }.thenBy { it.label })

            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Apps bloquées",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${apps.count { it.isBlocked }} app(s) bloquée(s)",
                    color = TextHint,
                    fontSize = 12.sp
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chargement…", color = TextHint, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    // Section bloquées
                    if (apps.any { it.isBlocked }) {
                        SectionChip("Bloquées")
                        Spacer(Modifier.height(6.dp))
                    }
                }

                items(apps.filter { it.isBlocked }, key = { it.packageName }) { app ->
                    AppRow(app) { newBlocked ->
                        apps = apps.map {
                            if (it.packageName == app.packageName) it.copy(isBlocked = newBlocked) else it
                        }
                        scope.launch(Dispatchers.IO) {
                            prefs.saveBlockedApps(apps.filter { it.isBlocked }
                                .map { it.packageName }.toSet())
                        }
                    }
                }

                item {
                    if (apps.any { !it.isBlocked }) {
                        Spacer(Modifier.height(14.dp))
                        SectionChip("Toutes les apps")
                        Spacer(Modifier.height(6.dp))
                    }
                }

                items(apps.filter { !it.isBlocked }, key = { it.packageName }) { app ->
                    AppRow(app) { newBlocked ->
                        apps = apps.map {
                            if (it.packageName == app.packageName) it.copy(isBlocked = newBlocked) else it
                        }
                        scope.launch(Dispatchers.IO) {
                            prefs.saveBlockedApps(apps.filter { it.isBlocked }
                                .map { it.packageName }.toSet())
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionChip(text: String) {
    Text(
        text.uppercase(),
        color = TextHint,
        fontSize = 10.sp,
        letterSpacing = 0.06.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun AppRow(app: AppItem, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BgCard)
            .border(
                0.5.dp,
                if (app.isBlocked) MgPrimary.copy(0.30f) else BgBorder,
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Initiales
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (app.isBlocked) MgPrimaryDim else Color.White.copy(0.06f)
                )
                .border(
                    0.5.dp,
                    if (app.isBlocked) MgPrimaryBorder else Color.White.copy(0.08f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                app.label.take(1).uppercase(),
                color = if (app.isBlocked) MgPrimary else TextHint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(1.dp))
            Text(app.packageName, color = TextHint, fontSize = 9.sp, maxLines = 1)
        }

        // Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (app.isBlocked) MgPrimaryDim else Color.White.copy(0.06f)
                )
                .border(
                    0.5.dp,
                    if (app.isBlocked) MgPrimaryBorder else Color.White.copy(0.10f),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(!app.isBlocked) }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                if (app.isBlocked) "Bloquée" else "Bloquer",
                color = if (app.isBlocked) MgPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (app.isBlocked) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}