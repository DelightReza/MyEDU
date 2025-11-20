package com.example.myedu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
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
    var status by mutableStateOf("Ready")
    var userName by mutableStateOf("")
    var scanResults by mutableStateOf("")
    
    // GHOST CONTROLS
    var ghostUrl by mutableStateOf("https://myedu.oshsu.kg/#/login")
    var triggerInjection by mutableStateOf(false)
    var emailInput by mutableStateOf("")
    var passInput by mutableStateOf("")

    // STEP 1: User Clicks Login (Native UI)
    fun attemptNativeLogin(e: String, p: String) {
        emailInput = e
        passInput = p
        status = "Authenticating (Ghost Engine)..."
        triggerInjection = true // Wakes up the hidden browser
    }

    // STEP 2: Hidden Browser Succeeds
    fun onGhostLoginSuccess(cookies: String, ua: String, context: Context) {
        if (appState == "DASHBOARD") return
        
        status = "Session Captured. Verifying..."
        TokenStore.cookies = cookies
        TokenStore.userAgent = ua
        
        // Save session for next time
        val prefs = context.getSharedPreferences("MyEduPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("cookies", cookies).putString("ua", ua).apply()
        
        verifySession()
    }

    // STEP 3: Verify Session with API
    fun checkSavedSession(context: Context) {
        val prefs = context.getSharedPreferences("MyEduPrefs", Context.MODE_PRIVATE)
        val c = prefs.getString("cookies", null)
        val u = prefs.getString("ua", null)
        
        if (c != null && u != null) {
            TokenStore.cookies = c
            TokenStore.userAgent = u
            verifySession()
        }
    }

    private fun verifySession() {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) { NetworkClient.api.getUser().string() }
                val json = JSONObject(raw)
                val user = json.optJSONObject("user")
                if (user != null) {
                    userName = user.optString("name", "Student")
                    appState = "DASHBOARD"
                } else {
                    status = "Session Expired."
                    appState = "LOGIN"
                }
            } catch (e: Exception) {
                status = "Login Error: ${e.message}"
                appState = "LOGIN"
            }
        }
    }

    // STEP 4: Grade Scanner
    fun scanForGrades() {
        viewModelScope.launch {
            scanResults = "Scanning..."
            var log = ""
            
            log += checkEndpoint("studentSession") { NetworkClient.api.scanSession() }
            log += checkEndpoint("studentCurricula") { NetworkClient.api.scanCurricula() }
            log += checkEndpoint("student_mark_list") { NetworkClient.api.scanMarkList() }
            log += checkEndpoint("student/transcript") { NetworkClient.api.scanTranscript() }
            
            scanResults = log
        }
    }

    private suspend fun checkEndpoint(name: String, call: suspend () -> ResponseBody): String {
        return try {
            val res = withContext(Dispatchers.IO) { call().string() }
            if (res.length > 50 && res.contains("{")) "✅ $name: OK (${res.length}B)\n" 
            else "❌ $name: Empty\n"
        } catch (e: Exception) { "❌ $name: Error\n" }
    }
    
    fun logout(context: Context) {
        context.getSharedPreferences("MyEduPrefs", Context.MODE_PRIVATE).edit().clear().apply()
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
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.checkSavedSession(context) }

    // THE INVISIBLE GHOST BROWSER (Size 0dp)
    Box(Modifier.size(0.dp)) { 
        GhostWebView(vm, context)
    }

    if (vm.appState == "LOGIN") {
        LoginScreen(vm)
    } else {
        DashboardScreen(vm, context)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhostWebView(vm: MainViewModel, context: Context) {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url != null && url.contains("/main")) {
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (cookies != null && cookies.contains("myedu-jwt-token")) {
                            vm.onGhostLoginSuccess(cookies, settings.userAgentString, context)
                        }
                    }
                    // Inject JS to fill password fields
                    if (url != null && url.contains("login") && vm.triggerInjection) {
                        Log.d("GHOST", "Injecting Credentials...")
                        val js = """
                            var inputs = document.querySelectorAll('input');
                            var btn = document.querySelector('button');
                            if(inputs.length >= 2) {
                                inputs[0].value = '${vm.emailInput}';
                                inputs[0].dispatchEvent(new Event('input'));
                                inputs[1].value = '${vm.passInput}';
                                inputs[1].dispatchEvent(new Event('input'));
                                setTimeout(function(){ 
                                    if(btn) btn.click(); 
                                }, 500);
                            }
                        """
                        view?.evaluateJavascript(js, null)
                        vm.triggerInjection = false 
                    }
                }
            }
            loadUrl(vm.ghostUrl)
        }
    }, update = { view ->
        if (vm.triggerInjection) {
            view.reload() 
        }
    })
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MyEDU Native", style = MaterialTheme.typography.displaySmall, color = Color(0xFF1565C0))
        Spacer(Modifier.height(32.dp))
        
        var e by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        
        OutlinedTextField(value = e, onValueChange = { e = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { vm.attemptNativeLogin(e, p) }, 
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("LOGIN")
        }
        Spacer(Modifier.height(16.dp))
        Text(vm.status, color = Color.Gray)
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel, context: Context) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Welcome, ${vm.userName}", style = MaterialTheme.typography.headlineSmall)
        Divider(Modifier.padding(vertical = 16.dp))
        
        Button(onClick = { vm.scanForGrades() }, modifier = Modifier.fillMaxWidth()) {
            Text("SCAN SERVER FOR GRADES")
        }
        
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF212121))) {
            Text(vm.scanResults, color = Color.Green, modifier = Modifier.padding(16.dp))
        }
        
        Button(onClick = { vm.logout(context) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.fillMaxWidth()) {
            Text("LOGOUT")
        }
    }
}
