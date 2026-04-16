package com.saunaltech.mindgate.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saunaltech.mindgate.app.data.db.entity.ThemeEntity

@Dao
interface ThemeDao {

    @Query("SELECT * FROM themes WHERE langue = :langue ORDER BY nom ASC")
    suspend fun getThemesByLangue(langue: String): List<ThemeEntity>

    @Query("SELECT * FROM themes ORDER BY nom ASC")
    suspend fun getAllThemes(): List<ThemeEntity>

    @Upsert
    suspend fun upsertAll(themes: List<ThemeEntity>)
}