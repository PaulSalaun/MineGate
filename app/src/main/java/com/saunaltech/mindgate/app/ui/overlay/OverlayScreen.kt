package com.saunaltech.mindgate.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) { viewModel.loadQuestions() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF01A1A2E))
    ) {
        // Croix en haut à droite
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
                tint = Color(0xFF888888)
            )
        }

        // Contenu centré
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is QuizState.Loading -> LoadingContent()
                is QuizState.NoQuestions -> NoQuestionsContent(onDismiss, packageName)
                is QuizState.Question -> QuestionContent(
                    state = s,
                    onAnswer = { viewModel.answerQuestion(it) }
                )

                is QuizState.Feedback -> FeedbackContent(
                    state = s,
                    onNext = { viewModel.nextQuestion() }
                )

                is QuizState.Granted -> GrantedContent(
                    state = s,
                    packageName = packageName,
                    onDismiss = onDismiss
                )

                is QuizState.Denied -> DeniedContent(
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Chargement...", color = Color.White)
    }
}

@Composable
private fun NoQuestionsContent(onDismiss: () -> Unit, packageName: String) {
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
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { state.current.toFloat() / state.total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF7C4DFF),
            trackColor = Color(0xFF3D3D5C)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("${state.current} / ${state.total}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))

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
        // Résultat de la réponse
        Text(
            text = if (state.isCorrect) "Bonne réponse !" else "Mauvaise réponse",
            color = if (state.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Rappel de la question
        Text(
            text = state.question.enonce,
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Réponses avec couleurs
        state.question.reponses.forEachIndexed { index, reponse ->
            val bgColor = when (index) {
                state.question.bonneReponse -> Color(0xFF2E7D32)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Explication
        if (state.question.explication.isNotBlank()) {
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

        // Message si dernière question et mauvaise réponse
        if (state.isLast && !state.isCorrect) {
            Text(
                text = "La dernière réponse doit être correcte pour accéder à l'app",
                color = Color(0xFFF44336),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isLast && !state.isCorrect)
                    Color(0xFF7C4DFF) else Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = when {
                    state.isLast && state.isCorrect -> "Accéder à l'app"
                    state.isLast && !state.isCorrect -> "Réessayer"
                    else -> "Question suivante"
                }
            )
        }
    }
}

@Composable
private fun GrantedContent(
    state: QuizState.Granted,
    packageName: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Accès accordé !",
            color = Color(0xFF4CAF50),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "${state.correctAnswers} / ${state.total} bonnes réponses",
            color = Color.White,
            fontSize = 18.sp
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

@Composable
private fun DeniedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Accès refusé",
            color = Color(0xFFF44336),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tu dois répondre correctement à la dernière question",
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Text("Réessayer")
        }
    }
}