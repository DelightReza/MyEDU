package myedu.oshsu.kg

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDocumentScreen(url: String, title: String, fileName: String, authToken: String?, themeMode: String, onClose: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(authToken) {
        if (authToken != null) {
            val cookieManager = CookieManager.getInstance(); cookieManager.setAcceptCookie(true)
            cookieManager.setCookie("https://myedu.oshsu.kg", "myedu-jwt-token=$authToken; Domain=myedu.oshsu.kg; Path=/;"); cookieManager.flush()
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back)) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = if (themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface, navigationIconContentColor = if (themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface)) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    
                    setDownloadListener { url, userAgent, _, mimetype, _ ->
                        try {
                            DebugLogger.log("WEB_DL", "Downloading: $fileName")
                            val request = DownloadManager.Request(Uri.parse(url)).setMimeType(mimetype).addRequestHeader("cookie", CookieManager.getInstance().getCookie(url)).addRequestHeader("User-Agent", userAgent)
                            request.setDescription(ctx.getString(R.string.status_download_desc, title)).setTitle(fileName).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                            Toast.makeText(ctx, ctx.getString(R.string.status_downloading, fileName), Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { 
                            DebugLogger.log("WEB_ERR", "Download failed: ${e.message}")
                            Toast.makeText(ctx, ctx.getString(R.string.status_download_failed, e.message), Toast.LENGTH_LONG).show() 
                        }
                    }
                    
                    // --- WEB SPY ADDITION ---
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                            // Log WebView console messages to our Debug Overlay
                            DebugLogger.log("WEB_JS", "${cm.message()} (L${cm.lineNumber()})")
                            return true
                        }
                    }
                    
                    webViewClient = object : WebViewClient() { 
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { 
                            isLoading = true
                            DebugLogger.log("WEB", "Loading: $url")
                        }
                        override fun onPageFinished(view: WebView?, url: String?) { 
                            isLoading = false
                            DebugLogger.log("WEB", "Finished: $url")
                        } 
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            DebugLogger.log("WEB_ERR", "Code: ${error?.errorCode} Desc: ${error?.description}")
                        }
                    }
                    loadUrl(url)
                }
            }, modifier = Modifier.fillMaxSize())
            if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
