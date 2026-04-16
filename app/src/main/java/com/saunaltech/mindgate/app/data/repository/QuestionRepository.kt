package com.saunaltech.mindgate.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity
import com.saunaltech.mindgate.app.data.remote.RetrofitClient
import com.saunaltech.mindgate.app.data.remote.SupabaseConfig

class QuestionRepository(private val context: Context) {

    private val db = MindGateDatabase.getInstance(context)
    private val questionDao = db.questionDao()
    private val themeDao = db.themeDao()
    private val api = RetrofitClient.create(SupabaseConfig.BASE_URL)

    suspend fun getQuestionsForQuiz(
        langue: String,
        themeIds: List<Long> = emptyList(),
        limit: Int = 5
    ): List<QuestionEntity> {
        return if (themeIds.isEmpty()) {
            questionDao.getQuestionsByLangue(langue, limit)
        } else {
            questionDao.getQuestions(langue, themeIds, limit)
        }
    }

    suspend fun syncIfNeeded(): SyncResult {
        if (!isNetworkAvailable()) return SyncResult.NoNetwork

        return try {
            // Récupère le dernier id local
            val localLastId = questionDao.getLastId() ?: 0L

            // Récupère le dernier id distant
            val remoteLastList = api.getMaxId(
                apiKey = SupabaseConfig.ANON_KEY,
                authorization = SupabaseConfig.AUTHORIZATION
            )
            val remoteLastId = remoteLastList.firstOrNull()?.id ?: 0L

            if (remoteLastId <= localLastId) return SyncResult.AlreadyUpToDate

            // Télécharge les nouvelles questions
            val newQuestions = api.getNewQuestions(
                apiKey = SupabaseConfig.ANON_KEY,
                authorization = SupabaseConfig.AUTHORIZATION,
                idFilter = "gt.$localLastId"
            )

            // Télécharge tous les thèmes
            val themes = api.getAllThemes(
                apiKey = SupabaseConfig.ANON_KEY,
                authorization = SupabaseConfig.AUTHORIZATION
            )

            db.runInTransaction {
                kotlinx.coroutines.runBlocking {
                    themeDao.upsertAll(themes.map { it.toEntity() })
                    questionDao.upsertAll(newQuestions.map { it.toEntity() })
                }
            }

            SyncResult.Success(newQuestions.size)

        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Erreur inconnue")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

sealed class SyncResult {
    object NoNetwork : SyncResult()
    object AlreadyUpToDate : SyncResult()
    data class Success(val newQuestions: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}