package com.saunaltech.mindgate.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Long,
    val theme: String,
    val enonce: String,
    val reponses: String,
    val bonneReponse: Int,
    val difficulte: Int,
    val version: Int,
    val actif: Boolean,
    val explication: String = ""
)