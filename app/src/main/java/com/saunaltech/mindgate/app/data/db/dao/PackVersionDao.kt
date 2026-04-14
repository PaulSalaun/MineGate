package com.saunaltech.mindgate.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saunaltech.mindgate.app.data.db.entity.PackVersionEntity

@Dao
interface PackVersionDao {

    @Query("SELECT version FROM pack_version WHERE id = 1")
    suspend fun getLocalVersion(): Int?

    @Upsert
    suspend fun upsert(packVersion: PackVersionEntity)
}