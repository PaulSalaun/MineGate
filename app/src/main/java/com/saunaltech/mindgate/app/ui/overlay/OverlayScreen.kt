package com.saunaltech.mindgate.app.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saunaltech.mindgate.app.model.QuizState
import com.saunaltech.mindgate.app.service.AppBlockerService

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens MindGate
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1A1828)
private val BgCardBorder = Color(0xFF2A2840)

private val MgPrimary = Color(0xFF4F46E5)
private val MgSecondary = Color(0xFFFACC15)

private val ColorCorrect = Color(0xFF22C55E)
private val ColorWrong = Color(0xFFEF4444)
private val ColorSkip = Color(0xFF555566)

/** Couleur associée à chaque niveau de difficulté 1–5 */
private val DifficultyColors = listOf(
    Color(0xFF22C55E),  // 1 – Novice (vert)
    Color(0xFF84CC16),  // 2 – Initié (vert-jaune)
    Color(0xFFF59E0B),  // 3 – Moyen  (ambre)
    Color(0xFFEF4444),  // 4 – Avancé (rouge)
    Color(0xFF8B5CF6),  // 5 – Expert (violet)
)

private val DifficultyLabels = listOf("Novice", "Initié", "Moyen", "Avancé", "Expert")

private fun diffColor(level: Int): Color = DifficultyColors.getOrElse(level - 1) { MgPrimary }
private fun diffLabel(level: Int): String = DifficultyLabels.getOrElse(level - 1) { "?" }

