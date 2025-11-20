package com.example.myedu

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainViewModel : ViewModel() {
    var status by mutableStateOf("Paste your 'myedu-jwt-token' below.")
    var userName by mutableStateOf("")
    var userDetails by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun useToken(token: String) {
        if (token.length < 20) {
            status = "Token is too short!"
            return
        }
        
        TokenStore.manualToken = token
        viewModelScope.launch {
            isLoading = true
            status = "Testing Token..."
            try {
                // 1. Test User API
                val rawJson = withContext(Dispatchers.IO) {
                    NetworkClient.api.getUser().string()
                }
                
                val json = JSONObject(rawJson)
                val user = json.optJSONObject("user")
                
                if (user != null) {
                    userName = user.optString("name", "Unknown")
                    userDetails = "ID: ${user.optString("id_student")} | Email: ${user.optString("email")}"
                    status = "SUCCESS! Access Granted."
                    
                    // 2. (Optional) Try to fetch extra data
                     try {
                        val payJson = withContext(Dispatchers.IO) { NetworkClient.api.getPayStatus().string() }
                        userDetails += "\nPayment Status: ${JSONObject(payJson).optString("total")} KGS"
                    } catch(e: Exception) {}
                    
                } else {
                    status = "Token accepted but User object is missing?"
                }

            } catch (e: Exception) {
                status = "FAILED: ${e.message}\n(Did you copy the full token?)"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { GodModeScreen() } }
    }
}

@Composable
fun GodModeScreen(vm: MainViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("GOD MODE V14", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF6200EE))
        Spacer(Modifier.height(8.dp))
        Text("Bypass the broken login by pasting the browser token directly.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Spacer(Modifier.height(24.dp))
        
        var text by remember { mutableStateOf("") }
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Paste 'myedu-jwt-token'") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            maxLines = 10
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { vm.useToken(text) }, 
            enabled = !vm.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if(vm.isLoading) "VERIFYING..." else "ACCESS SYSTEM")
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (vm.userName.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("WELCOME", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                    Text(vm.userName, style = MaterialTheme.typography.headlineLarge)
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text(vm.userDetails)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text("Status Log:", style = MaterialTheme.typography.labelLarge)
        Text(vm.status, color = if (vm.userName.isNotEmpty()) Color.Green else Color.Red)
    }
}
