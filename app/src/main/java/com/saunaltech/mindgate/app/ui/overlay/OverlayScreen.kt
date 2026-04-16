package com.saunaltech.mindgate.app.ui.overlay

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun OverlayScreen(packageName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory(context))
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadQuestions(packageName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF01A1A2E))
    ) {
        // Croix — passer le quiz
        IconButton(
            onClick = {
                AppBlockerService.unlockedSessions.add(packageName)
                onDismiss()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Passer",
                tint = Color(0xFF555555)
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = state) {
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
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// --- Loading ---

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chargement...", color = Color.White)
    }
}

// --- Pas de questions ---

@Composable
private fun NoQuestionsContent(packageName: String, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text("Aucune question disponible", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Synchronise les questions depuis l'app MindGate",
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            AppBlockerService.unlockedSessions.add(packageName)
            onDismiss()
        }) {
            Text("Accéder quand même")
        }
    }
}

// --- Question ---

@Composable
private fun QuestionContent(
    state: QuizState.Question,
    onAnswer: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MindGate", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Indicateur de streak
        StreakIndicator(streak = state.streak, required = state.required)

        Spacer(modifier = Modifier.height(24.dp))

        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A4A))
        ) {
            Text(
                text = state.question.enonce,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Réponses
        state.question.reponses.forEachIndexed { index, reponse ->
            Button(
                onClick = { onAnswer(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A4A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(reponse, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// --- Feedback ---

@Composable
private fun FeedbackContent(
    state: QuizState.Feedback,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.isCorrect) "Bonne réponse !" else "Mauvaise réponse",
            color = if (state.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Streak mis à jour
        StreakIndicator(streak = state.streak, required = state.required)

        if (!state.isCorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Série remise à zéro",
                color = Color(0xFFF44336),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Réponses colorées
        state.question.reponses.forEachIndexed { index, reponse ->
            val bgColor = when (index) {
                state.question.bonneReponse - 1 -> Color(0xFF2E7D32)
                state.selectedIndex -> if (!state.isCorrect) Color(0xFFC62828) else Color(0xFF2A2A4A)

                else -> Color(0xFF2A2A4A)
            }
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = bgColor,
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(reponse, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        // Explication
        if (state.question.explication.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F))
            ) {
                Text(
                    text = state.question.explication,
                    color = Color(0xFFBBDEFB),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isGranted) Color(0xFF4CAF50)
                else Color(0xFF7C4DFF)
            )
        ) {
            Text(if (state.isGranted) "Accéder à l'app" else "Question suivante")
        }
    }
}

// --- Accordé ---

@Composable
private fun GrantedContent(packageName: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Accès accordé !", color = Color(0xFF4CAF50), fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                AppBlockerService.unlockedSessions.add(packageName)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Continuer")
        }
    }
}

// --- Indicateur de streak ---

@Composable
private fun StreakIndicator(streak: Int, required: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$streak / $required bonnes réponses consécutives",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(required) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < streak) Color(0xFF4CAF50)
                            else Color(0xFF3D3D5C)
                        )
                )
            }
        }
    }
}