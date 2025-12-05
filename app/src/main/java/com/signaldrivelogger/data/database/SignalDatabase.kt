package com.signaldrivelogger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.signaldrivelogger.data.database.dao.SessionDao
import com.signaldrivelogger.data.database.dao.SignalRecordDao
import com.signaldrivelogger.data.database.dao.SimProfileDao
import com.signaldrivelogger.data.database.entities.SessionEntity
import com.signaldrivelogger.data.database.entities.SignalRecordEntity
import com.signaldrivelogger.data.database.entities.SimProfileEntity

/**
 * Room database for signal logging.
 * Version 1: Initial schema with sessions, signal_logs, and sim_profiles tables.
 */
@Database(
    entities = [
        SessionEntity::class,
        SignalRecordEntity::class,
        SimProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SignalDatabase : RoomDatabase() {
    abstract fun signalRecordDao(): SignalRecordDao
    abstract fun sessionDao(): SessionDao
    abstract fun simProfileDao(): SimProfileDao

    companion object {
        @Volatile
        private var INSTANCE: SignalDatabase? = null

        fun getDatabase(context: Context): SignalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SignalDatabase::class.java,
                    "signal_database"
                )
                    // Phase 4: Database Migration Strategy
                // WARNING: fallbackToDestructiveMigration() will DELETE ALL USER DATA if schema changes.
                // Before releasing v1.0, ensure schema is final. For v1.1+, remove this and provide
                // proper migrations (see MIGRATION_1_2 example below) or users will lose data on update.
                .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 1 to 2 (example for future migrations).
         * Uncomment and modify when needed.
         */
        /*
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration SQL here
                // Example: database.execSQL("ALTER TABLE signal_logs ADD COLUMN new_field TEXT")
            }
        }
        */
    }
}
