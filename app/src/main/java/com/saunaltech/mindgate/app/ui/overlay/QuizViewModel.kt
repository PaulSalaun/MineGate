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
    private val db = MindGateDatabase.getInstance(context)
    private val quizResultDao = db.quizResultDao()
    private val themeDao = db.themeDao()

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state: StateFlow<QuizState> = _state

    private var difficultyPlan: List<Int> = emptyList()
    private var currentSlotIndex: Int = 0
    private val completedSlots: MutableList<Boolean> = mutableListOf()
    private val poolByDifficulty: MutableMap<Int, MutableList<QuizQuestion>> = mutableMapOf()
    private val askedIdsByDiff: MutableMap<Int, MutableSet<Long>> = mutableMapOf()

    private var totalAnswered = 0
    private var totalCorrect = 0
    private var currentPackage = ""

    fun loadQuestions(packageName: String = "") {
        currentPackage = packageName
        _state.value = QuizState.Loading

        viewModelScope.launch {
            val langue = prefs.loadLangue()
            val themeIds = prefs.loadActiveThemeIds()
            val config = prefs.loadQuizConfig()

            // Charge le map themeId → nom pour l'affichage
            val themeMap: Map<Long, String> = try {
                themeDao.getAll().associate { it.id to it.nom }
            } catch (_: Exception) {
                emptyMap()
            }

            difficultyPlan = config.difficulties
            currentSlotIndex = 0
            totalAnswered = 0
            totalCorrect = 0
            poolByDifficulty.clear()
            askedIdsByDiff.clear()
            completedSlots.clear()
            repeat(difficultyPlan.size) { completedSlots.add(false) }

            val distinctDifficulties = difficultyPlan.toSet()
            var hasAtLeastOneQuestion = false

            for (diff in distinctDifficulties) {
                val needed = difficultyPlan.count { it == diff }
                val fetchLimit = (needed * 4).coerceAtLeast(12)

                val entities = repository.getQuestionsForQuizByDifficulty(
                    langue = langue,
                    themeIds = themeIds,
                    difficulty = diff,
                    limit = fetchLimit
                )

                if (entities.isNotEmpty()) {
                    hasAtLeastOneQuestion = true
                    poolByDifficulty[diff] = entities.map { entity ->
                        val reponses: List<String> = Gson().fromJson(
                            entity.reponses,
                            object : TypeToken<List<String>>() {}.type
                        )
                        QuizQuestion(
                            id = entity.id,
                            enonce = entity.enonce,
                            reponses = reponses,
                            bonneReponse = entity.bonneReponse,
                            explication = entity.explication,
                            difficulty = entity.difficulte,
                            themeNom = themeMap[entity.themeId] ?: ""
                        )
                    }.shuffled().toMutableList()
                    askedIdsByDiff[diff] = mutableSetOf()
                }
            }

            if (!hasAtLeastOneQuestion) {
                _state.value = QuizState.NoQuestions
                return@launch
            }

            showNextQuestion()
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val current = (state.value as? QuizState.Question)?.question ?: return
        val isCorrect = (selectedIndex + 1) == current.bonneReponse

        totalAnswered++
        if (isCorrect) {
            totalCorrect++
            completedSlots[currentSlotIndex] = true
            currentSlotIndex++
        } else {
            currentSlotIndex = 0
            completedSlots.fill(false)
        }

        val isGranted = currentSlotIndex >= difficultyPlan.size

        _state.value = QuizState.Feedback(
            question = current,
            selectedIndex = selectedIndex,
            isCorrect = isCorrect,
            difficultyPlan = difficultyPlan,
            currentSlotIndex = if (isCorrect) currentSlotIndex - 1 else 0,
            completedSlots = completedSlots.toList(),
            isGranted = isGranted
        )

        if (isGranted) saveResult(granted = true)
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
        if (difficultyPlan.isEmpty()) {
            _state.value = QuizState.NoQuestions; return
        }

        val targetDiff = difficultyPlan[currentSlotIndex]
        val pool = poolByDifficulty[targetDiff]
        val askedIds = askedIdsByDiff.getOrPut(targetDiff) { mutableSetOf() }

        if (pool.isNullOrEmpty()) {
            completedSlots[currentSlotIndex] = true
            currentSlotIndex++
            if (currentSlotIndex >= difficultyPlan.size) {
                _state.value = QuizState.Granted
                saveResult(granted = true)
            } else showNextQuestion()
            return
        }

        var next = pool.firstOrNull { it.id !in askedIds }
        if (next == null) {
            askedIds.clear(); next = pool.randomOrNull()
        }
        if (next == null) {
            _state.value = QuizState.NoQuestions; return
        }

        askedIds.add(next.id)
        _state.value = QuizState.Question(
            question = next,
            difficultyPlan = difficultyPlan,
            currentSlotIndex = currentSlotIndex,
            completedSlots = completedSlots.toList()
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