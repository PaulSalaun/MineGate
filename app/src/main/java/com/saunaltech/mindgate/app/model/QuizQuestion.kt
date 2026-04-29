package com.saunaltech.mindgate.app.model

data class QuizQuestion(
    val id: Long,
    val enonce: String,
    val reponses: List<String>,
    val bonneReponse: Int,
    val explication: String = "",
    val difficulty: Int = 1,
    val themeNom: String = ""
)