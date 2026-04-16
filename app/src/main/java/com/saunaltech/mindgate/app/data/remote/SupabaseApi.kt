package com.saunaltech.mindgate.app.data.remote

import com.saunaltech.mindgate.app.data.remote.dto.QuestionDto
import com.saunaltech.mindgate.app.data.remote.dto.ThemeDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/questions")
    suspend fun getNewQuestions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,          // "gt.42" → id > 42
        @Query("actif") actif: String = "eq.true",
        @Query("select") select: String = "*"
    ): List<QuestionDto>

    @GET("rest/v1/questions")
    suspend fun getMaxId(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id",
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: String = "1"
    ): List<QuestionDto>

    @GET("rest/v1/themes")
    suspend fun getAllThemes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*"
    ): List<ThemeDto>
}