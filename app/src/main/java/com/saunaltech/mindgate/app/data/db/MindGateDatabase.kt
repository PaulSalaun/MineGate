package com.saunaltech.mindgate.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saunaltech.mindgate.app.data.db.dao.QuestionDao
import com.saunaltech.mindgate.app.data.db.dao.QuizResultDao
import com.saunaltech.mindgate.app.data.db.dao.ThemeDao
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity
import com.saunaltech.mindgate.app.data.db.entity.QuizResultEntity
import com.saunaltech.mindgate.app.data.db.entity.ThemeEntity

@Database(
    entities = [QuestionEntity::class, ThemeEntity::class, QuizResultEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MindGateDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun themeDao(): ThemeDao
    abstract fun quizResultDao(): QuizResultDao

    companion object {
        @Volatile
        private var INSTANCE: MindGateDatabase? = null

        fun getInstance(context: Context): MindGateDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MindGateDatabase::class.java,
                    "mindgate.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}