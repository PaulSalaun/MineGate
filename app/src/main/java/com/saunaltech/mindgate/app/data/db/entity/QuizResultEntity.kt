package com.saunaltech.mindgate.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val date: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accessGranted: Boolean
)