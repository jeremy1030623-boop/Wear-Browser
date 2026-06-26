package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BrowserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BrowserRepository
    val bookmarks: StateFlow<List<com.example.data.Bookmark>>
    
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _isDeepMode = MutableStateFlow(true) // Default to OLED friendly
    val isDeepMode: StateFlow<Boolean> = _isDeepMode.asStateFlow()

    private val _isPowerSavingMode = MutableStateFlow(false)
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).browserDao()
        repository = BrowserRepository(dao)
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        observeBattery(application)
    }

    private fun observeBattery(context: Context) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        
        if (batteryPct < 20) {
            _isPowerSavingMode.value = true
        }
    }

    fun navigateTo(url: String) {
        var formattedUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            formattedUrl = "https://$url"
        }
        _currentUrl.value = formattedUrl
    }

    fun toggleDeepMode() {
        _isDeepMode.value = !_isDeepMode.value
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            repository.addBookmark(url, title)
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            repository.removeBookmark(url)
        }
    }
}
