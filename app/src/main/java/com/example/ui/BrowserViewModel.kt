package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.content.BroadcastReceiver
import android.os.Build
import android.webkit.URLUtil
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BrowserRepository
import com.example.data.DownloadedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BrowserRepository
    val bookmarks: StateFlow<List<com.example.data.Bookmark>>
    val cachedPages: StateFlow<List<com.example.data.CachedPage>>
    val downloadedFiles: StateFlow<List<DownloadedFile>>
    val history: StateFlow<List<com.example.data.HistoryEntry>>
    
    private val _currentUrl = MutableStateFlow("wearbrowser://home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _isDeepMode = MutableStateFlow(true) // Default to OLED friendly
    val isDeepMode: StateFlow<Boolean> = _isDeepMode.asStateFlow()

    private val _isPowerSavingMode = MutableStateFlow(false)
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    private val _textZoom = MutableStateFlow(100) // Default 100% text scale
    val textZoom: StateFlow<Int> = _textZoom.asStateFlow()

    private val _searchEngine = MutableStateFlow("Google")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).browserDao()
        repository = BrowserRepository(dao)
        bookmarks = repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        cachedPages = repository.cachedPages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        downloadedFiles = repository.downloadedFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        observeBattery(application)
        observePowerSaveMode(application)
        initTts(application)
    }

    private fun observePowerSaveMode(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            _isPowerSavingMode.value = powerManager.isPowerSaveMode
            
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    _isPowerSavingMode.value = powerManager.isPowerSaveMode
                }
            }
            try {
                context.registerReceiver(receiver, filter)
            } catch (e: Exception) {
                // Ignore receiver registration error if any
            }
        }
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
        if (url == "wearbrowser://home") {
            _currentUrl.value = url
            return
        }
        var formattedUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("file://") && !url.startsWith("about:") && !url.startsWith("wearbrowser://")) {
            formattedUrl = "https://$url"
        }
        _currentUrl.value = formattedUrl
    }

    fun updateUrlFromWebView(url: String) {
        if (url.isNotBlank() && _currentUrl.value != url) {
            _currentUrl.value = url
        }
    }

    fun translatePage(targetLanguageCode: String) {
        val current = _currentUrl.value
        if (current.isBlank() || current.startsWith("file://") || current.startsWith("about:blank") || current.contains("translate.google.com/translate")) {
            // Avoid double translation or translating local/empty files
            if (current.contains("translate.google.com/translate")) {
                // Try to extract original URL if they change language
                try {
                    val uri = android.net.Uri.parse(current)
                    val originalUrl = uri.getQueryParameter("u")
                    if (!originalUrl.isNullOrBlank()) {
                        val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
                        _currentUrl.value = "https://translate.google.com/translate?sl=auto&tl=$targetLanguageCode&u=$encodedUrl"
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }
        try {
            val encodedUrl = java.net.URLEncoder.encode(current, "UTF-8")
            _currentUrl.value = "https://translate.google.com/translate?sl=auto&tl=$targetLanguageCode&u=$encodedUrl"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleDeepMode() {
        _isDeepMode.value = !_isDeepMode.value
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(url, title)
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeBookmark(url)
        }
    }

    fun savePageForOffline(url: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cachePage(url, content)
        }
    }

    fun downloadFile(url: String, contentDisposition: String? = null, mimeType: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Guess the file name
                var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                if (fileName.isNullOrBlank() || fileName == "downloadfile.bin") {
                    // Extract name from URL if possible, otherwise use a timestamp
                    val urlPath = URL(url).path
                    val lastSegment = urlPath.substringAfterLast('/')
                    if (lastSegment.isNotBlank() && lastSegment.contains('.')) {
                        fileName = lastSegment
                    } else {
                        val ext = if (mimeType != null) android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) else null
                        fileName = "downloaded_" + System.currentTimeMillis() + (if (ext != null) ".$ext" else ".bin")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Starting download: $fileName", Toast.LENGTH_SHORT).show()
                }

                val u = URL(url)
                val conn = u.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.connect()
                
                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val contentLength = conn.contentLength.toLong()
                    val inputStream = conn.inputStream
                    
                    // Save to Environment.DIRECTORY_DOWNLOADS inside app's external files directory so no runtime permission is required
                    val dir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: getApplication<Application>().filesDir
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    
                    val file = File(dir, fileName)
                    val outputStream = FileOutputStream(file)
                    
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    
                    outputStream.close()
                    inputStream.close()
                    
                    repository.addDownloadedFile(
                        fileName = fileName,
                        url = url,
                        mimeType = mimeType ?: "application/octet-stream",
                        localPath = file.absolutePath,
                        fileSize = if (contentLength > 0) contentLength else totalBytesRead
                    )
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Downloaded to watch: $fileName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Download failed: HTTP $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteDownloadedFile(id: Long, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(localPath)
                if (file.exists()) {
                    file.delete()
                }
                repository.removeDownloadedFile(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "File deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error deleting: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addToHistory(url: String, title: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("file://") || url == "wearbrowser://home" || url.startsWith("wearbrowser://")) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addHistoryEntry(url, title.ifBlank { url })
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeHistoryEntry(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speakText(text: String) {
        if (text.isBlank()) return
        val textToSpeech = tts ?: return
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
            _isSpeaking.value = false
        } else {
            // Filter some web page noise if any
            val cleanText = text.replace(Regex("\\s+"), " ").trim()
            if (cleanText.isBlank()) return
            
            // Speak chunks of 300 chars to avoid buffer limitation
            val chunks = cleanText.chunked(300)
            _isSpeaking.value = true
            var first = true
            for (chunk in chunks) {
                val queueMode = if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                textToSpeech.speak(chunk, queueMode, null, "WebPageRead")
                first = false
            }
            
            // Monitor speaking status
            textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setTextZoom(zoom: Int) {
        _textZoom.value = zoom
    }

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
