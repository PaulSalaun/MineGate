package com.saunaltech.mindgate.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saunaltech.mindgate.app.data.db.dao.PackVersionDao
import com.saunaltech.mindgate.app.data.db.dao.QuestionDao
import com.saunaltech.mindgate.app.data.db.entity.PackVersionEntity
import com.saunaltech.mindgate.app.data.db.entity.QuestionEntity

@Database(
    entities = [QuestionEntity::class, PackVersionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MindGateDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun packVersionDao(): PackVersionDao

    companion object {
        @Volatile
        private var INSTANCE: MindGateDatabase? = null

        fun getInstance(context: Context): MindGateDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MindGateDatabase::class.java,
                    "mindgate.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}