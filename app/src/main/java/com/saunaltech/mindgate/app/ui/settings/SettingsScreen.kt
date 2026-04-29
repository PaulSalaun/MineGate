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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import com.saunaltech.mindgate.app.service.SyncWorker
import com.saunaltech.mindgate.app.ui.setup.isAccessibilityServiceEnabled

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x1A4F46E5)
private val MgPrimaryBorder = Color(0x4D4F46E5)
private val ColorOk = Color(0xFF22C55E)
private val ColorWarn = Color(0xFFF59E0B)
private val ColorErr = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }

    // Permissions — poolled chaque seconde
    var overlayGranted by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityEnabled = isAccessibilityServiceEnabled(context)
            kotlinx.coroutines.delay(1000)
        }
    }

    val allGranted = overlayGranted && accessibilityEnabled

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

        // ── Bannière si permissions manquantes ───────────────────────────────
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
                    "Des permissions sont nécessaires pour que MindGate puisse bloquer les apps et afficher le quiz.",
                    color = ColorWarn.copy(0.85f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Section Permissions ───────────────────────────────────────────────
        SectionLabel("Permissions", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Affichage par-dessus
            SettingsRow(
                label = "Affichage par-dessus les apps",
                sublabel = "Requis pour afficher le quiz lors de l'ouverture d'une app bloquée",
                isGranted = overlayGranted,
                onClick = if (!overlayGranted) {
                    {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        )
                    }
                } else null
            )

            SettingsDivider()

            // Accessibilité
            SettingsRow(
                label = "Service d'accessibilité",
                sublabel = "Requis pour détecter l'ouverture des apps bloquées",
                isGranted = accessibilityEnabled,
                onClick = if (!accessibilityEnabled) {
                    {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                } else null
            )
        }

        Spacer(Modifier.height(22.dp))

        // ── Section Quiz ──────────────────────────────────────────────────────
        SectionLabel("Quiz", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            val config = remember { prefs.loadQuizConfig() }

            SettingsActionRow(
                label = "Configuration du quiz",
                sublabel = "${config.questionCount} questions · Diff. moy. ${"%.1f".format(config.averageDifficulty)}",
                action = "Modifier"
            ) {
                // TODO: naviguer vers SetupScreen config section
            }

            SettingsDivider()

            SettingsActionRow(
                label = "Langue",
                sublabel = if (prefs.loadLangue() == "FR") "Français" else prefs.loadLangue(),
                action = "Changer"
            ) {
                // TODO: dialog langue
            }

            SettingsDivider()

            SettingsActionRow(
                label = "Thèmes actifs",
                sublabel = "Tous les thèmes",
                action = "Gérer"
            ) {
                // TODO: naviguer vers thèmes
            }
        }

        Spacer(Modifier.height(22.dp))

        // ── Section Données ───────────────────────────────────────────────────
        SectionLabel("Données", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))

        SettingsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingsActionRow(
                label = "Synchroniser les questions",
                sublabel = "Met à jour la base locale depuis Supabase",
                action = "Sync"
            ) {
                SyncWorker.syncNow(context)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants settings
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
private fun SettingsRow(
    label: String,
    sublabel: String,
    isGranted: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Point de statut
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isGranted) ColorOk else ColorWarn)
        )

        // Label
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(sublabel, color = TextHint, fontSize = 10.sp, lineHeight = 14.sp)
        }

        // Statut / action
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
        Column(modifier = Modifier.weight(1f)) {
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