package com.saunaltech.mindgate.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE actif = 1 AND langue = :langue AND themeId IN (:themeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestions(langue: String, themeIds: List<Long>, limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE actif = 1 AND langue = :langue ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByLangue(langue: String, limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE actif = 1 AND langue = :langue AND difficulte = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByDifficulty(
        langue: String,
        difficulty: Int,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE actif = 1 AND langue = :langue AND difficulte = :difficulty AND themeId IN (:themeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByDifficultyAndThemes(
        langue: String,
        difficulty: Int,
        themeIds: List<Long>,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT MAX(id) FROM questions")
    suspend fun getLastId(): Long?

    @Upsert
    suspend fun upsertAll(questions: List<QuestionEntity>)

    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1 AND difficulte = :difficulty")
    suspend fun getCountByDifficulty(difficulty: Int): Int

    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1 AND themeId = :themeId")
    suspend fun getCountByTheme(themeId: Long): Int

    /** Nombre de questions actives pour un thème ET une langue donnée. */
    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1 AND themeId = :themeId AND langue = :langue")
    suspend fun getCountByThemeAndLangue(themeId: Long, langue: String): Int

    /** Retourne les IDs de thèmes distincts qui ont au moins une question dans cette langue. */
    @Query("SELECT DISTINCT themeId FROM questions WHERE actif = 1 AND langue = :langue")
    suspend fun getThemeIdsByLangue(langue: String): List<Long>
}