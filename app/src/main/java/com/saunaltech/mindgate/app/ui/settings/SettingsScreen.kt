package com.saunaltech.mindgate.app.ui.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.ThemeEntity
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import com.saunaltech.mindgate.app.service.SyncWorker
import com.saunaltech.mindgate.app.ui.setup.isAccessibilityServiceEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x1A4F46E5)
private val MgPrimaryBorder = Color(0x4D4F46E5)
private val ColorOk = Color(0xFF22C55E)
private val ColorWarn = Color(0xFFF59E0B)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }
    val db = remember { MindGateDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var currentLangue by remember { mutableStateOf(prefs.loadLangue()) }

    // ── Permissions ───────────────────────────────────────────────────────────
    var overlayGranted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    // ── Thèmes ────────────────────────────────────────────────────────────────
    var allThemes by remember { mutableStateOf<List<ThemeEntity>>(emptyList()) }
    var activeThemeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Se relance à chaque changement de langue — ne montre que les thèmes de la langue courante
    LaunchedEffect(currentLangue) {
        withContext(Dispatchers.IO) {
            // Filtre les thèmes qui ont au moins une question dans la langue courante
            val themesForLangue = try {
                val themeIdsWithQuestions =
                    db.questionDao().getThemeIdsByLangue(currentLangue).toSet()
                db.themeDao().getAll().filter { it.id in themeIdsWithQuestions }
            } catch (_: Exception) {
                emptyList()
            }

            allThemes = themesForLangue

            // Recharge les IDs actifs et les intersecte avec les thèmes disponibles en langue
            val saved = prefs.loadActiveThemeIds().toSet()
            val availableIds = themesForLangue.map { it.id }.toSet()
            activeThemeIds = when {
                availableIds.isEmpty() -> emptySet()
                saved.isEmpty() -> availableIds          // rien sauvegardé = tout actif
                else -> {
                    val inter = saved.intersect(availableIds)
                    // prefs d'une autre langue → tout actif
                    inter.ifEmpty { availableIds }
                }
            }
        }
    }

    // ── Langue ────────────────────────────────────────────────────────────────
    var showLangDrawer by remember { mutableStateOf(false) }
    var showThemeDrawer by remember { mutableStateOf(false) }

    // ── Config quiz ───────────────────────────────────────────────────────────
    val config = remember { prefs.loadQuizConfig() }

    val allGranted = overlayGranted && accessibilityEnabled

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDeep)
                .verticalScroll(rememberScrollState())
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
                        "Paramètres",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (allGranted) ColorOk else ColorWarn)
                        )
                        Text(
                            if (allGranted) "MindGate actif" else "Configuration requise",
                            color = if (allGranted) ColorOk else ColorWarn,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Bannière si permissions manquantes
            if (!allGranted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorWarn.copy(0.08f))
                        .border(0.5.dp, ColorWarn.copy(0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚠", fontSize = 14.sp, color = ColorWarn)
                    Text(
                        "Des permissions sont nécessaires pour que MindGate bloque les apps.",
                        color = ColorWarn.copy(0.85f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── PERMISSIONS ───────────────────────────────────────────────────
            SectionLabel("Permissions", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            SettingsCard(Modifier.padding(horizontal = 20.dp)) {
                PermissionRow(
                    label = "Affichage par-dessus les apps",
                    sublabel = "Requis pour afficher le quiz",
                    isGranted = overlayGranted,
                    onConfigure = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        )
                    }
                )
                SettingsDivider()
                PermissionRow(
                    label = "Service d'accessibilité",
                    sublabel = "Requis pour détecter l'ouverture des apps bloquées",
                    isGranted = accessibilityEnabled,
                    onConfigure = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── LANGUE ────────────────────────────────────────────────────────
            SectionLabel("Langue", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            SettingsCard(Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showLangDrawer = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drapeau
                    Text(
                        if (currentLangue == "FR") "🇫🇷" else "🇬🇧",
                        fontSize = 22.sp
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (currentLangue == "FR") "Français" else "English",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            "Questions dans cette langue",
                            color = TextHint,
                            fontSize = 10.sp
                        )
                    }
                    Text("Changer →", color = MgPrimary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── THÈMES ────────────────────────────────────────────────────────
            SectionLabel("Thèmes de questions", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            SettingsCard(Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showThemeDrawer = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview des thèmes actifs
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MgPrimaryDim)
                            .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("◈", color = MgPrimary, fontSize = 12.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Thèmes actifs", color = TextPrimary, fontSize = 13.sp)
                        Text(
                            when {
                                allThemes.isEmpty() -> "Synchronise les questions d'abord"
                                activeThemeIds.size == allThemes.size -> "Tous les thèmes activés"
                                activeThemeIds.isEmpty() -> "Aucun thème actif"
                                else -> "${activeThemeIds.size} / ${allThemes.size} thèmes actifs"
                            },
                            color = TextHint, fontSize = 10.sp
                        )
                    }
                    Text("Gérer →", color = MgPrimary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── QUIZ ──────────────────────────────────────────────────────────
            SectionLabel("Quiz", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            SettingsCard(Modifier.padding(horizontal = 20.dp)) {
                SettingsActionRow(
                    label = "Configuration du quiz",
                    sublabel = "${config.questionCount} questions · Diff. moy. ${
                        "%.1f".format(
                            config.averageDifficulty
                        )
                    }",
                    action = "Modifier"
                ) { /* TODO: naviguer vers config quiz */ }
            }

            Spacer(Modifier.height(24.dp))

            // ── DONNÉES ───────────────────────────────────────────────────────
            SectionLabel("Données", Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))

            SettingsCard(Modifier.padding(horizontal = 20.dp)) {
                SettingsActionRow(
                    label = "Synchroniser les questions",
                    sublabel = "Met à jour depuis Supabase",
                    action = "Sync"
                ) { SyncWorker.syncNow(context) }
            }

            Spacer(Modifier.height(40.dp))
        }

        // ── Drawer Langue ─────────────────────────────────────────────────────
        if (showLangDrawer) {
            LanguageDrawer(
                currentLangue = currentLangue,
                onSelect = { langue ->
                    currentLangue = langue
                    prefs.saveLangue(langue)
                    // Réinitialise les thèmes sauvegardés : la nouvelle langue
                    // aura ses propres thèmes, le LaunchedEffect(currentLangue)
                    // les rechargera et mettra tout actif par défaut.
                    prefs.saveActiveThemeIds(emptySet())
                    showLangDrawer = false
                },
                onDismiss = { showLangDrawer = false }
            )
        }

        // ── Drawer Thèmes ─────────────────────────────────────────────────────
        if (showThemeDrawer) {
            ThemeDrawer(
                allThemes = allThemes,
                activeThemeIds = activeThemeIds,
                onToggle = { themeId, checked ->
                    activeThemeIds = if (checked) activeThemeIds + themeId
                    else activeThemeIds - themeId
                    scope.launch(Dispatchers.IO) { prefs.saveActiveThemeIds(activeThemeIds) }
                },
                onToggleAll = { checked ->
                    val newIds = if (checked) allThemes.map { it.id }.toSet() else emptySet()
                    activeThemeIds = newIds
                    scope.launch(Dispatchers.IO) { prefs.saveActiveThemeIds(newIds) }
                },
                onDismiss = { showThemeDrawer = false }
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Theme Drawer (bottom sheet scrollable)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeDrawer(
    allThemes: List<ThemeEntity>,
    activeThemeIds: Set<Long>,
    onToggle: (Long, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val allActive = allThemes.isNotEmpty() && allThemes.all { it.id in activeThemeIds }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Limite la hauteur à 75% de l'écran pour laisser voir qu'on est dans un drawer
                .fillMaxSize(0.78f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BgCard)
                .border(0.5.dp, BgBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
        ) {
            // Poignée
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BgBorder)
            )

            // Header fixe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Thèmes de questions",
                        color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        when {
                            allThemes.isEmpty() -> "Aucun thème disponible"
                            allActive -> "Tous les thèmes activés"
                            activeThemeIds.isEmpty() -> "Aucun thème actif"
                            else -> "${activeThemeIds.size} / ${allThemes.size} actifs"
                        },
                        color = TextHint, fontSize = 11.sp
                    )
                }
                // Toggle global
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tout", color = TextSecondary, fontSize = 11.sp)
                    MgToggle(checked = allActive, onCheckedChange = onToggleAll)
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(BgBorder)
            )

            if (allThemes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Synchronise les questions depuis l'onglet Données pour voir les thèmes.",
                        color = TextHint, fontSize = 13.sp, lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Liste scrollable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    allThemes.forEachIndexed { idx, theme ->
                        val isActive = theme.id in activeThemeIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onToggle(theme.id, !isActive) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Icône thème
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isActive) MgPrimaryDim
                                        else Color.White.copy(0.05f)
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isActive) MgPrimaryBorder else BgBorder,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    theme.nom.take(1).uppercase(),
                                    color = if (isActive) MgPrimary else TextHint,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(Modifier.weight(1f)) {
                                Text(
                                    theme.nom,
                                    color = if (isActive) TextPrimary else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                                )
                                if (theme.description.isNotBlank()) {
                                    Text(
                                        theme.description,
                                        color = TextHint,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            MgToggle(
                                checked = isActive,
                                onCheckedChange = { onToggle(theme.id, it) })
                        }

                        if (idx < allThemes.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .padding(start = 70.dp)
                                    .background(BgBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Language Drawer (bottom sheet manuel)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageDrawer(
    currentLangue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val langs = listOf(
        Triple("FR", "Français", "🇫🇷"),
        Triple("EN", "English", "🇬🇧")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(BgCard)
                .border(
                    0.5.dp,
                    BgBorder,
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(bottom = 32.dp)
        ) {
            // Poignée
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp, bottom = 18.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BgBorder)
            )

            Text(
                "Langue des questions",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Text(
                "Les questions seront filtrées selon la langue choisie.",
                color = TextHint,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            langs.forEach { (code, label, flag) ->
                val isSelected = currentLangue == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MgPrimaryDim else Color.Transparent
                        )
                        .border(
                            0.5.dp,
                            if (isSelected) MgPrimaryBorder else BgBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSelect(code) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Drapeau grande taille
                    Text(flag, fontSize = 30.sp)

                    Column(Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (isSelected) MgPrimary else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                        Text(
                            code,
                            color = TextHint,
                            fontSize = 11.sp
                        )
                    }

                    // Indicateur sélection
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MgPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✓",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.dp, BgBorder, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toggle thème
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeToggleRow(
    theme: ThemeEntity,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle(!isActive) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Point coloré (actif = primary, inactif = gris)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) MgPrimary else BgBorder)
        )

        Column(Modifier.weight(1f)) {
            Text(
                theme.nom,
                color = if (isActive) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
            if (theme.description.isNotBlank()) {
                Text(
                    theme.description,
                    color = TextHint,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        MgToggle(checked = isActive, onCheckedChange = onToggle)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toggle custom (pill switch)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MgToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackColor = if (checked) MgPrimary else BgBorder
    val thumbColor = Color.White

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants partagés
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = TextHint,
        fontSize = 10.sp,
        letterSpacing = 0.06.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(0.5.dp, BgBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(BgBorder)
    )
}

@Composable
private fun PermissionRow(
    label: String,
    sublabel: String,
    isGranted: Boolean,
    onConfigure: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isGranted) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onConfigure
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isGranted) ColorOk else ColorWarn)
        )
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, color = TextHint, fontSize = 10.sp, lineHeight = 14.sp)
        }
        if (isGranted) {
            Text("Accordé", color = ColorOk, fontSize = 11.sp)
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(MgPrimaryDim)
                    .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(7.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text("Configurer →", color = MgPrimary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    label: String,
    sublabel: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, color = TextHint, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(MgPrimaryDim)
                .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(action, color = MgPrimary, fontSize = 11.sp)
        }
    }
}