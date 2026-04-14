package com.saunaltech.mindgate.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PackVersionDto(
    @SerializedName("version") val version: Int
)