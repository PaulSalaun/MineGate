package com.saunaltech.mindgate.app.model

sealed class QuizState {
    object Loading : QuizState()
    object NoQuestions : QuizState()

    data class Question(
        val question: QuizQuestion,
        val streak: Int,
        val required: Int
    ) : QuizState()

    data class Feedback(
        val question: QuizQuestion,
        val selectedIndex: Int,
        val isCorrect: Boolean,
        val streak: Int,
        val required: Int,
        val isGranted: Boolean
    ) : QuizState()

    object Granted : QuizState()
}