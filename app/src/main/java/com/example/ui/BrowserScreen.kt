package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.TimeText

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isDeepMode by viewModel.isDeepMode.collectAsState()
    val isPowerSavingMode by viewModel.isPowerSavingMode.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showMenu) {
            BookmarkMenu(
                bookmarks = bookmarks,
                onNavigate = { url ->
                    viewModel.navigateTo(url)
                    showMenu = false
                },
                onClose = { showMenu = false },
                onToggleDeepMode = { viewModel.toggleDeepMode() },
                isDeepMode = isDeepMode
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDeepMode) Color.Black else MaterialTheme.colorScheme.background)
            ) {
                WebViewComponent(
                    url = currentUrl,
                    isPowerSaving = isPowerSavingMode,
                    onPageStarted = { isLoading = true },
                    onPageFinished = { title ->
                        isLoading = false
                        pageTitle = title ?: ""
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
    onPageStarted: () -> Unit,
    onPageFinished: (String?) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
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
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = if (isPowerSaving) WebSettings.LOAD_CACHE_ONLY else WebSettings.LOAD_CACHE_ELSE_NETWORK
                }
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun BookmarkMenu(
    bookmarks: List<com.example.data.Bookmark>,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onToggleDeepMode: () -> Unit,
    isDeepMode: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    
    ScalingLazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        item {
            ListHeader {
                Text("Settings")
            }
        }
        
        item {
            Button(
                onClick = onToggleDeepMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isDeepMode) "Disable Deep Mode" else "Enable Deep Mode")
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
            Button(
                onClick = { onNavigate(bookmark.url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(bookmark.title, style = MaterialTheme.typography.labelMedium)
                    Text(bookmark.url, style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
}
