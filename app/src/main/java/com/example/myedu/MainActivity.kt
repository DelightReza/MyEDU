package com.example.myedu

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke // <--- FIXED: Added missing import
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- MOCK DATA ---
data class ClassSession(val time: String, val subject: String, val room: String, val type: String)

// --- THEME ENGINE ---
@Composable
fun MyEduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Dynamic Colors (Material You)
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

// --- VIEWMODEL ---
class MainViewModel : ViewModel() {
    var appState by mutableStateOf("LOGIN")
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    
    val todayClasses = listOf(
        ClassSession("08:30", "Internal Medicine", "Lec Hall 1", "Lecture"),
        ClassSession("10:00", "Clinical Surgery", "Room 304", "Seminar"),
        ClassSession("13:00", "Pediatrics", "Clinic 2", "Practice")
    )
    val mockGpa = "3.82"
    val currentSemester = "9"

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = null
            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(email, pass)) }
                val token = resp.authorisation?.token
                
                if (token != null) {
                    NetworkClient.interceptor.authToken = token
                    userData = withContext(Dispatchers.IO) { NetworkClient.api.getUser().user }
                    profileData = withContext(Dispatchers.IO) { NetworkClient.api.getProfile() }
                    appState = "APP"
                } else {
                    errorMsg = "Incorrect credentials"
                }
            } catch (e: Exception) {
                errorMsg = if(e.message?.contains("401") == true) "Access Denied" else "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    fun logout() {
        appState = "LOGIN"
        currentTab = 0
        userData = null
        profileData = null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { MyEduTheme { AppContent() } }
    }
}

@Composable
fun AppContent(vm: MainViewModel = viewModel()) {
    AnimatedContent(
        targetState = vm.appState,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
        label = "RootNav"
    ) { state ->
        if (state == "LOGIN") LoginScreen(vm) else MainAppStructure(vm)
    }
}

@Composable
fun MainAppStructure(vm: MainViewModel) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = vm.currentTab == 0,
                    onClick = { vm.currentTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") },
                    selected = vm.currentTab == 1,
                    onClick = { vm.currentTab = 1 }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (vm.currentTab == 0) HomeScreen(vm) else ProfileScreen(vm)
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val user = vm.userData
    val profile = vm.profileData
    val scrollState = rememberScrollState()
    
    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Good Morning,", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Text(user?.name ?: "Student", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        
        Spacer(Modifier.height(24.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(modifier = Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text("Semester", style = MaterialTheme.typography.labelMedium)
                    Text(vm.currentSemester, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            ElevatedCard(modifier = Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.TrendingUp, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text("Avg. GPA", style = MaterialTheme.typography.labelMedium)
                    Text(vm.mockGpa, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // MODERN M3 CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // Fixed Import
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Status: Active Student", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(profile?.studentMovement?.avn_group_name ?: "...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Today's Classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        vm.todayClasses.forEach { session ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(session.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(session.subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("${session.room} • ${session.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val user = vm.userData
    val profile = vm.profileData
    val fullName = "${user?.last_name ?: ""} ${user?.name ?: ""}".trim().ifEmpty { "Student" }
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        
        // Avatar
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)), CircleShape).padding(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background)) {
            AsyncImage(model = profile?.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        }
        
        Spacer(Modifier.height(16.dp))
        Text(fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        
        Spacer(Modifier.height(32.dp))
        
        SectionHeader("Academic")
        DetailCard(Icons.Outlined.School, "Faculty", profile?.studentMovement?.faculty?.get() ?: "-")
        DetailCard(Icons.Outlined.Book, "Speciality", profile?.studentMovement?.speciality?.get() ?: "-")
        DetailCard(Icons.Outlined.Groups, "Group", profile?.studentMovement?.avn_group_name ?: "-")
        DetailCard(Icons.Outlined.CastForEducation, "Form", profile?.studentMovement?.edu_form?.get() ?: "-")

        Spacer(Modifier.height(24.dp))
        SectionHeader("Personal")
        DetailCard(Icons.Outlined.Badge, "Passport/PIN", profile?.pdsstudentinfo?.passport_number ?: "-")
        DetailCard(Icons.Outlined.Cake, "Birthday", profile?.pdsstudentinfo?.birthday ?: "-")
        DetailCard(Icons.Outlined.Phone, "Phone", profile?.pdsstudentinfo?.phone ?: "-")
        DetailCard(Icons.Outlined.Home, "Address", profile?.pdsstudentinfo?.address ?: "-")
        
        Spacer(Modifier.height(24.dp))
        SectionHeader("Family")
        DetailCard(Icons.Outlined.Person, "Father", profile?.pdsstudentinfo?.father_full_name ?: "-")
        DetailCard(Icons.Outlined.Person2, "Mother", profile?.pdsstudentinfo?.mother_full_name ?: "-")
        
        Spacer(Modifier.height(32.dp))
        Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) {
            Text("Log Out")
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp))
}

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun LoginScreen(vm: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.School, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("MyEDU", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(48.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())

            if (vm.errorMsg != null) {
                Spacer(Modifier.height(16.dp))
                Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(32.dp))
            Button(onClick = { vm.login(email, pass) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !vm.isLoading) {
                if (vm.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text("Sign In")
            }
        }
    }
}
