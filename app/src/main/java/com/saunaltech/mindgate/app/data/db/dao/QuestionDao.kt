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

    @Query("SELECT MAX(id) FROM questions")
    suspend fun getLastId(): Long?

    @Upsert
    suspend fun upsertAll(questions: List<QuestionEntity>)

    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1")
    suspend fun getCount(): Int
}