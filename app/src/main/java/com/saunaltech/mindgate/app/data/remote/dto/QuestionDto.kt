package com.saunaltech.mindgate.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity

data class QuestionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("theme") val theme: String,
    @SerializedName("enonce") val enonce: String,
    @SerializedName("reponses") val reponses: List<String>,
    @SerializedName("bonne_reponse") val bonneReponse: Int,
    @SerializedName("difficulte") val difficulte: Int,
    @SerializedName("version") val version: Int,
    @SerializedName("actif") val actif: Boolean,
    @SerializedName("explication") val explication: String = ""
) {
    fun toEntity() = QuestionEntity(
        id = id,
        theme = theme,
        enonce = enonce,
        reponses = com.google.gson.Gson().toJson(reponses),
        bonneReponse = bonneReponse,
        difficulte = difficulte,
        version = version,
        actif = actif,
        explication = explication
    )
}