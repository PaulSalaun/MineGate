package com.saunaltech.mindgate.app.data.remote.dto

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity

data class QuestionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("theme_id") val themeId: Long,
    @SerializedName("enonce") val enonce: String,
    @SerializedName("reponses") val reponses: List<String>,
    @SerializedName("bonne_reponse") val bonneReponse: Int,
    @SerializedName("difficulte") val difficulte: Int,
    @SerializedName("explication") val explication: String = "",
    @SerializedName("langue") val langue: String,
    @SerializedName("actif") val actif: Boolean
) {
    fun toEntity() = QuestionEntity(
        id = id,
        themeId = themeId,
        enonce = enonce,
        reponses = Gson().toJson(reponses),
        bonneReponse = bonneReponse,
        difficulte = difficulte,
        explication = explication,
        langue = langue,
        actif = actif
    )
}