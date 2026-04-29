package com.saunaltech.mindgate.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Nombre de questions disponibles par difficulté.
 * Affiché en attendant une vraie table quiz_answers.
 */
data class DiffStat(
    val difficulty: Int,
    val questionCount: Int   // nombre de questions dans la BDD pour ce niveau
)

/**
 * Top thème : basé sur le nombre de questions disponibles (données réelles).
 * Quand une table quiz_answers existera, on pourra y ajouter un taux de réussite.
 */
data class ThemeStat(
    val themeName: String,
    val questionCount: Int
)

data class DashboardData(
    val totalQuizzes: Int = 0,
    val accessRate: Float = 0f,   // % de quiz ayant accordé l'accès
    val correctRate: Float = 0f,   // % global de bonnes réponses (toutes sessions)
    val diffStats: List<DiffStat> = emptyList(),
    val topThemes: List<ThemeStat> = emptyList(),
    val isLoaded: Boolean = false
)

class MainViewModel(context: Context) : ViewModel() {

    private val db = MindGateDatabase.getInstance(context)
    private val quizResultDao = db.quizResultDao()
    private val questionDao = db.questionDao()
    private val themeDao = db.themeDao()

    private val _dashboard = MutableStateFlow(DashboardData())
    val dashboard: StateFlow<DashboardData> = _dashboard

    fun loadDashboard() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                // ── Stats globales (vraies, depuis quiz_results) ───────────────
                val totalQuizzes = quizResultDao.getTotalQuizzes()
                val totalGranted = quizResultDao.getTotalGranted()
                val totalCorrect = quizResultDao.getTotalCorrect() ?: 0
                val totalQuestions = quizResultDao.getTotalQuestions() ?: 0

                val accessRate = if (totalQuizzes > 0) totalGranted.toFloat() / totalQuizzes else 0f
                val correctRate =
                    if (totalQuestions > 0) totalCorrect.toFloat() / totalQuestions else 0f

                // ── Distribution par difficulté (données réelles de la BDD) ───
                // On affiche le nombre de questions disponibles par niveau.
                // Ces données sont vraies et reflètent le contenu synchronisé.
                val diffStats = (1..5).map { diff ->
                    DiffStat(
                        difficulty = diff,
                        questionCount = questionDao.getCountByDifficulty(diff)
                    )
                }

                // ── Top 3 thèmes par volume de questions (données réelles) ─────
                val themes = try {
                    themeDao.getAll()
                } catch (_: Exception) {
                    emptyList()
                }

                val topThemes = themes
                    .map { theme ->
                        ThemeStat(
                            themeName = theme.nom,
                            questionCount = questionDao.getCountByTheme(theme.id)
                        )
                    }
                    .filter { it.questionCount > 0 }
                    .sortedByDescending { it.questionCount }
                    .take(3)

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
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(ctx.applicationContext) as T
        }
    }
}