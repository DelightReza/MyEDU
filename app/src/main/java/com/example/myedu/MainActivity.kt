package com.example.myedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage

class MainViewModel : ViewModel() {
    var appState by mutableStateOf("LOGIN")
    var isLoading by mutableStateOf(false)
    var statusMsg by mutableStateOf("Ready (Windows Mode)")
    var profileUrl by mutableStateOf<String?>(null)

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            statusMsg = "Handshaking..."
            
            try {
                val resp = withContext(Dispatchers.IO) {
                    NetworkClient.api.login(LoginRequest(email, pass))
                }
                
                val token = resp.authorisation?.token
                if (token != null) {
                    NetworkClient.interceptor.authToken = token
                    statusMsg = "Authorized. Fetching Profile..."
                    fetchProfile()
                } else {
                    statusMsg = "Login Failed: Server rejected credentials."
                }
                
            } catch (e: Exception) {
                statusMsg = "Error: ${e.message}"
                if (e.message?.contains("401") == true) {
                    statusMsg = "401: Headers Rejected (Native Blocked)"
                }
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun fetchProfile() {
        try {
            val profile = withContext(Dispatchers.IO) {
                NetworkClient.api.getProfile()
            }
            profileUrl = profile.avatar
            appState = "PROFILE"
        } catch (e: Exception) {
            statusMsg = "Profile Fetch Failed: ${e.message}"
        }
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
        LoginScreen(vm)
    } else {
        ProfileScreen(vm)
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("MyEDU Windows Mode", style = MaterialTheme.typography.displaySmall, color = Color(0xFF0078D7))
        Spacer(Modifier.height(32.dp))
        
        var e by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        
        OutlinedTextField(value = e, onValueChange = { e = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { vm.login(e, p) }, 
            enabled = !vm.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D7))
        ) {
            Text(if (vm.isLoading) "Connecting..." else "Sign In")
        }
        
        Spacer(Modifier.height(16.dp))
        Text(vm.statusMsg, color = if (vm.statusMsg.contains("Error") || vm.statusMsg.contains("401")) Color.Red else Color.Gray)
    }
}

@Composable
fun ProfileScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Welcome Back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        
        AsyncImage(
            model = vm.profileUrl,
            contentDescription = "Avatar",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        Text("Native Login Successful!", color = Color.Green)
    }
}
