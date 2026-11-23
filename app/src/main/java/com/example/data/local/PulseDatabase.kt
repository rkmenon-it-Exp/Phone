package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        ContactGroupEntity::class,
        CallLogEntity::class,
        CallerIdCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao
    abstract fun callerIdCacheDao(): CallerIdCacheDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getInstance(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulsephone_unlimited.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
