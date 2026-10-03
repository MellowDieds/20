package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakDao {
    @Query("SELECT * FROM break_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BreakRecord>>

    @Query("SELECT * FROM break_records WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getRecordsSince(startOfDay: Long): Flow<List<BreakRecord>>

    @Query("SELECT COUNT(*) FROM break_records WHERE type = 'COMPLETED' AND timestamp >= :startOfDay")
    fun getTodayCompletedCount(startOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BreakRecord): Long

    @Query("DELETE FROM break_records")
    suspend fun clearAll()
}
