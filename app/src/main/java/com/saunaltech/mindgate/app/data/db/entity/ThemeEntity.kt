package com.saunaltech.mindgate.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: Long,
    val nom: String,
    val langue: String,
    val description: String
)