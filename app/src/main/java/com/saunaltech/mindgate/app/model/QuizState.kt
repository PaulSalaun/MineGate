package com.saunaltech.mindgate.app.model

sealed class QuizState {
    object Loading : QuizState()
    object NoQuestions : QuizState()

    data class Question(
        val question: QuizQuestion,
        val current: Int,
        val total: Int
    ) : QuizState()

    data class Feedback(
        val question: QuizQuestion,
        val selectedIndex: Int,
        val isCorrect: Boolean,
        val current: Int,
        val total: Int,
        val isLast: Boolean
    ) : QuizState()

    data class Granted(val correctAnswers: Int, val total: Int) : QuizState()
    object Denied : QuizState()
}