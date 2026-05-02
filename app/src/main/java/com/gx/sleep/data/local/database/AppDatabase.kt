package com.gx.sleep.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gx.sleep.data.local.dao.SleepSessionDao
import com.gx.sleep.data.local.dao.SoundEventDao
import com.gx.sleep.data.local.dao.SoundSampleDao
import com.gx.sleep.data.local.entity.SessionStatus
import com.gx.sleep.data.local.entity.SleepSessionEntity
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.local.entity.SoundSampleEntity

@Database(
    entities = [
        SleepSessionEntity::class,
        SoundSampleEntity::class,
        SoundEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun soundSampleDao(): SoundSampleDao
    abstract fun soundEventDao(): SoundEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gx_sleep.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
