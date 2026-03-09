package com.alertsystem.apptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alertsystem.apptracker.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings ORDER BY appName ASC")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getSettingsByPackage(packageName: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    fun getSettingsByPackageFlow(packageName: String): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE isExcluded = 0")
    suspend fun getNonExcludedApps(): List<AppSettingsEntity>

    @Query("SELECT * FROM app_settings WHERE isExcluded = 1")
    suspend fun getExcludedApps(): List<AppSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(settings: List<AppSettingsEntity>)

    @Update
    suspend fun update(settings: AppSettingsEntity)

    @Query("UPDATE app_settings SET timeLimitMinutes = :limitMinutes WHERE packageName = :packageName")
    suspend fun updateTimeLimit(packageName: String, limitMinutes: Int)

    @Query("UPDATE app_settings SET isExcluded = :isExcluded, exclusionConfirmCount = :confirmCount WHERE packageName = :packageName")
    suspend fun updateExclusion(packageName: String, isExcluded: Boolean, confirmCount: Int)

    @Query("UPDATE app_settings SET lastNotificationTime = :time WHERE packageName = :packageName")
    suspend fun updateLastNotificationTime(packageName: String, time: Long)

    @Query("UPDATE app_settings SET isNotificationEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateNotificationEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE app_settings SET isAddictive = :isAddictive, addictiveOverrideUntil = :overrideUntil WHERE packageName = :packageName")
    suspend fun updateAddictive(packageName: String, isAddictive: Boolean, overrideUntil: Long = 0)

    @Query("UPDATE app_settings SET addedTimeMinutes = :minutes, currentAddTimeIncrement = :increment WHERE packageName = :packageName")
    suspend fun updateAddedTime(packageName: String, minutes: Int, increment: Int)

    @Query("UPDATE app_settings SET unsubscribeConfirmCount = :count WHERE packageName = :packageName")
    suspend fun updateUnsubscribeConfirmCount(packageName: String, count: Int)

    @Query("UPDATE app_settings SET isNotificationEnabled = :enabled, unsubscribeConfirmCount = 0 WHERE packageName = :packageName")
    suspend fun updateNotificationEnabledAndResetConfirm(packageName: String, enabled: Boolean)

    @Query("SELECT * FROM app_settings WHERE isNotificationEnabled = 1")
    suspend fun getAppsWithNotificationsEnabled(): List<AppSettingsEntity>

    @Query("SELECT * FROM app_settings WHERE isAddictive = 1")
    fun getAddictiveApps(): Flow<List<AppSettingsEntity>>

    @Query("UPDATE app_settings SET isWork = :isWork WHERE packageName = :packageName")
    suspend fun updateWork(packageName: String, isWork: Boolean)

    @Query("DELETE FROM app_settings WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()
}
