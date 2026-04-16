package com.saunaltech.mindgate.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.saunaltech.mindgate.app.data.db.entity.ThemeEntity

data class ThemeDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nom") val nom: String,
    @SerializedName("langue") val langue: String,
    @SerializedName("description") val description: String = ""
) {
    fun toEntity() = ThemeEntity(
        id = id,
        nom = nom,
        langue = langue,
        description = description
    )
}