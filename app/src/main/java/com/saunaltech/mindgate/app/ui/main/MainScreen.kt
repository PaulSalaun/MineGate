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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val BgGrid = Color(0xFF232235)
private val MgPrimary = Color(0xFF4F46E5)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

private val DiffColors = listOf(
    Color(0xFF22C55E),  // 1 Novice
    Color(0xFF84CC16),  // 2 Initié
    Color(0xFFF59E0B),  // 3 Moyen
    Color(0xFFEF4444),  // 4 Avancé
    Color(0xFF8B5CF6)   // 5 Expert
)
private val DiffLabels = listOf("Novice", "Initié", "Moyen", "Avancé", "Expert")

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(onFreeQuiz: () -> Unit) {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(context))
    val data by vm.dashboard.collectAsState()


    LaunchedEffect(Unit) { vm.loadDashboard() }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
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
        Button(
            onClick = onFreeQuiz,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MgPrimary),
            shape = RoundedCornerShape(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
        ) {
            Text("▶  Lancer un quiz libre", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(20.dp))

        // ── Stat cards ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${data.totalQuizzes}",
                label = "Quiz passés"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = if (data.totalQuizzes > 0) "${(data.accessRate * 100).toInt()}%" else "—",
                label = "Taux d'accès",
                valueColor = Color(0xFF22C55E)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = if (data.totalQuizzes > 0) "${(data.correctRate * 100).toInt()}%" else "—",
                label = "Réponses ok",
                valueColor = MgPrimary
            )
        }

        Spacer(Modifier.height(26.dp))

        // ── Graphique distribution par difficulté ─────────────────────────────
        SectionTitle(
            text = if (data.totalQuizzes == 0)
                "Questions disponibles par niveau"
            else
                "Questions disponibles par niveau",
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Basé sur la bibliothèque de questions synchronisée",
            color = TextHint,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(0.5.dp, BgBorder, RoundedCornerShape(16.dp))
                .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            if (data.isLoaded) {
                DifficultyBarChart(stats = data.diffStats)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chargement…", color = TextHint, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // ── Top thèmes ────────────────────────────────────────────────────────
        SectionTitle("Top thèmes", modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Thèmes avec le plus de questions disponibles",
            color = TextHint,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (!data.isLoaded) {
            // Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgCard)
            )
        } else if (data.topThemes.isEmpty()) {
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
                    "Aucun thème disponible.\nSynchronise les questions depuis Paramètres.",
                    color = TextHint,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
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
                        count = theme.questionCount
                    )
                }
                // Espaces vides si < 3
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
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(0.5.dp, BgBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = TextHint, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Graphique en barres — axe baseline fixe, barres qui montent
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyBarChart(stats: List<DiffStat>) {
    val chartHeight: Dp = 120.dp
    val barAreaHeight: Dp = 90.dp   // hauteur réelle des barres (hors labels)
    val gridColor = BgGrid
    val axisColor = BgBorder

    val maxCount = stats.maxOfOrNull { it.questionCount } ?: 1
    val gridLines = listOf(0.25f, 0.5f, 0.75f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {

        // Zone graphique : lignes de grille + barres
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeight)
                // Lignes de grille horizontales dessinées via drawBehind
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    gridLines.forEach { frac ->
                        val y = h * (1f - frac)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                    // Axe bas (baseline)
                    drawLine(
                        color = axisColor,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.5f
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom   // ← barres collées à la baseline
            ) {
                stats.forEachIndexed { idx, stat ->
                    val color = DiffColors.getOrElse(idx) { MgPrimary }
                    val fraction = if (maxCount > 0) stat.questionCount.toFloat() / maxCount else 0f
                    val barH = (fraction * barAreaHeight.value).coerceAtLeast(2f)

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Valeur numérique au sommet de la barre
                        if (stat.questionCount > 0) {
                            Text(
                                "${stat.questionCount}",
                                color = color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        // Barre — part du bas, monte vers le haut
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(barH.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color.copy(alpha = if (stat.questionCount > 0) 0.85f else 0.18f))
                        )
                    }
                }
            }
        }

        // Axe X : labels de difficulté alignés sous chaque barre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEachIndexed { idx, stat ->
                val color = DiffColors.getOrElse(idx) { MgPrimary }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${idx + 1}",
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        DiffLabels.getOrElse(idx) { "" }.take(3),
                        color = TextHint,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Podium thèmes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PodiumCard(modifier: Modifier, rank: Int, name: String, count: Int) {
    val (borderColor, bgColor, labelColor) = when (rank) {
        1 -> Triple(
            Color(0xFFFACC15).copy(alpha = 0.40f),
            Color(0xFFFACC15).copy(alpha = 0.06f),
            Color(0xFFFACC15)
        )

        2 -> Triple(
            Color(0xFFB0B0B0).copy(alpha = 0.35f),
            Color(0xFFB0B0B0).copy(alpha = 0.04f),
            Color(0xFFB0B0B0)
        )

        3 -> Triple(
            Color(0xFFCD7F32).copy(alpha = 0.35f),
            Color(0xFFCD7F32).copy(alpha = 0.04f),
            Color(0xFFCD7F32)
        )

        else -> Triple(BgBorder, BgCard, MgPrimary)
    }
    val rankEmoji = when (rank) {
        1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "$rank"
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
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2
        )
        Text(
            "$count q.",
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}