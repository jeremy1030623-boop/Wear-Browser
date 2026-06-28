package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.speech.RecognizerIntent
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.TimeText
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isDeepMode by viewModel.isDeepMode.collectAsState()
    val isPowerSavingMode by viewModel.isPowerSavingMode.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val history by viewModel.history.collectAsState()
    val textZoom by viewModel.textZoom.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
 
    // Intercept swipe-to-dismiss gesture to navigate WebView history backwards gracefully, or return to home
    BackHandler(enabled = !showMenu && currentUrl != "wearbrowser://home") {
        if (canGoBack) {
            webViewRef?.goBack()
        } else {
            viewModel.navigateTo("wearbrowser://home")
        }
    }

    val webViewFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Focus WebView for Physical Rotating Crown Scrolling whenever main menu closes
    LaunchedEffect(showMenu) {
        if (!showMenu) {
            webViewFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showMenu) {
            BookmarkMenu(
                bookmarks = bookmarks,
                downloadedFiles = downloadedFiles,
                history = history,
                currentUrl = currentUrl,
                textZoom = textZoom,
                onSetTextZoom = { zoom -> viewModel.setTextZoom(zoom) },
                isSpeaking = isSpeaking,
                onToggleSpeak = {
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else {
                        webViewRef?.evaluateJavascript(
                            "(function() { return document.body.innerText; })();"
                        ) { text ->
                            val cleanText = text?.removePrefix("\"")?.removeSuffix("\"")
                                ?.replace("\\n", " ")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\u003C", "<")
                            if (!cleanText.isNullOrBlank()) {
                                viewModel.speakText(cleanText)
                            } else {
                                Toast.makeText(viewModel.getApplication(), "No readable text on this page", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onTranslate = { lang ->
                    viewModel.translatePage(lang)
                    showMenu = false
                },
                onDeleteDownloadedFile = { id, path ->
                    viewModel.deleteDownloadedFile(id, path)
                },
                onDeleteHistoryEntry = { id ->
                    viewModel.deleteHistoryEntry(id)
                },
                onClearHistory = {
                    viewModel.clearHistory()
                },
                onNavigate = { url ->
                    viewModel.navigateTo(url)
                    showMenu = false
                },
                onClose = { showMenu = false },
                onToggleDeepMode = { viewModel.toggleDeepMode() },
                isDeepMode = isDeepMode
            )
        } else if (currentUrl == "wearbrowser://home") {
            HomeScreen(
                bookmarks = bookmarks,
                history = history,
                onNavigate = { url ->
                    viewModel.navigateTo(url)
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDeepMode) Color.Black else MaterialTheme.colorScheme.background)
                    .focusRequester(webViewFocusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        // Support high-precision mechanical crown scrolling for the web page itself!
                        webViewRef?.scrollBy(0, (it.verticalScrollPixels * 1.5f).toInt())
                        true
                    }
            ) {
                WebViewComponent(
                    url = currentUrl,
                    isPowerSaving = isPowerSavingMode,
                    textZoom = textZoom,
                    onPageStarted = { isLoading = true },
                    onPageFinished = { title ->
                        isLoading = false
                        pageTitle = title ?: ""
                        viewModel.addToHistory(currentUrl, pageTitle)
                        canGoBack = webViewRef?.canGoBack() == true
                    },
                    onWebViewCreated = { webViewRef = it },
                    onDownloadRequested = { downloadUrl, contentDisposition, mimeType ->
                        viewModel.downloadFile(downloadUrl, contentDisposition, mimeType)
                    }
                )

                // Overlay Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        
                        IconButton(
                            onClick = { viewModel.addBookmark(currentUrl, pageTitle) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Bookmark, contentDescription = "Bookmark")
                        }

                        IconButton(
                            onClick = {
                                viewModel.downloadFile(currentUrl)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                }
                
                if (isLoading) {
                    CircularProgressIndicator(
                        progress = { 0.5f },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        
        TimeText(modifier = Modifier.align(Alignment.TopCenter)) {
            time()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewComponent(
    url: String,
    isPowerSaving: Boolean,
    textZoom: Int,
    onPageStarted: () -> Unit,
    onPageFinished: (String?) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onDownloadRequested: (String, String?, String?) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)
                
                // Enforce hardware acceleration for silky-smooth scrolling on Wear OS
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadRequested(downloadUrl, contentDisposition, mimetype)
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(view?.title)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = !isPowerSaving
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    allowFileAccess = true // Enable local file access to load downloaded pages
                    
                    // Support zooming for better readability on tiny smartwatch screens
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false // Hide default zoom buttons to keep screen clean
                    
                    // Text auto-scaling to optimize paragraphs for tiny watch viewports
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    
                    cacheMode = if (isPowerSaving) WebSettings.LOAD_CACHE_ONLY else WebSettings.LOAD_DEFAULT
                }
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
            webView.settings.textZoom = textZoom
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun BookmarkMenu(
    bookmarks: List<com.example.data.Bookmark>,
    downloadedFiles: List<com.example.data.DownloadedFile>,
    history: List<com.example.data.HistoryEntry>,
    currentUrl: String,
    textZoom: Int,
    onSetTextZoom: (Int) -> Unit,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onTranslate: (String) -> Unit,
    onDeleteDownloadedFile: (Long, String) -> Unit,
    onDeleteHistoryEntry: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onToggleDeepMode: () -> Unit,
    isDeepMode: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSearchDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showTranslateMenu by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                showSearchDialog = true
            }
        }
    }

    // Check if current URL is a translatable web page (http/https and not local/blank files)
    val isTranslatable = currentUrl.startsWith("http://") || currentUrl.startsWith("https://")

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = listState,
            modifier = modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            listState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    }
            ) {
                item {
                    ListHeader {
                        Text("Navigation")
                    }
                }

                item {
                    Card(
                        onClick = { onNavigate("wearbrowser://home") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home icon",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text("Home", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                // Search & Enter URL Button
                item {
                    Card(
                        onClick = { showSearchDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search icon",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Search or Enter URL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Share/Open on Phone Button
                item {
                    Card(
                        onClick = { showQrDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = "Phone icon",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text("Open on Phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("Settings")
                    }
                }
                
                item {
                    Card(
                        onClick = onToggleDeepMode,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OLED Black Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDeepMode) "Pure black background" else "Standard dark theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                imageVector = if (isDeepMode) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                contentDescription = "Toggle state",
                                tint = if (isDeepMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Real-Time TTS Voice Reader (Read Aloud)
                item {
                    Card(
                        onClick = onToggleSpeak,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSpeaking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Listen to Page", style = MaterialTheme.typography.labelMedium, color = if (isSpeaking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(if (isSpeaking) "Reading aloud..." else "Text-to-speech reader", style = MaterialTheme.typography.bodySmall, color = if (isSpeaking) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud icon",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Web Page Text Zoom Control
                item {
                    Card(
                        onClick = {},
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Text Zoom: $textZoom%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { if (textZoom > 60) onSetTextZoom(textZoom - 20) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onSetTextZoom(100) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { if (textZoom < 240) onSetTextZoom(textZoom + 20) },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase text size",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isTranslatable) {
                    item {
                        ListHeader {
                            Text("Translate Page")
                        }
                    }

                    val languages = listOf(
                        "zh-TW" to "繁體中文",
                        "zh-CN" to "简体中文",
                        "en" to "English",
                        "ja" to "日本語",
                        "es" to "Español",
                        "fr" to "Français",
                        "de" to "Deutsch",
                        "ko" to "한국어"
                    )

                    if (!showTranslateMenu) {
                        item {
                            Card(
                                onClick = { showTranslateMenu = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("翻譯", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                onClick = { showTranslateMenu = false },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
                            ) {
                                Text("返回", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp))
                            }
                        }
                        languages.forEach { (code, name) ->
                            item {
                                Card(
                                    onClick = {
                                        onTranslate(code)
                                        showTranslateMenu = false
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("Bookmarks")
                    }
                }

                if (bookmarks.isEmpty()) {
                    item {
                        Text("No bookmarks", style = MaterialTheme.typography.bodySmall)
                    }
                }

                items(bookmarks) { bookmark ->
                    Card(
                        onClick = { onNavigate(bookmark.url) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column {
                            Text(bookmark.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(bookmark.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("Downloads")
                    }
                }

                if (downloadedFiles.isEmpty()) {
                    item {
                        Text("No downloaded files", style = MaterialTheme.typography.bodySmall)
                    }
                }

                items(downloadedFiles) { file ->
                    Card(
                        onClick = { onNavigate("file://${file.localPath}") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.fileName, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(formatFileSize(file.fileSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { onDeleteDownloadedFile(file.id, file.localPath) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete File",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("History")
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Text("No history", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    item {
                        Button(
                            onClick = onClearHistory,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Text("Clear All History", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    items(history) { entry ->
                        Card(
                            onClick = { onNavigate(entry.url) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                    Text(entry.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { onDeleteHistoryEntry(entry.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete History Entry",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            TimeText {
                time()
            }
            ScrollIndicator(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

    // SEARCH & URL INPUT DIALOG
    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val searchListState = rememberScalingLazyListState()
                val searchFocusRequester = remember { FocusRequester() }
                
                LaunchedEffect(Unit) {
                    searchFocusRequester.requestFocus()
                }
                
                ScalingLazyColumn(
                    state = searchListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(searchFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                searchListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        ListHeader {
                            Text("Search & URL", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) searchQuery else "https://www.google.com/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}"
                                            onNavigate(dest)
                                            showSearchDialog = false
                                        }
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Type query...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            
                            IconButton(
                                onClick = {
                                    val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak URL or search")
                                    }
                                    try {
                                        speechLauncher.launch(voiceIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice input not supported", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Dictation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    if (searchQuery.isNotBlank()) {
                        item {
                            Button(
                                onClick = {
                                    val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) searchQuery else "https://www.google.com/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}"
                                    onNavigate(dest)
                                    showSearchDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go / Search Direct")
                            }
                        }
                    }
                    
                    item {
                        ListHeader {
                            Text("Engines")
                        }
                    }
                    
                    val engines = listOf(
                        "Google" to "https://www.google.com/search?q=",
                        "Wikipedia" to "https://en.wikipedia.org/w/index.php?search=",
                        "Baidu" to "https://www.baidu.com/s?wd=",
                        "YouTube" to "https://www.youtube.com/results?search_query="
                    )
                    
                    engines.forEach { (name, baseUrl) ->
                        item {
                            Button(
                                onClick = {
                                    val q = if (searchQuery.isNotBlank()) searchQuery else "Wear OS"
                                    onNavigate(baseUrl + URLEncoder.encode(q, "UTF-8"))
                                    showSearchDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (searchQuery.isNotBlank()) "Search $name" else name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        IconButton(onClick = { showSearchDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    }
                }
            }
        }
    }

    // QR CODE DIALOG (SEND TO PHONE)
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrListState = rememberScalingLazyListState()
                val qrFocusRequester = remember { FocusRequester() }
                
                LaunchedEffect(Unit) {
                    qrFocusRequester.requestFocus()
                }
                
                ScalingLazyColumn(
                    state = qrListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(qrFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                qrListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        ListHeader {
                            Text("Open on Phone", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    item {
                        Text(
                            "Scan QR with your phone to open current page",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    item {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color.White, shape = MaterialTheme.shapes.medium)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val encodedUrl = URLEncoder.encode(currentUrl, "UTF-8")
                            AsyncImage(
                                model = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=$encodedUrl",
                                contentDescription = "QR Code to open current page on phone",
                                modifier = Modifier.size(94.dp)
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    item {
                        Text(
                            currentUrl,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        IconButton(onClick = { showQrDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close QR")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.wear.compose.material3.ExperimentalWearMaterial3Api::class)
@Composable
fun HomeScreen(
    bookmarks: List<com.example.data.Bookmark>,
    history: List<com.example.data.HistoryEntry>,
    onNavigate: (String) -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            listState.scrollBy(it.verticalScrollPixels)
                        }
                        true
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ListHeader {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Wear Browser", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                item {
                    Card(
                        onClick = { showSearchDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search icon",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Search or Enter URL",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    ListHeader {
                        Text("Quick Links")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { onNavigate("https://www.google.com") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Google", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { onNavigate("https://en.wikipedia.org") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Wikipedia", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(
                            onClick = { onNavigate("https://weather.com") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = "Weather", tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }

                if (bookmarks.isNotEmpty()) {
                    item {
                        ListHeader {
                            Text("Bookmarks")
                        }
                    }
                    items(bookmarks.take(3)) { bookmark ->
                        Card(
                            onClick = { onNavigate(bookmark.url) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column {
                                Text(bookmark.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(bookmark.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    item {
                        ListHeader {
                            Text("Recent History")
                        }
                    }
                    items(history.take(3)) { entry ->
                        Card(
                            onClick = { onNavigate(entry.url) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column {
                                Text(entry.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                                Text(entry.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            TimeText {
                time()
            }
            ScrollIndicator(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val searchListState = rememberScalingLazyListState()
                val searchFocusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    searchFocusRequester.requestFocus()
                }

                ScalingLazyColumn(
                    state = searchListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(searchFocusRequester)
                        .focusable()
                        .onRotaryScrollEvent {
                            coroutineScope.launch {
                                searchListState.scrollBy(it.verticalScrollPixels)
                            }
                            true
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        ListHeader {
                            Text("Search & URL", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) {
                                                if (searchQuery.startsWith("http")) searchQuery else "https://$searchQuery"
                                            } else {
                                                "https://www.google.com/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}"
                                            }
                                            onNavigate(dest)
                                            showSearchDialog = false
                                        }
                                    }
                                )
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    val dest = if (searchQuery.contains(".") && !searchQuery.contains(" ")) {
                                        if (searchQuery.startsWith("http")) searchQuery else "https://$searchQuery"
                                    } else {
                                        "https://www.google.com/search?q=${URLEncoder.encode(searchQuery, "UTF-8")}"
                                    }
                                    onNavigate(dest)
                                    showSearchDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Go", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
