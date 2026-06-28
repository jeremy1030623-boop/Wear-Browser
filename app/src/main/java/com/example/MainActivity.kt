package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.WearAppTheme
import java.io.File

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Proactively create WebView Code Cache directories to prevent Chromium startup errors
    try {
      val webViewCacheJs = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
      if (!webViewCacheJs.exists()) {
        webViewCacheJs.mkdirs()
      }
      val webViewCacheWasm = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
      if (!webViewCacheWasm.exists()) {
        webViewCacheWasm.mkdirs()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    enableEdgeToEdge()
    setContent {
      WearAppTheme {
        val viewModel: BrowserViewModel = viewModel()
        BrowserScreen(viewModel = viewModel)
      }
    }
  }
}
