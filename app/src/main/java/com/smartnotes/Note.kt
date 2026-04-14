package com.smartnotes

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity tells Room to turn this exact blueprint into a database table
@Entity(tableName = "notes_table")
data class Note(
    // @PrimaryKey tells Room to automatically give every note a unique ID number
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalText: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
