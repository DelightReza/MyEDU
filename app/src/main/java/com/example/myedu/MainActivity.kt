package com.example.myedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var logText by mutableStateOf("System Ready.\n")

    fun appendLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        logText += "[$time] $msg\n"
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            appendLog("--- STARTING LOGIN ---")
            appendLog("Target: $email")
            
            try {
                // 1. LOGIN REQUEST
                appendLog("Sending POST request...")
                val loginResp = withContext(Dispatchers.IO) {
                    NetworkClient.api.login(LoginRequest(email, pass))
                }
                appendLog("Response Status: ${loginResp.status}")
                
                val token = loginResp.authorisation?.token
                if (token == null) {
                    appendLog("FAILURE: Token is null!")
                    return@launch
                }
                appendLog("Token received (Length: ${token.length})")
                
                // 2. PROFILE REQUEST
                appendLog("Fetching Profile (studentinfo)...")
                val bearer = "Bearer $token"
                
                val rawJson = withContext(Dispatchers.IO) {
                    // Read string on IO thread to prevent UI freeze
                    NetworkClient.api.getStudentInfo(bearer).string()
                }
                
                appendLog("Profile Data Downloaded: ${rawJson.length} chars")
                // Log first 100 chars to verify content
                appendLog("Snippet: ${rawJson.take(100)}...")

                // 3. PARSE
                val jsonObj = JSONObject(rawJson)
                val movement = jsonObj.optJSONObject("studentMovement")
                val group = movement?.optString("avn_group_name")
                
                appendLog("SUCCESS! Group: $group")
                
            } catch (e: Exception) {
                appendLog("CRITICAL ERROR: ${e.javaClass.simpleName}")
                appendLog("Msg: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
                appendLog("--- OPERATION END ---")
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            MaterialTheme { 
                LoggerScreen() 
            } 
        }
    }
}

@Composable
fun LoggerScreen(vm: MainViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        
        Text("DIAGNOSTIC MODE V6", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // INPUTS
        var e by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = e, 
                onValueChange = { e = it }, 
                label = { Text("Email") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Green,
                    unfocusedBorderColor = Color.Gray
                )
            )
            OutlinedTextField(
                value = p, 
                onValueChange = { p = it }, 
                label = { Text("Pass") },
                modifier = Modifier.weight(1f),
                 colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Green,
                    unfocusedBorderColor = Color.Gray
                )
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = { vm.login(e, p) }, 
            enabled = !vm.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.isLoading) "RUNNING..." else "START TEST")
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = Color.DarkGray)
        
        // THE LOG CONSOLE
        val scroll = rememberScrollState()
        
        // Auto-scroll to bottom when log changes
        LaunchedEffect(vm.logText) {
            scroll.animateScrollTo(scroll.maxValue)
        }

        Text(
            text = vm.logText,
            color = Color.Green,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        )
    }
}
