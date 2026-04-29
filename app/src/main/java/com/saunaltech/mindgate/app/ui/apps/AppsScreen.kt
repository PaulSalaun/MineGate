package com.saunaltech.mindgate.app.ui.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val BgSearch = Color(0xFF161525)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x1A4F46E5)
private val MgPrimaryBorder = Color(0x4D4F46E5)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Model
// ─────────────────────────────────────────────────────────────────────────────

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,       // icône réelle de l'application
    var isBlocked: Boolean
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }
    val scope = rememberCoroutineScope()

    var allApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    // Chargement initial
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val blockedPackages = prefs.loadBlockedApps()

            val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launchIntent, 0)

            allApps = resolveInfos
                .map { ri ->
                    AppItem(
                        packageName = ri.activityInfo.packageName,
                        label = ri.loadLabel(pm).toString(),
                        icon = try {
                            ri.loadIcon(pm)
                        } catch (_: Exception) {
                            null
                        },
                        isBlocked = ri.activityInfo.packageName in blockedPackages
                    )
                }
                .filter { it.packageName != context.packageName }
                .sortedWith(
                    compareByDescending<AppItem> { it.isBlocked }.thenBy { it.label.lowercase() }
                )

            isLoading = false
        }
    }

    // Toggle persisté
    fun toggle(packageName: String, newBlocked: Boolean) {
        allApps = allApps.map {
            if (it.packageName == packageName) it.copy(isBlocked = newBlocked) else it
        }
        scope.launch(Dispatchers.IO) {
            prefs.saveBlockedApps(allApps.filter { it.isBlocked }.map { it.packageName }.toSet())
        }
    }

    // Filtrage par recherche
    val filtered = remember(allApps, query) {
        if (query.isBlank()) allApps
        else allApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }
    }

    val blockedFiltered = filtered.filter { it.isBlocked }
    val otherFiltered = filtered.filter { !it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
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
                    "${allApps.count { it.isBlocked }} bloquée(s) · ${allApps.size} installées",
                    color = TextHint,
                    fontSize = 12.sp
                )
            }
        }

        // ── Barre de recherche ──────────────────────────────────────────────
        SearchBar(
            query = query,
            onQuery = { query = it },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── Liste ───────────────────────────────────────────────────────────
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MgPrimary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Chargement des apps…", color = TextHint, fontSize = 13.sp)
                    }
                }
            }

            filtered.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 36.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Aucun résultat pour « $query »",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Section bloquées
                    if (blockedFiltered.isNotEmpty()) {
                        item {
                            SectionLabel("Bloquées (${blockedFiltered.size})")
                            Spacer(Modifier.height(6.dp))
                        }
                        items(blockedFiltered, key = { it.packageName }) { app ->
                            AppRow(app = app, onToggle = { toggle(app.packageName, it) })
                        }
                    }

                    // Section autres apps
                    if (otherFiltered.isNotEmpty()) {
                        item {
                            if (blockedFiltered.isNotEmpty()) Spacer(Modifier.height(14.dp))
                            SectionLabel(
                                if (query.isBlank()) "Toutes les apps (${otherFiltered.size})"
                                else "Résultats (${otherFiltered.size})"
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        items(otherFiltered, key = { it.packageName }) { app ->
                            AppRow(app = app, onToggle = { toggle(app.packageName, it) })
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Barre de recherche
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        cursorBrush = SolidColor(MgPrimary),
        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgSearch)
                    .border(
                        0.5.dp,
                        if (query.isNotBlank()) MgPrimary.copy(0.30f) else BgBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔍", fontSize = 14.sp)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Rechercher une application…",
                            color = TextHint,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
                if (query.isNotBlank()) {
                    Text(
                        "✕",
                        color = TextHint,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onQuery("") }
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Label de section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = TextHint,
        fontSize = 10.sp,
        letterSpacing = 0.06.sp,
        fontWeight = FontWeight.Medium
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Ligne d'application
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppRow(app: AppItem, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BgCard)
            .border(
                0.5.dp,
                if (app.isBlocked) MgPrimary.copy(alpha = 0.30f) else BgBorder,
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Icône réelle de l'app ───────────────────────────────────────────
        AppIcon(drawable = app.icon, label = app.label, isBlocked = app.isBlocked)

        // ── Nom + package ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.label,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                app.packageName,
                color = TextHint,
                fontSize = 9.sp,
                maxLines = 1
            )
        }

        // ── Toggle bloquer ──────────────────────────────────────────────────
        BlockToggle(isBlocked = app.isBlocked, onToggle = onToggle)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icône d'application (Drawable → Bitmap → ImageBitmap)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIcon(drawable: Drawable?, label: String, isBlocked: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            // Convertit le Drawable en Bitmap puis en ImageBitmap pour Compose
            val bitmap = remember(drawable) {
                drawable.toBitmap(width = 80, height = 80).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay subtil si bloquée
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MgPrimary.copy(alpha = 0.18f))
                )
            }
        } else {
            // Fallback initiale si l'icône n'a pas pu être chargée
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isBlocked) MgPrimaryDim else Color.White.copy(0.06f)
                    )
                    .border(
                        0.5.dp,
                        if (isBlocked) MgPrimaryBorder else Color.White.copy(0.08f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label.take(1).uppercase(),
                    color = if (isBlocked) MgPrimary else TextHint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bouton bloquer / bloquée
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockToggle(isBlocked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isBlocked) MgPrimaryDim else Color.White.copy(0.06f)
            )
            .border(
                0.5.dp,
                if (isBlocked) MgPrimaryBorder else Color.White.copy(0.10f),
                RoundedCornerShape(8.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(!isBlocked) }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (isBlocked) "Bloquée" else "Bloquer",
            color = if (isBlocked) MgPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isBlocked) FontWeight.Medium else FontWeight.Normal
        )
    }
}