package com.saunaltech.mindgate.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.saunaltech.mindgate.app.data.db.entity.QuizResultEntity

@Dao
interface QuizResultDao {

    @Insert
    suspend fun insert(result: QuizResultEntity)

    @Query("SELECT * FROM quiz_results ORDER BY date DESC")
    suspend fun getAll(): List<QuizResultEntity>

    @Query("SELECT * FROM quiz_results WHERE appPackage = :pkg ORDER BY date DESC")
    suspend fun getByApp(pkg: String): List<QuizResultEntity>

    @Query("SELECT COUNT(*) FROM quiz_results")
    suspend fun getTotalQuizzes(): Int

    @Query("SELECT COUNT(*) FROM quiz_results WHERE accessGranted = 1")
    suspend fun getTotalGranted(): Int

    @Query("SELECT SUM(correctAnswers) FROM quiz_results")
    suspend fun getTotalCorrect(): Int?

    @Query("SELECT SUM(totalQuestions) FROM quiz_results")
    suspend fun getTotalQuestions(): Int?

    @Query("SELECT * FROM quiz_results ORDER BY date DESC LIMIT 20")
    suspend fun getRecent(): List<QuizResultEntity>
}

// Note: Les stats par difficulté et par thème sont calculées côté ViewModel
// à partir des QuizResultEntity combinées avec les QuestionEntity via QuestionDao.
// Voir MainViewModel pour l'implémentation.