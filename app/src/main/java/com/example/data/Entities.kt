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
