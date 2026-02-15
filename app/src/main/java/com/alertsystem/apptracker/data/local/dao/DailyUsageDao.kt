package com.alertsystem.apptracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alertsystem.apptracker.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY totalUsageSeconds DESC")
    fun getUsageByDate(date: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY totalUsageSeconds DESC")
    suspend fun getUsageByDateSync(date: String): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName AND date = :date")
    suspend fun getUsageByPackageAndDate(packageName: String, date: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName ORDER BY date DESC LIMIT :days")
    suspend fun getRecentUsageByPackage(packageName: String, days: Int): List<DailyUsageEntity>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getUsageBetweenDates(startDate: String, endDate: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getUsageBetweenDatesSync(startDate: String, endDate: String): List<DailyUsageEntity>

    @Query("SELECT SUM(totalUsageSeconds) FROM daily_usage WHERE date = :date")
    fun getTotalUsageByDate(date: String): Flow<Long?>

    @Query("SELECT SUM(totalUsageSeconds) FROM daily_usage WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalUsageForPackageBetweenDates(packageName: String, startDate: String, endDate: String): Long?

    @Query("SELECT AVG(totalUsageSeconds) FROM daily_usage WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageUsageForPackage(packageName: String, startDate: String, endDate: String): Double?

    @Query("SELECT AVG(openCount) FROM daily_usage WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageOpenCountForPackage(packageName: String, startDate: String, endDate: String): Double?

    @Query("SELECT AVG(shortSessionCount) FROM daily_usage WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageShortSessionsForPackage(packageName: String, startDate: String, endDate: String): Double?

    @Query("SELECT AVG(lateNightUsageSeconds) FROM daily_usage WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageLateNightUsageForPackage(packageName: String, startDate: String, endDate: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(usage: DailyUsageEntity)

    @Query("""
        UPDATE daily_usage
        SET totalUsageSeconds = totalUsageSeconds + :seconds,
            lateNightUsageSeconds = lateNightUsageSeconds + :lateNightSeconds
        WHERE packageName = :packageName AND date = :date
    """)
    suspend fun addUsageTime(packageName: String, date: String, seconds: Long, lateNightSeconds: Long)

    @Query("UPDATE daily_usage SET openCount = openCount + 1 WHERE packageName = :packageName AND date = :date")
    suspend fun incrementOpenCount(packageName: String, date: String)

    @Query("UPDATE daily_usage SET shortSessionCount = shortSessionCount + 1 WHERE packageName = :packageName AND date = :date")
    suspend fun incrementShortSessionCount(packageName: String, date: String)

    @Query("DELETE FROM daily_usage WHERE date < :date")
    suspend fun deleteOlderThan(date: String)

    @Query("DELETE FROM daily_usage")
    suspend fun deleteAll()
}
