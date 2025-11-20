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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.ResponseBody

class MainViewModel : ViewModel() {
    var appState by mutableStateOf("LOGIN") 
    var userName by mutableStateOf("")
    var scanLog by mutableStateOf("Ready to scan.")

    // 1. Called when WebView detects login
    fun onSessionCaptured(cookies: String, ua: String) {
        if (appState == "DASHBOARD") return
        
        SessionStore.cookies = cookies
        SessionStore.userAgent = ua
        
        verifySession()
    }

    // 2. Test the captured session
    private fun verifySession() {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) { NetworkClient.api.getUser().string() }
                val json = JSONObject(raw)
                val user = json.optJSONObject("user")
                
                if (user != null) {
                    userName = user.optString("name", "Student")
                    appState = "DASHBOARD"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // 3. Scanner to find the grades
    fun scanGrades() {
        viewModelScope.launch {
            scanLog = "Scanning..."
            var log = ""
            
            log += checkEndpoint("studentSession") { NetworkClient.api.scanSession() }
            log += checkEndpoint("studentCurricula") { NetworkClient.api.scanCurricula() }
            log += checkEndpoint("student_mark_list") { NetworkClient.api.scanMarkList() }
            log += checkEndpoint("transcript") { NetworkClient.api.scanTranscript() }
            log += checkEndpoint("payStatus") { NetworkClient.api.scanPayStatus() }
            
            scanLog = log
        }
    }

    private suspend fun checkEndpoint(name: String, call: suspend () -> ResponseBody): String {
        return try {
            val res = withContext(Dispatchers.IO) { call().string() }
            if (res.contains("{")) "✅ $name: FOUND! (${res.length}B)\n" 
            else "❌ $name: Empty\n"
        } catch (e: Exception) { "❌ $name: ${e.message}\n" }
    }
    
    fun logout() {
        CookieManager.getInstance().removeAllCookies(null)
        appState = "LOGIN"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppContent() } }
    }
}

@Composable
fun AppContent(vm: MainViewModel = viewModel()) {
    if (vm.appState == "LOGIN") {
        LoginWebView(vm)
    } else {
        DashboardScreen(vm)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebView(vm: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Use the User-Agent that worked for you
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (cookies != null && cookies.contains("myedu-jwt-token")) {
                            vm.onSessionCaptured(cookies, settings.userAgentString)
                        }
                    }
                }
                loadUrl("https://myedu.oshsu.kg/#/login")
            }
        }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, ${vm.userName}", style = MaterialTheme.typography.headlineSmall)
        Divider(Modifier.padding(vertical = 16.dp))
        
        Button(onClick = { vm.scanGrades() }, modifier = Modifier.fillMaxWidth()) {
            Text("FIND GRADES")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Text(vm.scanLog, color = Color.Green, modifier = Modifier.padding(16.dp))
        }
        
        Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth()) {
            Text("LOGOUT")
        }
    }
}
