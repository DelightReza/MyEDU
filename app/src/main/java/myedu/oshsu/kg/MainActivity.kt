package myedu.oshsu.kg

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import myedu.oshsu.kg.ui.components.ThemedBackground
import myedu.oshsu.kg.ui.screens.*
import myedu.oshsu.kg.ui.theme.GlassWhite
import myedu.oshsu.kg.ui.theme.MyEduTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent { 
            val vm: MainViewModel = viewModel()
            val context = LocalContext.current
            
            LaunchedEffect(Unit) { vm.initSession(context) }
            
            LaunchedEffect(vm.fullSchedule, vm.timeMap) {
                if (vm.fullSchedule.isNotEmpty() && vm.timeMap.isNotEmpty()) {
                    ScheduleAlarmManager(context).scheduleNotifications(vm.fullSchedule, vm.timeMap)
                }
            }

            MyEduTheme(themeMode = vm.themeMode) { 
                ThemedBackground(isGlass = vm.isGlass) {
                    AppContent(vm) 
                }
            } 
        }
    }
}

@Composable
fun AppContent(vm: MainViewModel) {
    AnimatedContent(targetState = vm.appState, label = "Root") { state ->
        when (state) {
            "LOGIN" -> LoginScreen(vm)
            "APP" -> MainAppStructure(vm)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(vm: MainViewModel) {
    BackHandler(enabled = vm.selectedClass != null || vm.showTranscriptScreen || vm.showReferenceScreen || vm.showSettingsScreen || vm.webDocumentUrl != null) { 
        when {
            vm.webDocumentUrl != null -> vm.webDocumentUrl = null
            vm.selectedClass != null -> vm.selectedClass = null
            vm.showTranscriptScreen -> vm.showTranscriptScreen = false
            vm.showReferenceScreen -> vm.showReferenceScreen = false
            vm.showSettingsScreen -> vm.showSettingsScreen = false
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent, 
        bottomBar = {
            if (vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && !vm.showSettingsScreen && vm.webDocumentUrl == null) {
                if (vm.isGlass) {
                    NavigationBar(
                        containerColor = Color(0xFF0F2027).copy(alpha = 0.8f),
                        contentColor = Color.White
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00C6FF),
                            selectedTextColor = Color(0xFF00C6FF),
                            unselectedIconColor = Color.White.copy(0.7f),
                            unselectedTextColor = Color.White.copy(0.7f),
                            indicatorColor = GlassWhite
                        )
                        NavItems(vm, itemColors)
                    }
                } else {
                    NavigationBar { NavItems(vm, NavigationBarItemDefaults.colors()) }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && !vm.showSettingsScreen && vm.webDocumentUrl == null) {
                when(vm.currentTab) {
                    0 -> HomeScreen(vm)
                    1 -> ScheduleScreen(vm)
                    2 -> GradesScreen(vm)
                    3 -> ProfileScreen(vm)
                }
            }
            AnimatedVisibility(visible = vm.showTranscriptScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(vm.isGlass) { TranscriptView(vm) { vm.showTranscriptScreen = false } } 
            }
            AnimatedVisibility(visible = vm.showReferenceScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(vm.isGlass) { ReferenceView(vm) { vm.showReferenceScreen = false } } 
            }
            // Removed old full-screen detail transition in favor of BottomSheet logic below
            // But keeping logic to nullify selectedClass if back is pressed on sheet (handled by ModalBottomSheet onDismiss)
            
            AnimatedVisibility(visible = vm.showSettingsScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(vm.isGlass) { SettingsScreen(vm) { vm.showSettingsScreen = false } } 
            }
            
            if (vm.webDocumentUrl != null) {
                WebDocumentScreen(
                    url = vm.webDocumentUrl!!,
                    title = "Document",
                    authToken = vm.getAuthToken(),
                    onClose = { vm.webDocumentUrl = null }
                )
            }

            // --- POPUP: CLASS DETAILS BOTTOM SHEET ---
            if (vm.selectedClass != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { vm.selectedClass = null },
                    sheetState = sheetState,
                    containerColor = if (vm.isGlass) Color(0xFF0F2027) else MaterialTheme.colorScheme.surface,
                    contentColor = if (vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = if(vm.isGlass) Color.White.copy(0.5f) else MaterialTheme.colorScheme.onSurfaceVariant) }
                ) {
                    vm.selectedClass?.let { ClassDetailsSheet(vm, it) }
                }
            }
        }
    }
}

@Composable
fun RowScope.NavItems(vm: MainViewModel, colors: NavigationBarItemColors) {
    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = vm.currentTab == 0, onClick = { vm.currentTab = 0 }, colors = colors)
    NavigationBarItem(icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Schedule") }, selected = vm.currentTab == 1, onClick = { vm.currentTab = 1 }, colors = colors)
    NavigationBarItem(icon = { Icon(Icons.Default.Description, null) }, label = { Text("Grades") }, selected = vm.currentTab == 2, onClick = { vm.currentTab = 2 }, colors = colors)
    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }, selected = vm.currentTab == 3, onClick = { vm.currentTab = 3 }, colors = colors)
}
