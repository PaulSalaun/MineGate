package com.saunaltech.mindgate.app.ui.overlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saunaltech.mindgate.app.model.QuizState
import com.saunaltech.mindgate.app.service.AppBlockerService

// ─────────────────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────────────────

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val MgPrimaryDim = Color(0x264F46E5)
private val MgPrimaryBorder = Color(0x4D4F46E5)
private val ColorOk = Color(0xFF22C55E)
private val ColorErr = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

private val DiffColors = listOf(
    Color(0xFF22C55E), Color(0xFF84CC16),
    Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6)
)
private val DiffLabels = listOf("Novice", "Initié", "Moyen", "Avancé", "Expert")

private fun diffColor(d: Int) = DiffColors.getOrElse(d - 1) { MgPrimary }
private fun diffLabel(d: Int) = DiffLabels.getOrElse(d - 1) { "?" }

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OverlayScreen(packageName: String, onDismiss: () -> Unit, isFreeQuiz: Boolean = false) {
    val context = LocalContext.current
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory(context))
    val state by viewModel.state.collectAsState()
    var showSkipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadQuestions(packageName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .statusBarsPadding()   // évite que la topbar passe sous la status bar système
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar
            QuizTopBar(
                state = state,
                onClose = {
                    if (isFreeQuiz) {
                        // Quiz libre : simple retour au dashboard
                        onDismiss()
                    } else {
                        // Quiz de blocage : ferme l'overlay, tue l'app cible, retour accueil
                        onDismiss()
                        try {
                            val am =
                                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                            am.killBackgroundProcesses(packageName)
                        } catch (_: Exception) {
                        }
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                    }
                },
                onSkipRequest = { showSkipDialog = true }
            )

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 10 })
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "quiz_content"
            ) { s ->
                when (s) {
                    is QuizState.Loading -> LoadingContent()
                    is QuizState.NoQuestions -> NoQuestionsContent(packageName, onDismiss)
                    is QuizState.Question -> QuestionContent(s) { viewModel.answerQuestion(it) }
                    is QuizState.Feedback -> FeedbackContent(s) { viewModel.nextQuestion() }
                    is QuizState.Granted -> GrantedContent(
                        difficultyPlan = (state as? QuizState.Feedback)?.difficultyPlan
                            ?: emptyList(),
                        onDismiss = {
                            AppBlockerService.unlockedSessions.add(packageName)
                            onDismiss()
                        }
                    )
                }
            }
        }

        // ── Popup "passer le quiz" ────────────────────────────────────────────
        if (showSkipDialog) {
            SkipDialog(
                onConfirm = {
                    AppBlockerService.unlockedSessions.add(packageName)
                    showSkipDialog = false
                    onDismiss()
                },
                onDismiss = { showSkipDialog = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TopBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuizTopBar(
    state: QuizState,
    onClose: () -> Unit,
    onSkipRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Bouton fermer (rouge) — quitte l'app
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ColorErr.copy(alpha = 0.12f))
                    .border(0.5.dp, ColorErr.copy(alpha = 0.30f), CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClose
                    ),
                contentAlignment = Alignment.Center
            ) {
                // ✕
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(1.5.dp)
                            .background(ColorErr, RoundedCornerShape(1.dp))
                            .then(Modifier.padding(0.dp))
                    ) {}
                }
                // Dessiné via Text pour simplicité
                Text("✕", color = ColorErr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // Logo centré
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mind", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("Gate", color = MgPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }

            // Bouton passer (discret, flèche) — ouvre la popup
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onSkipRequest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("→", color = TextHint, fontSize = 13.sp)
            }
        }

        // Bulles de progression
        val (plan, completed, activeIdx) = when (state) {
            is QuizState.Question -> Triple(
                state.difficultyPlan,
                state.completedSlots,
                state.currentSlotIndex
            )

            is QuizState.Feedback -> Triple(
                state.difficultyPlan,
                state.completedSlots,
                state.currentSlotIndex
            )

            else -> Triple(emptyList(), emptyList(), 0)
        }
        if (plan.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text("PROGRESSION", color = TextHint, fontSize = 10.sp, letterSpacing = 0.05.sp)
            Spacer(modifier = Modifier.height(8.dp))
            DifficultyBubbles(plan, completed, activeIdx)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bulles de difficulté
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DifficultyBubbles(
    plan: List<Int>,
    completed: List<Boolean>,
    activeIdx: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        plan.forEachIndexed { i, diff ->
            val color = diffColor(diff)
            val isDone = completed.getOrElse(i) { false }
            val isActive = i == activeIdx && !isDone

            if (i > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            if (completed.getOrElse(i - 1) { false })
                                diffColor(plan[i - 1]).copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> color.copy(alpha = 0.18f)
                            isActive -> color.copy(alpha = 0.12f)
                            else -> Color.White.copy(alpha = 0.04f)
                        }
                    )
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = when {
                            isDone -> color.copy(alpha = 0.55f)
                            isActive -> color
                            else -> Color.White.copy(alpha = 0.14f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDone) "✓" else diff.toString(),
                    color = when {
                        isDone -> color
                        isActive -> color
                        else -> TextHint
                    },
                    fontSize = 10.sp,
                    fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Badge difficulté + meta (thème, id)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuestionMeta(question: com.saunaltech.mindgate.app.model.QuizQuestion) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Thème
        if (question.themeNom.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MgPrimaryDim)
                    .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(question.themeNom, color = MgPrimary.copy(alpha = 0.9f), fontSize = 10.sp)
            }
        }

        // ID
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text("#${question.id}", color = TextHint, fontSize = 10.sp)
        }

        // Difficulté
        val color = diffColor(question.difficulty)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.12f))
                .border(0.5.dp, color.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < question.difficulty) color
                                else Color.White.copy(alpha = 0.10f)
                            )
                    )
                }
            }
            Text(
                diffLabel(question.difficulty),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MgPrimary,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(12.dp))
            Text("Préparation du quiz…", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Aucune question
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoQuestionsContent(packageName: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Aucune question disponible", color = TextPrimary, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Synchronise les questions depuis l'app MindGate.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { AppBlockerService.unlockedSessions.add(packageName); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Accéder quand même", color = TextSecondary) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Question
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuestionContent(state: QuizState.Question, onAnswer: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        QuestionMeta(state.question)
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BgCard)
                .border(0.5.dp, BgBorder, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Text(state.question.enonce, color = TextPrimary, fontSize = 15.sp, lineHeight = 23.sp)
        }

        Spacer(Modifier.height(14.dp))

        val letters = listOf("A", "B", "C", "D", "E")
        state.question.reponses.forEachIndexed { i, rep ->
            AnswerBtn(letters.getOrElse(i) { "?" }, rep, AnswerState.Idle) { onAnswer(i) }
            if (i < state.question.reponses.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Feedback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackContent(state: QuizState.Feedback, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            QuestionMeta(state.question)
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BgCard)
                    .border(0.5.dp, BgBorder, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    state.question.enonce,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            val letters = listOf("A", "B", "C", "D", "E")
            state.question.reponses.forEachIndexed { i, rep ->
                val aState = when {
                    i == state.question.bonneReponse - 1 -> AnswerState.Correct
                    i == state.selectedIndex && !state.isCorrect -> AnswerState.Wrong
                    else -> AnswerState.Dimmed
                }
                AnswerBtn(
                    if (aState == AnswerState.Correct) "✓" else letters.getOrElse(i) { "?" },
                    rep,
                    aState
                ) {}
                if (i < state.question.reponses.lastIndex) Spacer(Modifier.height(8.dp))
            }

            if (state.question.explication.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MgPrimaryDim)
                        .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        state.question.explication,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        FeedbackBar(
            isCorrect = state.isCorrect,
            isGranted = state.isGranted,
            validatedCount = if (state.isCorrect) state.currentSlotIndex + 1 else 0,
            totalCount = state.difficultyPlan.size,
            onNext = onNext
        )
    }
}

@Composable
private fun FeedbackBar(
    isCorrect: Boolean,
    isGranted: Boolean,
    validatedCount: Int,
    totalCount: Int,
    onNext: () -> Unit
) {
    val bg = if (isCorrect) ColorOk.copy(0.09f) else ColorErr.copy(0.08f)
    val border = if (isCorrect) ColorOk.copy(0.18f) else ColorErr.copy(0.14f)
    val iconBg = if (isCorrect) ColorOk.copy(0.18f) else ColorErr.copy(0.16f)
    val iconColor = if (isCorrect) ColorOk else ColorErr
    val title = if (isCorrect) "Bonne réponse !" else "Mauvaise réponse"
    val subtitle = when {
        isGranted -> "Tous les niveaux validés !"
        isCorrect -> "$validatedCount / $totalCount validées"
        else -> "Série remise à zéro"
    }
    val btnBg = when {
        isGranted -> ColorOk; isCorrect -> MgPrimary; else -> Color.White.copy(0.10f)
    }
    val btnLabel = when {
        isGranted -> "Accéder à l'app"; isCorrect -> "Suivant"; else -> "Réessayer"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .border(width = 0.5.dp, color = border, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isCorrect) "✓" else "✕",
                color = iconColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = iconColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextHint, fontSize = 11.sp)
        }
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = btnBg),
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 6.dp
            )
        ) {
            Text(btnLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Granted
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GrantedContent(difficultyPlan: List<Int>, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(ColorOk.copy(0.13f))
                    .border(1.5.dp, ColorOk.copy(0.38f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = ColorOk, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            if (difficultyPlan.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    difficultyPlan.forEach { d ->
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(diffColor(d))
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            Text(
                "Accès accordé !",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tu as validé les ${difficultyPlan.size} niveaux.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MgPrimary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
            ) {
                Text("Continuer vers l'app", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Popup Skip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkipDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
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
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(BgCard)
                .border(
                    0.5.dp,
                    BgBorder,
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(20.dp)
        ) {
            // Icône
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MgPrimaryDim)
                    .border(0.5.dp, MgPrimaryBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("⏩", fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Passer le quiz ?",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tu peux accéder directement à l'app sans répondre aux questions. Cette action sera enregistrée.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MgPrimaryDim)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "Les Keys permettront de débloquer des passes sans répondre — bientôt disponible.",
                    color = MgPrimary.copy(0.80f),
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.07f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    Text("Annuler", color = TextSecondary, fontSize = 13.sp)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MgPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                ) {
                    Text("Accéder quand même", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bouton réponse
// ─────────────────────────────────────────────────────────────────────────────

private enum class AnswerState { Idle, Correct, Wrong, Dimmed }

@Composable
private fun AnswerBtn(letter: String, text: String, state: AnswerState, onClick: () -> Unit) {
    val bg = when (state) {
        AnswerState.Idle -> Color.White.copy(.06f); AnswerState.Correct -> ColorOk.copy(.11f); AnswerState.Wrong -> ColorErr.copy(
            .10f
        ); AnswerState.Dimmed -> Color.White.copy(.03f)
    }
    val border = when (state) {
        AnswerState.Idle -> Color.White.copy(.11f); AnswerState.Correct -> ColorOk.copy(.38f); AnswerState.Wrong -> ColorErr.copy(
            .35f
        ); AnswerState.Dimmed -> Color.White.copy(.05f)
    }
    val lBg = when (state) {
        AnswerState.Idle -> Color.White.copy(.07f); AnswerState.Correct -> ColorOk.copy(.22f); AnswerState.Wrong -> ColorErr.copy(
            .20f
        ); AnswerState.Dimmed -> Color.White.copy(.03f)
    }
    val lColor = when (state) {
        AnswerState.Idle -> Color.White.copy(.35f); AnswerState.Correct -> ColorOk; AnswerState.Wrong -> ColorErr; AnswerState.Dimmed -> Color.White.copy(
            .18f
        )
    }
    val tColor = when (state) {
        AnswerState.Dimmed -> Color.White.copy(.30f); else -> Color.White.copy(if (state == AnswerState.Idle) .82f else 1f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(13.dp))
            .clickable(
                enabled = state == AnswerState.Idle,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(lBg),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, color = lColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text,
            color = tColor,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}