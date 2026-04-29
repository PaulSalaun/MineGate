package com.saunaltech.mindgate.app.model

sealed class QuizState {

    object Loading : QuizState()
    object NoQuestions : QuizState()

    data class Question(
        val question: QuizQuestion,
        val difficultyPlan: List<Int>,
        val currentSlotIndex: Int,
        val completedSlots: List<Boolean>
    ) : QuizState()

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