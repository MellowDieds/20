package com.example.data.repository

import com.example.data.db.BreakDao
import com.example.data.db.BreakRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class BreakRepository(private val breakDao: BreakDao) {

    val allRecords: Flow<List<BreakRecord>> = breakDao.getAllRecords()

    fun getTodayRecords(): Flow<List<BreakRecord>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return breakDao.getRecordsSince(calendar.timeInMillis)
    }

    fun getTodayCompletedCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return breakDao.getTodayCompletedCount(calendar.timeInMillis)
    }

    suspend fun recordCompletedBreak(durationSeconds: Int = 20, routineName: String = "20-20-20 Kuralı") {
        breakDao.insertRecord(
            BreakRecord(
                type = "COMPLETED",
                durationSeconds = durationSeconds,
                routineName = routineName,
                note = "Göz dinlendirme tamamlandı"
            )
        )
    }

    suspend fun recordSnooze(durationMinutes: Int = 5) {
        breakDao.insertRecord(
            BreakRecord(
                type = "SNOOZED",
                durationSeconds = durationMinutes * 60,
                routineName = "Erteleme",
                note = "$durationMinutes dakika ertelendi"
            )
        )
    }

    suspend fun clearHistory() {
        breakDao.clearAll()
    }
}
