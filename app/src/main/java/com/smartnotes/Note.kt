package com.smartnotes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)