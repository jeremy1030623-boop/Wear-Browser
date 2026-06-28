package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_pages")
data class CachedPage(
    @PrimaryKey val url: String,
    val content: String, // For offline-first simplified viewing if needed, or just track metadata
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_files")
data class DownloadedFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val localPath: String,
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

