package com.alertsystem.apptracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alertsystem.apptracker.data.local.dao.AppSettingsDao
import com.alertsystem.apptracker.data.local.dao.DailyUsageDao
import com.alertsystem.apptracker.data.local.dao.UsageSessionDao
import com.alertsystem.apptracker.data.local.entity.AppSettingsEntity
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import com.alertsystem.apptracker.data.local.entity.UsageSessionEntity

@Database(
    entities = [
        AppSettingsEntity::class,
        DailyUsageEntity::class,
        UsageSessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun usageSessionDao(): UsageSessionDao

    companion object {
        const val DATABASE_NAME = "app_tracker_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN isNotificationEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN isAddictive INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN addedTimeMinutes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN currentAddTimeIncrement INTEGER NOT NULL DEFAULT 5")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN unsubscribeConfirmCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN isWork INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN addictiveManualOverride INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE app_settings ADD COLUMN addictiveOverrideUntil INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
