package com.alertsystem.apptracker.di

import android.content.Context
import androidx.room.Room
import com.alertsystem.apptracker.data.local.AppDatabase
import com.alertsystem.apptracker.data.local.dao.AppSettingsDao
import com.alertsystem.apptracker.data.local.dao.DailyUsageDao
import com.alertsystem.apptracker.data.local.dao.UsageSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }

    @Provides
    @Singleton
    fun provideDailyUsageDao(database: AppDatabase): DailyUsageDao {
        return database.dailyUsageDao()
    }

    @Provides
    @Singleton
    fun provideUsageSessionDao(database: AppDatabase): UsageSessionDao {
        return database.usageSessionDao()
    }
}
