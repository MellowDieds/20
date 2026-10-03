package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "break_records")
data class BreakRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "COMPLETED", "SNOOZED", "SKIPPED"
    val durationSeconds: Int = 20,
    val routineName: String = "20-20-20 Kuralı",
    val note: String = ""
)
