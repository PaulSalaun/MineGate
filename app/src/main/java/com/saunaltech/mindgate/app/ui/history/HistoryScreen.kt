package com.saunaltech.mindgate.app.ui.history

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.QuizResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgDeep = Color(0xFF0F0E1A)
private val BgCard = Color(0xFF1E1D30)
private val BgBorder = Color(0xFF2A2840)
private val MgPrimary = Color(0xFF4F46E5)
private val ColorOk = Color(0xFF22C55E)
private val ColorErr = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0x99FFFFFF)
private val TextHint = Color(0x59FFFFFF)

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dao = remember { MindGateDatabase.getInstance(context).quizResultDao() }
    var results by remember { mutableStateOf<List<QuizResultEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            results = dao.getAll()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Historique",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Text("${results.size} sessions enregistrées", color = TextHint, fontSize = 12.sp)
            }
        }

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("◷", fontSize = 40.sp, color = TextHint)
                    Spacer(Modifier.height(12.dp))
                    Text("Aucune session pour l'instant.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Lance un quiz pour commencer.", color = TextHint, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(0.dp)) }
                items(results, key = { it.id }) { result ->
                    HistoryRow(context = context, result = result)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRow(context: Context, result: QuizResultEntity) {
    val appName = remember(result.appPackage) {
        if (result.appPackage == "__free_quiz__") "Quiz libre" else {
            try {
                context.packageManager
                    .getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            result.appPackage,
                            0
                        )
                    )
                    .toString()
            } catch (_: PackageManager.NameNotFoundException) {
                result.appPackage.substringAfterLast('.')
            }
        }
    }

    val date = remember(result.date) {
        SimpleDateFormat("dd/MM/yy · HH:mm", Locale.getDefault()).format(Date(result.date))
    }

    val pct = if (result.totalQuestions > 0)
        (result.correctAnswers * 100 / result.totalQuestions) else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(0.5.dp, BgBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Indicateur accès
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (result.accessGranted) ColorOk.copy(0.12f)
                    else ColorErr.copy(0.10f)
                )
                .border(
                    0.5.dp,
                    if (result.accessGranted) ColorOk.copy(0.30f) else ColorErr.copy(0.25f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (result.accessGranted) "✓" else "✕",
                color = if (result.accessGranted) ColorOk else ColorErr,
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }

        // Infos
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(date, color = TextHint, fontSize = 10.sp)
        }

        // Score
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${result.correctAnswers}/${result.totalQuestions}",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$pct%",
                color = if (pct >= 60) ColorOk else ColorErr,
                fontSize = 10.sp
            )
        }
    }
}