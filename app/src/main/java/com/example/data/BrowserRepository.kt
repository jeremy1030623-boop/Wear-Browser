package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {
    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val cachedPages: Flow<List<CachedPage>> = dao.getAllCachedPages()
    val downloadedFiles: Flow<List<DownloadedFile>> = dao.getAllDownloadedFiles()
    val history: Flow<List<HistoryEntry>> = dao.getAllHistory()

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(Bookmark(url, title))
    }

    suspend fun removeBookmark(url: String) {
        dao.deleteBookmark(url)
    }

    suspend fun cachePage(url: String, content: String) {
        dao.cachePage(CachedPage(url, content))
    }

    suspend fun addDownloadedFile(fileName: String, url: String, mimeType: String, localPath: String, fileSize: Long) {
        dao.insertDownloadedFile(
            DownloadedFile(
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                localPath = localPath,
                fileSize = fileSize
            )
        )
    }

    suspend fun removeDownloadedFile(id: Long) {
        dao.deleteDownloadedFile(id)
    }

    suspend fun addHistoryEntry(url: String, title: String) {
        dao.insertHistoryEntry(HistoryEntry(url = url, title = title))
    }

    suspend fun removeHistoryEntry(id: Long) {
        dao.deleteHistoryEntry(id)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }
}
