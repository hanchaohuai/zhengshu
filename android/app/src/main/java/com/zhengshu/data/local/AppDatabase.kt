package com.zhengshu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhengshu.data.model.Evidence

@Database(
    entities = [Evidence::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun evidenceDao(): EvidenceDao
    
    companion object {
        private const val DATABASE_NAME = "zhengshu.db"
        
        @Volatile
        private var instance: AppDatabase? = null
        
        fun getDatabase(context: android.content.Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                instance = newInstance
                newInstance
            }
        }
    }
}
