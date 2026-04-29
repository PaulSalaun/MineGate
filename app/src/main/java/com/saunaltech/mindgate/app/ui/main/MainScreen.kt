package com.saunaltech.mindgate.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saunaltech.mindgate.app.ui.overlay.OverlayScreen

// ─────────────────────────────────────────────────────────────────────────────
// Tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgCard2 = Color(0xFF161525)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x1A4F46E5)
private val MgSecondary = Color(0xFFFACC15)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

private val DiffColors = listOf(
    Color(0xFF22C55E), Color(0xFF84CC16),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6)
)
private val DiffLabels = listOf("Novice", "Initié", "Moyen", "Avancé", "Expert")

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(context))
    val data by vm.dashboard.collectAsState()
    var showFreeQuiz by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadDashboard() }

    if (showFreeQuiz) {
        // Quiz libre (sans package cible)
        OverlayScreen(packageName = "__free_quiz__", onDismiss = { showFreeQuiz = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mind",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Gate",
                        color = MgPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text("Tableau de bord", color = TextHint, fontSize = 12.sp)
            }
        }

        // ── Bouton quiz libre ─────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = { showFreeQuiz = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MgPrimary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) {
                Text("▶  Lancer un quiz libre", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Stat cards ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(Modifier.weight(1f), "${data.totalQuizzes}", "Quiz passés")
            StatCard(
                Modifier.weight(1f),
                "${(data.accessRate * 100).toInt()}%",
                "Taux d'accès",
                color = Color(0xFF22C55E)
            )
            StatCard(
                Modifier.weight(1f),
                "${(data.correctRate * 100).toInt()}%",
                "Réponses ok",
                color = MgPrimary
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Graphique en barres — réussite par difficulté ─────────────────────
        SectionTitle("Réussite par difficulté", modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(0.5.dp, BgBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                DifficultyBarChart(data.diffStats)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Novice", color = TextHint, fontSize = 9.sp)
                    Text("% de bonnes réponses estimé", color = TextHint, fontSize = 9.sp)
                    Text("Expert", color = TextHint, fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Podium des thèmes ─────────────────────────────────────────────────
        SectionTitle("Top thèmes", modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))

        if (data.topThemes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
                    .border(0.5.dp, BgBorder, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun thème disponible.\nSynchronise les questions depuis les paramètres.",
                    color = TextHint,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                data.topThemes.forEachIndexed { idx, theme ->
                    PodiumCard(
                        modifier = Modifier.weight(1f),
                        rank = idx + 1,
                        name = theme.themeName,
                        pct = (theme.successRate * 100).toInt()
                    )
                }
                // Remplir si < 3 thèmes
                repeat(3 - data.topThemes.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
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
private fun StatCard(modifier: Modifier, value: String, label: String, color: Color = TextPrimary) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = color)
        Spacer(Modifier.height(2.dp))
        Text(
            label, fontSize = 9.sp, color = TextHint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun DifficultyBarChart(stats: List<DiffStat>) {
    val maxBarHeightDp = 72.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeightDp + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEachIndexed { idx, stat ->
            val color = DiffColors.getOrElse(idx) { MgPrimary }
            val barFrac = stat.successRate.coerceIn(0f, 1f)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Pourcentage au-dessus
                Text(
                    "${(stat.successRate * 100).toInt()}%",
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 3.dp)
                )

                // Barre
                val barH = (barFrac * 60).coerceAtLeast(4f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barH.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(color.copy(alpha = 0.85f))
                )

                Spacer(Modifier.height(5.dp))

                // Label difficulté
                Text(
                    DiffLabels.getOrElse(idx) { "?" }.take(3),
                    color = TextHint,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun PodiumCard(modifier: Modifier, rank: Int, name: String, pct: Int) {
    val (borderColor, bgColor) = when (rank) {
        1 -> Color(0xFFFACC15).copy(0.40f) to Color(0xFFFACC15).copy(0.06f)
        2 -> Color(0xFFB0B0B0).copy(0.35f) to Color(0xFFB0B0B0).copy(0.04f)
        3 -> Color(0xFFCD7F32).copy(0.35f) to Color(0xFFCD7F32).copy(0.04f)
        else -> BgBorder to BgCard
    }
    val rankEmoji = when (rank) {
        1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank"
    }
    val pctColor = when (rank) {
        1 -> Color(0xFFFACC15)
        else -> MgPrimary
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(rankEmoji, fontSize = 18.sp)
        Text(
            name,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2
        )
        Text(
            "$pct%",
            color = pctColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}