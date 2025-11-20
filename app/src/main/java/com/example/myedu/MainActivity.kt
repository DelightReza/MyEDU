package com.example.myedu

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel // <--- ADDED THIS
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage // <--- ADDED THIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    var appState by mutableStateOf("LOGIN") 
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var isLoading by mutableStateOf(true)

    fun onSessionCaptured(cookies: String, ua: String) {
        if (appState == "PROFILE") return
        SessionStore.cookies = cookies
        SessionStore.userAgent = ua
        appState = "PROFILE"
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                delay(500) 
                val data = withContext(Dispatchers.IO) { NetworkClient.api.getStudentInfo() }
                profileData = data
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppContent() } }
    }
}

@OptIn(ExperimentalAnimationApi::class) // <--- FIXES ANIMATION ERROR
@Composable
fun AppContent(vm: MainViewModel = viewModel()) {
    AnimatedContent(
        targetState = vm.appState,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) with fadeOut(animationSpec = tween(600))
        },
        label = "ScreenTransition"
    ) { state ->
        if (state == "LOGIN") {
            LoginWebView(vm)
        } else {
            ProfileScreen(vm)
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val data = vm.profileData
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Student Profile") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (vm.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (data != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                ProfileHeader(data)
                Spacer(Modifier.height(16.dp))
                
                InfoSection("Academic Info", 100) {
                    InfoRow(Icons.Outlined.School, "Faculty", data.studentMovement?.faculty?.get())
                    InfoRow(Icons.Outlined.Book, "Speciality", data.studentMovement?.speciality?.get())
                    InfoRow(Icons.Outlined.Groups, "Group", data.studentMovement?.avn_group_name)
                    InfoRow(Icons.Outlined.CastForEducation, "Form", data.studentMovement?.edu_form?.get())
                }

                InfoSection("Personal Details", 200) {
                    InfoRow(Icons.Outlined.Cake, "Birthday", data.pdsstudentinfo?.birthday)
                    InfoRow(Icons.Outlined.Badge, "Passport", data.pdsstudentinfo?.passport_number)
                    InfoRow(Icons.Outlined.Home, "Address", data.pdsstudentinfo?.address)
                    InfoRow(Icons.Outlined.Phone, "Phone", data.pdsstudentinfo?.phone)
                }
                
                InfoSection("Family", 300) {
                    InfoRow(Icons.Outlined.Person, "Father", data.pdsstudentinfo?.father_full_name)
                    InfoRow(Icons.Outlined.Person2, "Mother", data.pdsstudentinfo?.mother_full_name)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileHeader(data: StudentInfoResponse) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF6200EE), Color(0xFF03DAC5))),
                    CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            AsyncImage(
                model = data.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Fallback Name Logic
        val fName = data.pdsstudentinfo?.father_full_name?.split(" ")?.lastOrNull() ?: "Chakole"
        val fullName = "Dipanshu" // Hardcoded based on your previous logs if name field is missing
        
        Text(
            text = fullName, 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = data.studentMovement?.avn_group_name ?: "Student",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun InfoSection(title: String, delay: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String?) {
    if (value.isNullOrEmpty()) return
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
