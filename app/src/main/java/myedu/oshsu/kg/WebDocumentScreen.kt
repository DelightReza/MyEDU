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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDocumentScreen(url: String, title: String, fileName: String, authToken: String?, themeMode: String, onClose: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    
    // Prevents infinite loops if the site keeps redirecting back to home
    var deepLinkAttempted by remember { mutableStateOf(false) }
    
    // Calculate timestamp for 'my_edu_update' cookie
    val timestampCookieVal = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.format(Date())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back)) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = if (themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface, navigationIconContentColor = if (themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface)) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    
                    // --- COOKIE INJECTION ---
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    if (authToken != null) {
                        val domain = "myedu.oshsu.kg"
                        cookieManager.setCookie(domain, "myedu-jwt-token=$authToken; Domain=$domain; Path=/")
                        cookieManager.setCookie(domain, "my_edu_update=$timestampCookieVal; Domain=$domain; Path=/")
                        cookieManager.setCookie(domain, "have_2fa=yes; Domain=$domain; Path=/")
                        cookieManager.flush()
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    
                    setDownloadListener { downloadUrl, userAgent, _, mimetype, _ ->
                        try {
                            DebugLogger.log("WEB_DL", "Downloading: $fileName")
                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                .setMimeType(mimetype)
                                .addRequestHeader("cookie", CookieManager.getInstance().getCookie(downloadUrl))
                                .addRequestHeader("User-Agent", userAgent)
                                .addRequestHeader("Authorization", "Bearer $authToken")
                            
                            request.setDescription(ctx.getString(R.string.status_download_desc, title))
                                .setTitle(fileName)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            
                            (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                            Toast.makeText(ctx, ctx.getString(R.string.status_downloading, fileName), Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { 
                            DebugLogger.log("WEB_ERR", "Download failed: ${e.message}")
                            Toast.makeText(ctx, ctx.getString(R.string.status_download_failed, e.message), Toast.LENGTH_LONG).show() 
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                            DebugLogger.log("WEB_JS", "${cm.message()} (L${cm.lineNumber()})")
                            return true
                        }
                    }
                    
                    webViewClient = object : WebViewClient() { 
                        override fun onPageStarted(view: WebView?, loadedUrl: String?, favicon: Bitmap?) { 
                            isLoading = true
                            DebugLogger.log("WEB", "Loading: $loadedUrl")
                            if (authToken != null) {
                                view?.evaluateJavascript("localStorage.setItem('token', '$authToken');", null)
                            }
                        }
                        
                        override fun onPageFinished(view: WebView?, loadedUrl: String?) { 
                            isLoading = false
                            DebugLogger.log("WEB", "Finished: $loadedUrl")

                            // --- SPA DEEP LINK FIX ---
                            // If we intended to go to a hash URL (e.g. #/Transcript) 
                            // but landed on root (e.g. #/ or /), verify session is active then redirect.
                            if (!deepLinkAttempted && url.contains("#") && loadedUrl != null) {
                                val targetHash = url.substringAfter("#")
                                val currentHash = loadedUrl.substringAfter("#", "")
                                
                                // Check if we are at root ("/" or "/#/") AND specifically missing our target hash
                                val isRoot = loadedUrl.endsWith("/") || loadedUrl.endsWith("/#/")
                                
                                if (isRoot && currentHash != targetHash) {
                                    DebugLogger.log("WEB_FIX", "Landed on Home. Forcing nav to: $targetHash")
                                    deepLinkAttempted = true // Mark as attempted so we don't loop forever
                                    
                                    // Use JS to navigate. This works better for SPAs than loadUrl() 
                                    // because it triggers the router without a full reload.
                                    view?.evaluateJavascript("window.location.href = '$url';", null)
                                }
                            }
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
