package com.example.myedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

// Simple holder for UI data
data class UIProfile(val name: String, val group: String, val avatar: String)

class MainViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)
    var profile by mutableStateOf<UIProfile?>(null)
    var logs by mutableStateOf("Ready.")

    fun log(msg: String) { logs = "\n" }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            log("--- Login Start ---")
            try {
                val response = NetworkClient.api.login(LoginRequest(email, pass))
                val t = response.authorisation?.token
                if (t != null) {
                    token = "Bearer "
                    log("Token OK. Fetching Profile...")
                    fetchProfile()
                } else {
                    log("Login Failed: No token.")
                }
            } catch (e: Exception) {
                log("Login Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                // 1. Get Raw Text
                val rawResponse = NetworkClient.api.getStudentInfo(token!!).string()
                log("Raw Data Length: ${rawResponse.length}")
                
                // 2. Manual Parsing (Safer)
                val json = JSONObject(rawResponse)
                val avatar = json.optString("avatar", "")
                
                val movement = json.optJSONObject("studentMovement")
                val group = movement?.optString("avn_group_name") ?: "Unknown Group"
                
                val spec = movement?.optJSONObject("speciality")
                val name = spec?.optString("name_en") ?: "Unknown Major"

                profile = UIProfile(name, group, avatar)
                log("Parsed:  / ")
                
            } catch (e: Exception) {
                log("Profile Parse Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { OshSuApp() } }
    }
}

@Composable
fun OshSuApp(vm: MainViewModel = viewModel()) {
    Column(Modifier.fillMaxSize()) {
        if (vm.token == null) {
            LoginScreen(vm)
        } else {
            ProfileScreen(vm)
        }
        
        // CONSOLE
        Divider(color = Color.Red, thickness = 2.dp)
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black).padding(8.dp)) {
            val scroll = rememberScrollState()
            Text(vm.logs, color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.verticalScroll(scroll))
        }
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MyEDU V5 (Raw Mode)", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        var e by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        OutlinedTextField(value = e, onValueChange = { e = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.login(e, p) }, enabled = !vm.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(if (vm.isLoading) "..." else "Login")
        }
    }
}

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val p = vm.profile
    if (p == null) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = p.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(p.group, style = MaterialTheme.typography.bodyLarge, color = Color.Blue)
                }
            }
        }
    }
}
