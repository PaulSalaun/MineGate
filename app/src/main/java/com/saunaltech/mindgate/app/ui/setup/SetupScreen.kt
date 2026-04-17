package com.saunaltech.mindgate.app.ui.setup

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import com.saunaltech.mindgate.app.model.QuizConfig
import com.saunaltech.mindgate.app.service.AppBlockerService
import com.saunaltech.mindgate.app.service.SyncWorker
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Couleurs MindGate
// ─────────────────────────────────────────────────────────────────────────────

private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryLight = Color(0xFFEEF2FF)
private val MgSecondary = Color(0xFFFACC15)
private val MgSecondaryDark = Color(0xFFD97706)

private val DifficultyColors = listOf(
    Color(0xFF22C55E), // 1 – Novice (vert)
    Color(0xFF84CC16), // 2 – Initié (vert-jaune)
    Color(0xFFF59E0B), // 3 – Intermédiaire (ambre)
    Color(0xFFEF4444), // 4 – Avancé (rouge-orange)
    Color(0xFF7C3AED), // 5 – Expert (violet)
)

// ─────────────────────────────────────────────────────────────────────────────
// Écran principal
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onGoToAppList: () -> Unit,
    onGoToSettings: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }
    val scope = rememberCoroutineScope()

    // ── Permissions ──────────────────────────────────────────────────────────
    var overlayGranted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var permsPanelOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
            // Auto-ferme le panel si tout est OK
            if (overlayGranted && accessibilityEnabled) permsPanelOpen = false
            kotlinx.coroutines.delay(1000)
        }
    }

    // ── Config du quiz ────────────────────────────────────────────────────────
    var quizConfig by remember { mutableStateOf(prefs.loadQuizConfig()) }
    var configDirty by remember { mutableStateOf(false) }
    var savedFeedback by remember { mutableStateOf(false) }

    val allGranted = overlayGranted && accessibilityEnabled

    // ─────────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("MindGate", fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    // Bouton permissions – discret, uniquement visible si besoin
                    PermissionsHeaderButton(
                        allGranted = allGranted,
                        isOpen = permsPanelOpen,
                        onClick = { permsPanelOpen = !permsPanelOpen }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {

            // ── Panel permissions (dépliable) ─────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = permsPanelOpen || !allGranted && !permsPanelOpen.not(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    PermissionsPanel(
                        overlayGranted = overlayGranted,
                        accessibilityEnabled = accessibilityEnabled,
                        context = context,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                // Bannière "setup requis" si permissions manquantes et panel fermé
                if (!allGranted && !permsPanelOpen) {
                    SetupBanner(onClick = { permsPanelOpen = true })
                }
            }

            // ── Résumé config ─────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                ConfigSummaryRow(
                    config = quizConfig,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // ── Configuration du quiz ─────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Configuration du quiz",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Définissez le nombre de questions et leur niveau de difficulté.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Préréglages
            item {
                PresetsRow(
                    currentConfig = quizConfig,
                    onSelect = { preset ->
                        quizConfig = preset
                        configDirty = true
                        savedFeedback = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Nombre de questions + slots de difficulté
            item {
                QuizConfigCard(
                    config = quizConfig,
                    onConfigChange = { newConfig ->
                        quizConfig = newConfig
                        configDirty = true
                        savedFeedback = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Légende des niveaux
            item {
                DifficultyLegend(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bouton Enregistrer
            item {
                Button(
                    onClick = {
                        scope.launch {
                            prefs.saveQuizConfig(quizConfig)
                            configDirty = false
                            savedFeedback = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = configDirty,
                    colors = ButtonDefaults.buttonColors(containerColor = MgPrimary)
                ) {
                    Text(if (savedFeedback) "✓ Configuration enregistrée" else "Enregistrer la configuration")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Autres actions ────────────────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onGoToAppList,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MgPrimary)
                    ) { Text("Apps à bloquer") }

                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MgPrimary)
                    ) { Text("Langue & thèmes") }

                    OutlinedButton(
                        onClick = onGoToDashboard,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dashboard") }

                    OutlinedButton(
                        onClick = { SyncWorker.syncNow(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Synchroniser les questions") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bouton permissions dans le header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionsHeaderButton(
    allGranted: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    val dotColor = if (allGranted) Color(0xFF22C55E) else Color(0xFFF59E0B)
    val label = if (allGranted) "Actif" else "Permissions"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                imageVector = if (isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bannière setup requis
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFFFFFBEB),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Des permissions sont requises pour activer MindGate.",
                fontSize = 13.sp,
                color = Color(0xFF92400E),
                modifier = Modifier.weight(1f)
            )
            Text(
                "Voir",
                fontSize = 12.sp,
                color = Color(0xFFD97706),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel permissions dépliable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionsPanel(
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column {
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionRow(
                label = "Service d'accessibilité",
                isGranted = accessibilityEnabled,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Résumé de la config
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfigSummaryRow(config: QuizConfig, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Questions",
            value = config.questionCount.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Difficulté moy.",
            value = "%.1f".format(config.averageDifficulty)
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Plage",
            value = if (config.minDifficulty == config.maxDifficulty)
                "${config.minDifficulty}"
            else "${config.minDifficulty}–${config.maxDifficulty}"
        )
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Préréglages
// ─────────────────────────────────────────────────────────────────────────────

private data class Preset(val label: String, val config: QuizConfig)

private val PRESETS = listOf(
    Preset("Équilibré", QuizConfig.PRESET_BALANCED),
    Preset("Progressif", QuizConfig.PRESET_PROGRESSIVE),
    Preset("Facile ×3", QuizConfig.PRESET_EASY),
    Preset("Expert", QuizConfig.PRESET_EXPERT),
)

@Composable
private fun PresetsRow(
    currentConfig: QuizConfig,
    onSelect: (QuizConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PRESETS.forEach { preset ->
            val isActive = currentConfig == preset.config
            FilterChip(
                selected = isActive,
                onClick = { onSelect(preset.config) },
                label = { Text(preset.label, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MgPrimaryLight,
                    selectedLabelColor = MgPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isActive,
                    selectedBorderColor = MgPrimary.copy(alpha = 0.5f),
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Carte de configuration : stepper + slots de difficulté
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizConfigCard(
    config: QuizConfig,
    onConfigChange: (QuizConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Stepper nombre de questions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Nombre de questions", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Entre 1 et 15", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CountStepper(
                    value = config.questionCount,
                    onDecrement = {
                        if (config.questionCount > 1) {
                            onConfigChange(QuizConfig(config.difficulties.dropLast(1)))
                        }
                    },
                    onIncrement = {
                        if (config.questionCount < 15) {
                            onConfigChange(QuizConfig(config.difficulties + 3))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Difficulté par question",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Appuyez sur un slot pour changer son niveau",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Grille de slots
            val columns = 5
            val rows = (config.difficulties.size + columns - 1) / columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(rows) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val startIdx = row * columns
                        val endIdx = minOf(startIdx + columns, config.difficulties.size)
                        for (i in startIdx until endIdx) {
                            DifficultySlot(
                                index = i,
                                difficulty = config.difficulties[i],
                                onDifficultyChange = { newDiff ->
                                    val updated = config.difficulties.toMutableList()
                                    updated[i] = newDiff
                                    onConfigChange(QuizConfig(updated))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Espaces vides pour compléter la dernière ligne
                        repeat(columns - (endIdx - startIdx)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stepper (+/-)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CountStepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slot de difficulté individuel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultySlot(
    index: Int,
    difficulty: Int,
    onDifficultyChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val diffColor = DifficultyColors.getOrElse(difficulty - 1) { MgPrimary }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    diffColor.copy(alpha = if (expanded) 0.8f else 0.3f),
                    RoundedCornerShape(10.dp)
                )
                .background(diffColor.copy(alpha = if (expanded) 0.12f else 0.06f))
                .clickable { expanded = !expanded }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Q${index + 1}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                difficulty.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = diffColor
            )
            // Indicateur étoiles
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { star ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (star < difficulty) diffColor
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }

        // Popup de sélection
        if (expanded) {
            DifficultyPopup(
                currentDifficulty = difficulty,
                onSelect = { newDiff ->
                    onDifficultyChange(newDiff)
                    expanded = false
                },
                onDismiss = { expanded = false }
            )
        }
    }
}

@Composable
private fun DifficultyPopup(
    currentDifficulty: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Overlay pour capturer les clics en dehors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    )
    Card(
        modifier = Modifier
            .width(180.dp)
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            (1..5).forEach { level ->
                val isSelected = level == currentDifficulty
                val levelColor = DifficultyColors.getOrElse(level - 1) { MgPrimary }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) levelColor.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(level) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mini étoiles
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { star ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (star < level) levelColor
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    Text(
                        "$level – ${QuizConfig.DIFFICULTY_LABELS[level]}",
                        fontSize = 12.sp,
                        color = if (isSelected) levelColor
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Légende des niveaux
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyLegend(modifier: Modifier = Modifier) {
    Text(
        "Niveaux de difficulté",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column {
            (1..5).forEach { level ->
                val levelColor = DifficultyColors.getOrElse(level - 1) { MgPrimary }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(levelColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            level.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = levelColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            QuizConfig.DIFFICULTY_LABELS[level] ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            QuizConfig.DIFFICULTY_DESCRIPTIONS[level] ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (level < 5) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ligne de permission (réutilisée dans le panel)
// ─────────────────────────────────────────────────────────────────────────────

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
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) Color(0xFF22C55E) else Color(0xFFF59E0B)
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        if (!isGranted) {
            Text(
                text = "Configurer →",
                color = MgPrimary,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                text = "Accordé",
                color = Color(0xFF16A34A),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Utilitaire accessibility
// ─────────────────────────────────────────────────────────────────────────────

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${AppBlockerService::class.java.canonicalName}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(service)
}