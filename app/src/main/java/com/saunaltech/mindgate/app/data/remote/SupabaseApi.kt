package com.saunaltech.mindgate.app.data.remote

import com.saunaltech.mindgate.app.data.remote.dto.PackVersionDto
import com.saunaltech.mindgate.app.data.remote.dto.QuestionDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/questions")
    suspend fun getQuestions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("version") versionFilter: String,    // ex: "gt.5" (greater than 5)
        @Query("actif") actif: String = "eq.true",
        @Query("select") select: String = "*"
    ): List<QuestionDto>

    @GET("rest/v1/pack_version")
    suspend fun getPackVersion(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") id: String = "eq.1",
        @Query("select") select: String = "version"
    ): List<PackVersionDto>
}