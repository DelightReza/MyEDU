package myedu.oshsu.kg

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDocumentScreen(
    url: String, 
    title: String, 
    fileName: String, 
    authToken: String?, 
    loginEmail: String,
    loginPass: String,
    themeMode: String, 
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    
    // Tracks if we have already redirected from the login page to the target document
    var hasRedirectedToTarget by remember { mutableStateOf(false) }
    var loginScriptInjected by remember { mutableStateOf(false) }
    
    // State to track the ID of the file being downloaded by this screen
    var lastDownloadId by remember { mutableStateOf<Long>(-1L) }
    val context = LocalContext.current

    // --- AUTOMATIC FILE OPENER ---
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    
                    if (id != -1L && id == lastDownloadId) {
                        val manager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val query = DownloadManager.Query().setFilterById(id)
                        val cursor = manager.query(query)
                        
                        try {
                            if (cursor.moveToFirst()) {
                                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)) ?: "application/pdf"
                                    
                                    if (uriStr != null) {
                                        val file = File(Uri.parse(uriStr).path!!)
                                        val contentUri = FileProvider.getUriForFile(
                                            ctx,
                                            "${ctx.packageName}.provider",
                                            file
                                        )
                                        
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, mimeType)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        
                                        ctx.startActivity(viewIntent)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, ctx.getString(R.string.error_no_pdf_viewer), Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        } finally {
                            cursor?.close()
                        }
                    }
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context, 
            receiver, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), 
            ContextCompat.RECEIVER_EXPORTED
        )
        
        onDispose { context.unregisterReceiver(receiver) }
    }

    // --- CRASH-PROOF AUTO-LOGIN SCRIPT ---
    val autoLoginScript = remember(loginEmail, loginPass) {
        """
        (function() {
            if (window.isLoginInjected) return;
            window.isLoginInjected = true;
            console.log("Auto-Login: Script started...");

            // Clear old tokens to force login form if page reloads
            localStorage.clear();
            sessionStorage.clear();

            var attempts = 0;
            var maxAttempts = 40; // 20 seconds

            function tryLogin() {
                attempts++;
                
                // 1. Find Inputs
                var emailInput = document.querySelector('input[type="email"]') || 
                                 document.querySelector('input[name="email"]') || 
                                 document.querySelector('input[type="text"]');
                                 
                var passInput = document.querySelector('input[type="password"]') || 
                                document.querySelector('input[name="password"]');
                
                // 2. Find Button
                var btn = Array.from(document.querySelectorAll('button')).find(b => {
                    var t = (b.textContent || b.innerText || "").trim().toLowerCase();
                    return t.includes('войти') || t === 'login' || t === 'sign in';
                }) || document.querySelector('button[type="submit"]');

                if (emailInput && passInput && btn) {
                    console.log("Auto-Login: Form found. Filling credentials...");
                    
                    // --- SAFE INPUT SETTER ---
                    function safeFill(elem, val) {
                        try {
                            // 1. Standard
                            elem.value = val;
                            
                            // 2. Try Prototype Setter (Safe)
                            var proto = window.HTMLInputElement.prototype;
                            if (proto) {
                                var desc = Object.getOwnPropertyDescriptor(proto, 'value');
                                if (desc && desc.set) {
                                    desc.set.call(elem, val);
                                }
                            }
                            
                            // 3. Dispatch Events
                            elem.dispatchEvent(new Event('input', { bubbles: true }));
                            elem.dispatchEvent(new Event('change', { bubbles: true }));
                            elem.dispatchEvent(new Event('blur', { bubbles: true }));
                        } catch(e) {
                            console.log("Fill error: " + e.message);
                        }
                    }

                    safeFill(emailInput, '$loginEmail');
                    safeFill(passInput, '$loginPass');
                    
                    setTimeout(function() { 
                        console.log("Auto-Login: Clicking button...");
                        btn.click(); 
                    }, 800);
                    
                    return;
                }

                if (attempts < maxAttempts) {
                    setTimeout(tryLogin, 500);
                } else {
                    console.log("Auto-Login: Timeout - Form elements not found.");
                }
            }

            tryLogin();
        })();
        """
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { 
            TopAppBar(
                title = { Text(title) }, 
                navigationIcon = { 
                    IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back)) } 
                }, 
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, 
                    titleContentColor = MaterialTheme.colorScheme.onSurface, 
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    
                    // CRITICAL: Ensure we do NOT inject the old token, forcing a fresh login
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()

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
                                
                            request.setDescription(ctx.getString(R.string.status_download_desc, title))
                                .setTitle(fileName)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            lastDownloadId = dm.enqueue(request)
                            
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
                        }
                        
                        override fun onPageFinished(view: WebView?, loadedUrl: String?) { 
                            isLoading = false
                            DebugLogger.log("WEB", "Finished: $loadedUrl")
                            
                            val safeUrl = loadedUrl ?: ""
                            val targetPath = url.substringAfter("#") 

                            // 1. If we are already at the target, stop.
                            if (safeUrl.contains(targetPath, ignoreCase = true)) {
                                DebugLogger.log("WEB", "Reached target.")
                                return
                            }

                            // 2. Login Page Detection (Root)
                            val isLoginPage = safeUrl.endsWith("/#/") || safeUrl.contains("login") || safeUrl.endsWith(".kg/")
                            
                            if (!hasRedirectedToTarget && isLoginPage) {
                                DebugLogger.log("WEB", "Login page detected. Injecting auto-login...")
                                view?.evaluateJavascript(autoLoginScript, null)
                            } 
                            // 3. Redirect Detection
                            else if (!hasRedirectedToTarget && (safeUrl.contains("main") || safeUrl.contains("cabinet") || safeUrl.contains("dashboard"))) {
                                DebugLogger.log("WEB", "Logged in successfully. Redirecting to target: $url")
                                hasRedirectedToTarget = true
                                view?.loadUrl(url)
                            }
                        } 
                        
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            DebugLogger.log("WEB_ERR", "Code: ${error?.errorCode} Desc: ${error?.description}")
                        }
                    }
                    // Start at root to ensure login happens
                    loadUrl("https://myedu.oshsu.kg/")
                }
            }, modifier = Modifier.fillMaxSize())
            if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
