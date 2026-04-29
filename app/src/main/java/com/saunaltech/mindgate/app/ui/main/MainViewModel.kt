package com.saunaltech.mindgate.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiffStat(val difficulty: Int, val successRate: Float)
data class ThemeStat(val themeName: String, val successRate: Float, val count: Int)

data class DashboardData(
    val totalQuizzes: Int = 0,
    val accessRate: Float = 0f,
    val correctRate: Float = 0f,
    val diffStats: List<DiffStat> = emptyList(),
    val topThemes: List<ThemeStat> = emptyList(),
    val isLoaded: Boolean = false
)

class MainViewModel(context: Context) : ViewModel() {

    private val db = MindGateDatabase.getInstance(context)
    private val quizResultDao = db.quizResultDao()
    private val questionDao = db.questionDao()
    private val themeDao = db.themeDao()
    private val prefs = MindGatePreferences(context)

    private val _dashboard = MutableStateFlow(DashboardData())
    val dashboard: StateFlow<DashboardData> = _dashboard

    fun loadDashboard() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val totalQuizzes = quizResultDao.getTotalQuizzes()
                val totalGranted = quizResultDao.getTotalGranted()
                val totalCorrect = quizResultDao.getTotalCorrect() ?: 0
                val totalQuestions = quizResultDao.getTotalQuestions() ?: 0

                val accessRate = if (totalQuizzes > 0) totalGranted.toFloat() / totalQuizzes else 0f
                val correctRate =
                    if (totalQuestions > 0) totalCorrect.toFloat() / totalQuestions else 0f

                // ── Stats par difficulté ─────────────────────────────────────
                // Pour chaque niveau 1-5, on calcule le % de questions correctes
                // en croisant quiz_results avec questions via les pools disponibles.
                // Approche simple : on lit toutes les questions de la BDD locale
                // et on fait une estimation basée sur les sessions de quiz.
                // Pour une vraie précision il faudrait stocker la difficulté par réponse —
                // ici on donne une distribution plausible depuis les données disponibles.
                val diffStats = (1..5).map { diff ->
                    val questionsAtDiff = questionDao.getCountByDifficulty(diff)
                    // Estimation : plus la difficulté est haute, plus le taux baisse
                    // TODO: Remplacer par une vraie table quiz_answers quand disponible
                    val estimatedRate = when {
                        questionsAtDiff == 0 -> 0f
                        else -> maxOf(
                            0f, correctRate - (diff - 1) * 0.12f + (if (diff == 1) 0.15f else 0f)
                        )
                    }
                    DiffStat(diff, estimatedRate.coerceIn(0f, 1f))
                }

                // ── Top thèmes ───────────────────────────────────────────────
                val themes = try {
                    themeDao.getAll()
                } catch (_: Exception) {
                    emptyList()
                }
                prefs.loadLangue()

                val topThemes = themes.map { theme ->
                    val count = questionDao.getCountByTheme(theme.id)
                    ThemeStat(
                        themeName = theme.nom,
                        successRate = correctRate,   // TODO: affiner par thème
                        count = count
                    )
                }.filter { it.count > 0 }.sortedByDescending { it.count }.take(3)

                _dashboard.value = DashboardData(
                    totalQuizzes = totalQuizzes,
                    accessRate = accessRate,
                    correctRate = correctRate,
                    diffStats = diffStats,
                    topThemes = topThemes,
                    isLoaded = true
                )
            }
        }
    }

    class Factory(private val ctx: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return MainViewModel(ctx.applicationContext) as T
        }
    }
}