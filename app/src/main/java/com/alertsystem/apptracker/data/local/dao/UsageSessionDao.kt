package com.alertsystem.apptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alertsystem.apptracker.data.local.entity.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSessionDao {

    @Query("SELECT * FROM usage_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsByDate(date: String): Flow<List<UsageSessionEntity>>

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName AND date = :date ORDER BY startTime DESC")
    suspend fun getSessionsByPackageAndDate(packageName: String, date: String): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessionsByPackage(packageName: String, limit: Int): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<UsageSessionEntity>>

    @Query("SELECT COUNT(*) FROM usage_sessions WHERE packageName = :packageName AND date = :date")
    suspend fun getSessionCountByPackageAndDate(packageName: String, date: String): Int

    @Insert
    suspend fun insert(session: UsageSessionEntity): Long

    @Query("DELETE FROM usage_sessions WHERE date < :date")
    suspend fun deleteOlderThan(date: String)

    @Query("DELETE FROM usage_sessions")
    suspend fun deleteAll()
}
