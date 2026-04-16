package com.saunaltech.mindgate.app.ui.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.QuizResultEntity
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import com.saunaltech.mindgate.app.data.repository.QuestionRepository
import com.saunaltech.mindgate.app.model.QuizQuestion
import com.saunaltech.mindgate.app.model.QuizState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuizViewModel(context: Context) : ViewModel() {

    private val repository = QuestionRepository(context)
    private val prefs = MindGatePreferences(context)
    private val quizResultDao = MindGateDatabase.getInstance(context).quizResultDao()

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state: StateFlow<QuizState> = _state

    // Pool de questions disponibles (on pioche dedans)
    private var questionPool = listOf<QuizQuestion>()

    // Questions déjà posées dans cette session (pour éviter les répétitions)
    private val askedIds = mutableSetOf<Long>()

    private var streak = 0
    private val required = 3       // configurable plus tard par app
    private var totalAnswered = 0
    private var totalCorrect = 0
    private var currentPackage = ""

    fun loadQuestions(packageName: String = "") {
        currentPackage = packageName
        viewModelScope.launch {
            val langue = prefs.loadLangue()
            val themeIds = prefs.loadActiveThemeIds()
            val entities = repository.getQuestionsForQuiz(langue, themeIds, limit = 50)

            if (entities.isEmpty()) {
                _state.value = QuizState.NoQuestions
                return@launch
            }

            questionPool = entities.map { entity ->
                val reponses: List<String> = Gson().fromJson(
                    entity.reponses,
                    object : TypeToken<List<String>>() {}.type
                )
                QuizQuestion(
                    id = entity.id,
                    enonce = entity.enonce,
                    reponses = reponses,
                    bonneReponse = entity.bonneReponse,
                    explication = entity.explication
                )
            }.shuffled()

            streak = 0
            totalAnswered = 0
            totalCorrect = 0
            askedIds.clear()

            showNextQuestion()
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val current = (state.value as? QuizState.Question)?.question ?: return
        val isCorrect = (selectedIndex + 1) == current.bonneReponse

        totalAnswered++
        if (isCorrect) {
            totalCorrect++
            streak++
        } else {
            streak = 0
        }

        val isGranted = streak >= required

        _state.value = QuizState.Feedback(
            question = current,
            selectedIndex = selectedIndex,
            isCorrect = isCorrect,
            streak = streak,
            required = required,
            isGranted = isGranted
        )

        if (isGranted) {
            saveResult(granted = true)
        }
    }

    fun nextQuestion() {
        val feedback = _state.value as? QuizState.Feedback ?: return

        if (feedback.isGranted) {
            _state.value = QuizState.Granted
            return
        }

        showNextQuestion()
    }

    private fun showNextQuestion() {
        // Prend une question pas encore posée
        val next = questionPool
            .filter { it.id !in askedIds }
            .randomOrNull()
            ?: run {
                // Toutes les questions ont été posées, on recharge le pool
                askedIds.clear()
                questionPool.randomOrNull()
            }

        if (next == null) {
            _state.value = QuizState.NoQuestions
            return
        }

        askedIds.add(next.id)

        _state.value = QuizState.Question(
            question = next,
            streak = streak,
            required = required
        )
    }

    private fun saveResult(granted: Boolean) {
        viewModelScope.launch {
            quizResultDao.insert(
                QuizResultEntity(
                    appPackage = currentPackage,
                    date = System.currentTimeMillis(),
                    totalQuestions = totalAnswered,
                    correctAnswers = totalCorrect,
                    accessGranted = granted
                )
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(context.applicationContext) as T
        }
    }
}