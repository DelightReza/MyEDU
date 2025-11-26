package myedu.oshsu.kg

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDocumentScreen(
    url: String,
    title: String,
    fileName: String, // <--- REPLACES server filename
    authToken: String?,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(authToken) {
        if (authToken != null) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val cookieString = "myedu-jwt-token=$authToken; Domain=myedu.oshsu.kg; Path=/;"
            cookieManager.setCookie("https://myedu.oshsu.kg", cookieString)
            cookieManager.flush()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        // --- DOWNLOAD LISTENER (CUSTOM NAMING) ---
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(url))
                                request.setMimeType(mimetype)
                                
                                val cookies = CookieManager.getInstance().getCookie(url)
                                request.addRequestHeader("cookie", cookies)
                                request.addRequestHeader("User-Agent", userAgent)
                                
                                // FORCE CUSTOM FILENAME
                                request.setDescription("Downloading $title...")
                                request.setTitle(fileName)
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                
                                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(request)
                                
                                Toast.makeText(ctx, "Downloading: $fileName", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {}
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { isLoading = true }
                            override fun onPageFinished(view: WebView?, url: String?) { isLoading = false }
                        }

                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
