package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val dao: BrowserDao) {
    val bookmarks: Flow<List<Bookmark>> = dao.getAllBookmarks()
    val cachedPages: Flow<List<CachedPage>> = dao.getAllCachedPages()

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(Bookmark(url, title))
    }

    suspend fun removeBookmark(url: String) {
        dao.deleteBookmark(url)
    }

    suspend fun cachePage(url: String, content: String) {
        dao.cachePage(CachedPage(url, content))
    }
}
