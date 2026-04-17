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

    // ── Plan de jeu ──────────────────────────────────────────────────────────
    // Le plan est la liste ORDONNÉE des difficultés à valider (ex : [1,2,3,4,5]).
    // L'utilisateur doit répondre correctement à chaque slot dans l'ordre.
    // Une mauvaise réponse remet currentSlotIndex à 0 (recommencer le plan).
    private var difficultyPlan: List<Int> = emptyList()
    private var currentSlotIndex: Int = 0

    // completedSlots[i] = true si le slot i a été validé dans ce cycle
    private val completedSlots: MutableList<Boolean> = mutableListOf()

    // Pool par difficulté : Map<difficulté 1-5, questions disponibles>
    private val poolByDifficulty: MutableMap<Int, MutableList<QuizQuestion>> = mutableMapOf()

    // IDs déjà posés par difficulté (pour éviter les répétitions dans un cycle)
    private val askedIdsByDiff: MutableMap<Int, MutableSet<Long>> = mutableMapOf()

    private var totalAnswered = 0
    private var totalCorrect = 0
    private var currentPackage = ""

    // ── Chargement initial ────────────────────────────────────────────────────

    fun loadQuestions(packageName: String = "") {
        currentPackage = packageName
        _state.value = QuizState.Loading

        viewModelScope.launch {
            val langue = prefs.loadLangue()
            val themeIds = prefs.loadActiveThemeIds()
            val config = prefs.loadQuizConfig()

            difficultyPlan = config.difficulties
            currentSlotIndex = 0
            totalAnswered = 0
            totalCorrect = 0
            poolByDifficulty.clear()
            askedIdsByDiff.clear()

            // Initialise completedSlots
            completedSlots.clear()
            repeat(difficultyPlan.size) { completedSlots.add(false) }

            // Charge un pool pour chaque niveau de difficulté distinct présent dans le plan.
            // On charge 4× le besoin pour avoir de la variété et gérer les relances.
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
                            difficulty = entity.difficulte
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

    // ── Réponse utilisateur ───────────────────────────────────────────────────

    fun answerQuestion(selectedIndex: Int) {
        val current = (state.value as? QuizState.Question)?.question ?: return
        // bonneReponse est stocké en 1-based dans la BDD
        val isCorrect = (selectedIndex + 1) == current.bonneReponse

        totalAnswered++
        if (isCorrect) {
            totalCorrect++
            completedSlots[currentSlotIndex] = true
            currentSlotIndex++
        } else {
            // Mauvaise réponse → reset complet du cycle
            currentSlotIndex = 0
            completedSlots.fill(false)
        }

        val isGranted = currentSlotIndex >= difficultyPlan.size

        _state.value = QuizState.Feedback(
            question = current,
            selectedIndex = selectedIndex,
            isCorrect = isCorrect,
            difficultyPlan = difficultyPlan,
            // Pour l'affichage on montre le slot qui vient d'être répondu
            currentSlotIndex = if (isCorrect) currentSlotIndex - 1 else 0,
            completedSlots = completedSlots.toList(),
            isGranted = isGranted
        )

        if (isGranted) {
            saveResult(granted = true)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun nextQuestion() {
        val feedback = _state.value as? QuizState.Feedback ?: return
        if (feedback.isGranted) {
            _state.value = QuizState.Granted
            return
        }
        showNextQuestion()
    }

    // ── Sélection de la prochaine question ────────────────────────────────────

    private fun showNextQuestion() {
        if (difficultyPlan.isEmpty()) {
            _state.value = QuizState.NoQuestions
            return
        }

        // Niveau de difficulté cible = slot courant dans le plan
        val targetDiff = difficultyPlan[currentSlotIndex]
        val pool = poolByDifficulty[targetDiff]
        val askedIds = askedIdsByDiff.getOrPut(targetDiff) { mutableSetOf() }

        if (pool.isNullOrEmpty()) {
            // Aucune question pour ce niveau : on passe le slot (mode dégradé)
            completedSlots[currentSlotIndex] = true
            currentSlotIndex++
            if (currentSlotIndex >= difficultyPlan.size) {
                _state.value = QuizState.Granted
                saveResult(granted = true)
            } else {
                showNextQuestion()
            }
            return
        }

        // Cherche une question non encore posée pour ce niveau
        var next = pool.firstOrNull { it.id !in askedIds }
        if (next == null) {
            // Pool épuisé pour ce niveau → on remet à zéro les ids de ce niveau
            askedIds.clear()
            next = pool.randomOrNull()
        }

        if (next == null) {
            _state.value = QuizState.NoQuestions
            return
        }

        askedIds.add(next.id)

        _state.value = QuizState.Question(
            question = next,
            difficultyPlan = difficultyPlan,
            currentSlotIndex = currentSlotIndex,
            completedSlots = completedSlots.toList()
        )
    }

    // ── Persistance ───────────────────────────────────────────────────────────

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