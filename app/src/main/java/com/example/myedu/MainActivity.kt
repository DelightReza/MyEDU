package com.example.myedu

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainViewModel : ViewModel() {
    var appState by mutableStateOf("LOGIN") // LOGIN, LOADING, DASHBOARD
    var debugLog by mutableStateOf("Waiting for WebView Login...")
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")

    fun onLoginSuccess(cookies: String, ua: String) {
        appState = "LOADING"
        CredentialStore.cookies = cookies
        CredentialStore.userAgent = ua
        debugLog = "Cookies Hijacked! Testing API..."
        
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch {
            try {
                val rawJson = withContext(Dispatchers.IO) {
                    NetworkClient.api.getUser().string()
                }
                debugLog = "API SUCCESS: 200 OK"
                
                val json = JSONObject(rawJson)
                val user = json.optJSONObject("user")
                userName = user?.optString("name") ?: "Unknown"
                userEmail = user?.optString("email") ?: ""
                
                appState = "DASHBOARD"
            } catch (e: Exception) {
                debugLog = "API Error: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { HybridApp() } }
    }
}

@Composable
fun HybridApp(vm: MainViewModel = viewModel()) {
    when (vm.appState) {
        "LOGIN" -> WebViewLoginScreen(vm)
        "LOADING" -> LoadingScreen(vm)
        "DASHBOARD" -> DashboardScreen(vm)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewLoginScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("MyEDU Hybrid Login", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            // Check if we have reached the main page
                            if (url != null && url.contains("/main")) {
                                val cookies = CookieManager.getInstance().getCookie(url)
                                val ua = settings.userAgentString
                                if (cookies != null && cookies.contains("myedu-jwt-token")) {
                                    vm.onLoginSuccess(cookies, ua)
                                }
                            }
                        }
                    }
                    loadUrl("https://myedu.oshsu.kg/#/login")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LoadingScreen(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(vm.debugLog, color = Color.Blue)
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome Back!", style = MaterialTheme.typography.displaySmall, color = Color(0xFF1565C0))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(vm.userName, style = MaterialTheme.typography.headlineSmall)
                Text(vm.userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("Status: Authenticated via WebView", color = Color.Green)
            }
        }
        
        Button(onClick = { /* TODO: Scan Grades */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Grades (Coming Soon)")
        }
    }
}
