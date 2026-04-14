package com.saunaltech.mindgate.app.ui.overlay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state: StateFlow<QuizState> = _state

    private var questions = listOf<QuizQuestion>()
    private var currentIndex = 0
    private var correctAnswers = 0

    fun loadQuestions() {
        viewModelScope.launch {
            val themes = prefs.loadActiveThemes()
            val entities = repository.getQuestionsForQuiz(themes)

            if (entities.isEmpty()) {
                _state.value = QuizState.NoQuestions
                return@launch
            }

            questions = entities
                .shuffled()
                .take(5)
                .map { entity ->
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
                }

            currentIndex = 0
            correctAnswers = 0
            showCurrentQuestion()
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val current = questions.getOrNull(currentIndex) ?: return
        val isCorrect = selectedIndex == current.bonneReponse
        val isLast = currentIndex == questions.size - 1

        if (isCorrect) correctAnswers++

        _state.value = QuizState.Feedback(
            question = current,
            selectedIndex = selectedIndex,
            isCorrect = isCorrect,
            current = currentIndex + 1,
            total = questions.size,
            isLast = isLast
        )
    }

    fun nextQuestion() {
        val feedback = _state.value as? QuizState.Feedback ?: return

        if (feedback.isLast) {
            // Accès accordé uniquement si la dernière réponse est correcte
            if (feedback.isCorrect) {
                _state.value = QuizState.Granted(correctAnswers, questions.size)
            } else {
                _state.value = QuizState.Denied
            }
            return
        }

        currentIndex++
        showCurrentQuestion()
    }

    fun retry() {
        currentIndex = 0
        correctAnswers = 0
        questions = questions.shuffled()
        showCurrentQuestion()
    }

    private fun showCurrentQuestion() {
        _state.value = QuizState.Question(
            question = questions[currentIndex],
            current = currentIndex + 1,
            total = questions.size
        )
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(context.applicationContext) as T
        }
    }
}