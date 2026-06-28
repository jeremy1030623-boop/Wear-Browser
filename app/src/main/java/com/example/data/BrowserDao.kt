package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("SELECT * FROM cached_pages ORDER BY timestamp DESC")
    fun getAllCachedPages(): Flow<List<CachedPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cachePage(page: CachedPage)

    @Query("SELECT * FROM downloaded_files ORDER BY timestamp DESC")
    fun getAllDownloadedFiles(): Flow<List<DownloadedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedFile(file: DownloadedFile)

    @Query("DELETE FROM downloaded_files WHERE id = :id")
    suspend fun deleteDownloadedFile(id: Long)

    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntry)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryEntry(id: Long)

    @Query("DELETE FROM history_entries")
    suspend fun clearAllHistory()
}
