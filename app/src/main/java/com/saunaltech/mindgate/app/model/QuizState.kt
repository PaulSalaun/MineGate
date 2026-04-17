package com.saunaltech.mindgate.app.model

sealed class QuizState {

    object Loading : QuizState()
    object NoQuestions : QuizState()

    /**
     * Une question est affichée et attend une réponse.
     *
     * @param question        La question courante (contient sa difficulté)
     * @param difficultyPlan  Plan complet : liste des niveaux à valider, ex [1,2,3,4,5]
     * @param currentSlotIndex Index de la question courante dans le plan (0-based)
     * @param completedSlots  Quels slots ont été validés dans ce cycle
     */
    data class Question(
        val question: QuizQuestion,
        val difficultyPlan: List<Int>,
        val currentSlotIndex: Int,
        val completedSlots: List<Boolean>
    ) : QuizState()

    /**
     * Feedback après une réponse.
     *
     * @param isGranted
     */
    data class Feedback(
        val question: QuizQuestion,
        val selectedIndex: Int,
        val isCorrect: Boolean,
        val difficultyPlan: List<Int>,
        val currentSlotIndex: Int,
        val completedSlots: List<Boolean>,
        val isGranted: Boolean
    ) : QuizState()

    object Granted : QuizState()
}