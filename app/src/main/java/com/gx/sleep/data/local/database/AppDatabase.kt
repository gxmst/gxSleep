package com.gx.sleep.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun soundSampleDao(): SoundSampleDao
    abstract fun soundEventDao(): SoundEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN baselineRms REAL NOT NULL DEFAULT 50.0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gx_sleep.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