// ─────────────────────────────────────────────────────────────────────────────
// Écran racine
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OverlayScreen(packageName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory(context))
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadQuestions(packageName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ───────────────────────────────────────────────────────
            TopBar(
                state = state,
                packageName = packageName,
                onSkip = {
                    AppBlockerService.unlockedSessions.add(packageName)
                    onDismiss()
                }
            )

            // ── Contenu principal ─────────────────────────────────────────────
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 8 })
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "quiz_state"
            ) { s ->
                when (s) {
                    is QuizState.Loading -> LoadingContent()
                    is QuizState.NoQuestions -> NoQuestionsContent(packageName, onDismiss)
                    is QuizState.Question -> QuestionContent(
                        state = s,
                        onAnswer = { viewModel.answerQuestion(it) }
                    )

                    is QuizState.Feedback -> FeedbackContent(
                        state = s,
                        onNext = { viewModel.nextQuestion() }
                    )

                    is QuizState.Granted -> GrantedContent(
                        packageName = packageName,
                        difficultyPlan = when (val prev = state) {
                            is QuizState.Feedback -> prev.difficultyPlan
                            else -> emptyList()
                        },
                        onDismiss = {
                            AppBlockerService.unlockedSessions.add(packageName)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar : logo + indicateur de progression + bouton passer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(state: QuizState, packageName: String, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mind", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text("Gate", color = MgPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            // Bouton passer (discret)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSkip() },
                contentAlignment = Alignment.Center
            ) {
                // Icône "skip" en SVG dessiné manuellement
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.5f.dp)
                            .background(ColorSkip, RoundedCornerShape(1.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.5f.dp)
                            .background(ColorSkip, RoundedCornerShape(1.dp))
                    )
                }
            }
        }

        // Indicateur de progression (bulles) — visible uniquement si quiz actif
        val plan = when (state) {
            is QuizState.Question -> state.difficultyPlan to state.completedSlots to state.currentSlotIndex
            is QuizState.Feedback -> state.difficultyPlan to state.completedSlots to state.currentSlotIndex
            else -> null
        }
        if (plan != null) {
            val (planPair, activeIdx) = plan
            val (diffPlan, completed) = planPair
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Progression",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                letterSpacing = 0.04.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            DifficultyBubbles(
                difficultyPlan = diffPlan,
                completedSlots = completed,
                currentSlotIndex = activeIdx
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bulles de difficulté
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyBubbles(
    difficultyPlan: List<Int>,
    completedSlots: List<Boolean>,
    currentSlotIndex: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        difficultyPlan.forEachIndexed { i, diff ->
            val color = diffColor(diff)
            val isDone = completedSlots.getOrElse(i) { false }
            val isActive = i == currentSlotIndex && !isDone

            // Connecteur entre bulles
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (completedSlots.getOrElse(i - 1) { false })
                                color.copy(alpha = 0.4f)
                            else
                                Color.White.copy(alpha = 0.10f)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> color.copy(alpha = 0.20f)
                            isActive -> color.copy(alpha = 0.15f)
                            else -> Color.White.copy(alpha = 0.04f)
                        }
                    )
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = when {
                            isDone -> color.copy(alpha = 0.6f)
                            isActive -> color
                            else -> Color.White.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    )
                    .then(
                        if (isActive) Modifier.border(
                            width = 3.dp,
                            color = color.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    // Checkmark
                    CheckmarkIcon(color = color)
                } else {
                    Text(
                        diff.toString(),
                        color = if (isActive) color else Color.White.copy(alpha = 0.30f),
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckmarkIcon(color: Color) {
    // Checkmark dessiné avec des Box (approximation simple)
    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Text("✓", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Badge de difficulté
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyBadge(difficulty: Int) {
    val color = diffColor(difficulty)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Petits points de difficulté
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < difficulty) color
                            else Color.White.copy(alpha = 0.12f)
                        )
                )
            }
        }
        Text(
            diffLabel(difficulty),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MgPrimary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("Préparation du quiz…", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Aucune question
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoQuestionsContent(packageName: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Aucune question disponible", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Synchronise les questions depuis l'app MindGate.",
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    AppBlockerService.unlockedSessions.add(packageName)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Accéder quand même", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Question
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuestionContent(
    state: QuizState.Question,
    onAnswer: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Badge de difficulté
        DifficultyBadge(difficulty = state.question.difficulty)
        Spacer(modifier = Modifier.height(16.dp))

        // Énoncé
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(0.5.dp, BgCardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = state.question.enonce,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Réponses
        val letters = listOf("A", "B", "C", "D", "E")
        state.question.reponses.forEachIndexed { index, reponse ->
            AnswerButton(
                letter = letters.getOrElse(index) { "?" },
                text = reponse,
                state = AnswerState.Idle,
                onClick = { onAnswer(index) }
            )
            if (index < state.question.reponses.lastIndex) {
                Spacer(modifier = Modifier.height(9.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feedback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackContent(
    state: QuizState.Feedback,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Badge de difficulté
            DifficultyBadge(difficulty = state.question.difficulty)
            Spacer(modifier = Modifier.height(16.dp))

            // Énoncé
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .border(0.5.dp, BgCardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    state.question.enonce,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Réponses colorées
            val letters = listOf("A", "B", "C", "D", "E")
            state.question.reponses.forEachIndexed { index, reponse ->
                val answerState = when {
                    index == state.question.bonneReponse - 1 -> AnswerState.Correct
                    index == state.selectedIndex && !state.isCorrect -> AnswerState.Wrong
                    else -> AnswerState.Dimmed
                }
                AnswerButton(
                    letter = if (answerState == AnswerState.Correct) "✓" else letters.getOrElse(
                        index
                    ) { "?" },
                    text = reponse,
                    state = answerState,
                    onClick = {}
                )
                if (index < state.question.reponses.lastIndex) {
                    Spacer(modifier = Modifier.height(9.dp))
                }
            }

            // Explication
            if (state.question.explication.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MgPrimary.copy(alpha = 0.10f))
                        .border(0.5.dp, MgPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = state.question.explication,
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Barre de feedback bas de page
        FeedbackBottomBar(
            isCorrect = state.isCorrect,
            isGranted = state.isGranted,
            currentSlotIndex = state.currentSlotIndex,
            totalSlots = state.difficultyPlan.size,
            onNext = onNext
        )
    }
}

@Composable
private fun FeedbackBottomBar(
    isCorrect: Boolean,
    isGranted: Boolean,
    currentSlotIndex: Int,
    totalSlots: Int,
    onNext: () -> Unit
) {
    val bgColor =
        if (isCorrect) ColorCorrect.copy(alpha = 0.10f) else ColorWrong.copy(alpha = 0.08f)
    val borderColor =
        if (isCorrect) ColorCorrect.copy(alpha = 0.20f) else ColorWrong.copy(alpha = 0.15f)
    val iconColor = if (isCorrect) ColorCorrect else ColorWrong
    val title = if (isCorrect) "Bonne réponse !" else "Mauvaise réponse"
    val subtitle = when {
        isGranted -> "Tous les niveaux validés !"
        isCorrect -> "${currentSlotIndex + 1}/$totalSlots validées"
        else -> "Série remise à zéro"
    }
    val btnColor = when {
        isGranted -> ColorCorrect
        isCorrect -> MgPrimary
        else -> Color.White.copy(alpha = 0.12f)
    }
    val btnLabel = when {
        isGranted -> "Accéder à l'app"
        isCorrect -> "Question suivante"
        else -> "Réessayer"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .border(
                width = 0.dp,
                color = Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icône résultat
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isCorrect) "✓" else "✕",
                color = iconColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = iconColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(alpha = 0.50f), fontSize = 12.sp)
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = btnColor),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp, vertical = 7.dp
            )
        ) {
            Text(btnLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bouton de réponse
// ─────────────────────────────────────────────────────────────────────────────

private enum class AnswerState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun AnswerButton(
    letter: String,
    text: String,
    state: AnswerState,
    onClick: () -> Unit
) {
    val bgColor = when (state) {
        AnswerState.Idle -> Color.White.copy(alpha = 0.06f)
        AnswerState.Correct -> ColorCorrect.copy(alpha = 0.12f)
        AnswerState.Wrong -> ColorWrong.copy(alpha = 0.12f)
        AnswerState.Dimmed -> Color.White.copy(alpha = 0.03f)
    }
    val borderColor = when (state) {
        AnswerState.Idle -> Color.White.copy(alpha = 0.12f)
        AnswerState.Correct -> ColorCorrect.copy(alpha = 0.40f)
        AnswerState.Wrong -> ColorWrong.copy(alpha = 0.40f)
        AnswerState.Dimmed -> Color.White.copy(alpha = 0.06f)
    }
    val letterBg = when (state) {
        AnswerState.Idle -> Color.White.copy(alpha = 0.08f)
        AnswerState.Correct -> ColorCorrect.copy(alpha = 0.25f)
        AnswerState.Wrong -> ColorWrong.copy(alpha = 0.22f)
        AnswerState.Dimmed -> Color.White.copy(alpha = 0.04f)
    }
    val letterColor = when (state) {
        AnswerState.Idle -> Color.White.copy(alpha = 0.40f)
        AnswerState.Correct -> ColorCorrect
        AnswerState.Wrong -> ColorWrong
        AnswerState.Dimmed -> Color.White.copy(alpha = 0.20f)
    }
    val textColor = when (state) {
        AnswerState.Dimmed -> Color.White.copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = if (state == AnswerState.Idle) 0.85f else 1f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(
                enabled = state == AnswerState.Idle,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(letterBg),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, color = letterColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Écran Accordé
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GrantedContent(
    packageName: String,
    difficultyPlan: List<Int>,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icône succès
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ColorCorrect.copy(alpha = 0.14f))
                    .border(1.5.dp, ColorCorrect.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = ColorCorrect, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bulles de difficulté toutes validées
            if (difficultyPlan.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    difficultyPlan.forEach { diff ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(diffColor(diff))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                "Accès accordé !",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tu as validé les ${difficultyPlan.size} niveaux avec succès.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MgPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp)
            ) {
                Text("Continuer vers l'app", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}