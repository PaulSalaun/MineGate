package com.saunaltech.mindgate.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity

@Dao
interface QuestionDao {

    @Query("SELECT * FROM questions WHERE actif = 1 AND theme IN (:themes)")
    suspend fun getQuestionsByThemes(themes: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE actif = 1")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Upsert
    suspend fun upsertAll(questions: List<QuestionEntity>)

    @Query("UPDATE questions SET actif = 0 WHERE id IN (:ids)")
    suspend fun softDeleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM questions WHERE actif = 1")
    suspend fun getCount(): Int
}