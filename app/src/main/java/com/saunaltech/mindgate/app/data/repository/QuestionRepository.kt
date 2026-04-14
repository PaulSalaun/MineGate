package com.saunaltech.mindgate.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.PackVersionEntity
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity
import com.saunaltech.mindgate.app.data.remote.RetrofitClient
import com.saunaltech.mindgate.app.data.remote.SupabaseConfig

class QuestionRepository(private val context: Context) {

    private val db = MindGateDatabase.getInstance(context)
    private val questionDao = db.questionDao()
    private val packVersionDao = db.packVersionDao()
    private val api = RetrofitClient.create(SupabaseConfig.BASE_URL)

    // Récupère les questions locales pour le quiz
    suspend fun getQuestionsForQuiz(themes: List<String>): List<QuestionEntity> {
        return if (themes.isEmpty()) questionDao.getAllQuestions()
        else questionDao.getQuestionsByThemes(themes)
    }

    // Sync depuis Supabase si réseau disponible
    suspend fun syncIfNeeded(): SyncResult {
        if (!isNetworkAvailable()) return SyncResult.NoNetwork

        return try {
            val localVersion = packVersionDao.getLocalVersion() ?: 0

            // Vérifie si une nouvelle version existe
            val remoteVersionList = api.getPackVersion(
                apiKey = SupabaseConfig.ANON_KEY,
                authorization = SupabaseConfig.AUTHORIZATION
            )
            val remoteVersion = remoteVersionList.firstOrNull()?.version ?: 0

            val localCount = questionDao.getCount()
            if (remoteVersion <= localVersion && localCount > 0) return SyncResult.AlreadyUpToDate

            // Télécharge uniquement les nouvelles questions
            val newQuestions = api.getQuestions(
                apiKey = SupabaseConfig.ANON_KEY,
                authorization = SupabaseConfig.AUTHORIZATION,
                versionFilter = "gt.$localVersion"
            )

            // Sauvegarde en local
            questionDao.upsertAll(newQuestions.map { it.toEntity() })
            packVersionDao.upsert(PackVersionEntity(version = remoteVersion))

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