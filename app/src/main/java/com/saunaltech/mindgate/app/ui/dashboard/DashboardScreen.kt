package com.saunaltech.mindgate.app.ui.dashboard

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.QuizResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { MindGateDatabase.getInstance(context).quizResultDao() }

    var results by remember { mutableStateOf<List<QuizResultEntity>>(emptyList()) }
    var totalQuizzes by remember { mutableIntStateOf(0) }
    var totalGranted by remember { mutableIntStateOf(0) }
    var totalCorrect by remember { mutableIntStateOf(0) }
    var totalQuestions by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            results = dao.getRecent()
            totalQuizzes = dao.getTotalQuizzes()
            totalGranted = dao.getTotalGranted()
            totalCorrect = dao.getTotalCorrect()
            totalQuestions = dao.getTotalQuestions()
        }
    }

    val successRate = if (totalQuizzes > 0) totalGranted * 100 / totalQuizzes else 0
    val correctRate = if (totalQuestions > 0) totalCorrect * 100 / totalQuestions else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Retour") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // --- Cartes de stats globales ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Vue globale", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Quiz passés",
                        value = totalQuizzes.toString()
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Taux d'accès",
                        value = "$successRate%"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Taux correct",
                        value = "$correctRate%"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Barre de progression globale
                Text(
                    "Réponses correctes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { correctRate / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                Text("Historique récent", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- Historique ---
            if (results.isEmpty()) {
                item {
                    Text(
                        "Aucun quiz effectué pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(results, key = { it.id }) { result ->
                    ResultRow(context = context, result = result)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultRow(context: Context, result: QuizResultEntity) {
    val appName = remember(result.appPackage) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(result.appPackage, 0)
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            result.appPackage
        }
    }

    val date = remember(result.date) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(result.date))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "${result.correctAnswers}/${result.totalQuestions}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Badge(
            containerColor = if (result.accessGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                if (result.accessGranted) "Accès" else "Refus",
                color = if (result.accessGranted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}