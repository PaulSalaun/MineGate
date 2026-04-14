package com.saunaltech.mindgate.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pack_version")
data class PackVersionEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int
)