package myedu.oshsu.kg

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import myedu.oshsu.kg.ui.components.MyEduPullToRefreshBox
import myedu.oshsu.kg.ui.components.ThemedBackground
import myedu.oshsu.kg.ui.screens.*
import myedu.oshsu.kg.ui.theme.GlassWhite
import myedu.oshsu.kg.ui.theme.MilkyGlass
import myedu.oshsu.kg.ui.theme.MyEduTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    
    // --- NETWORK MONITORING ---
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Notification Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Setup Network Monitoring
        setupNetworkMonitoring()

        // --- SETUP BACKGROUND WORK ---
        setupBackgroundWork()

        setContent { 
            val vm: MainViewModel = viewModel()
            // Store reference for network callback
            mainViewModel = vm
            
            val context = LocalContext.current
            
            // --- 1. HANDLE APP RESUME (AUTO REFRESH) ---
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        vm.onAppResume()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            LaunchedEffect(Unit) { vm.initSession(context) }
            LaunchedEffect(vm.fullSchedule, vm.timeMap) {
                if (vm.fullSchedule.isNotEmpty() && vm.timeMap.isNotEmpty()) {
                    ScheduleAlarmManager(context).scheduleNotifications(vm.fullSchedule, vm.timeMap)
                }
            }
            MyEduTheme(themeMode = vm.themeMode) { 
                ThemedBackground(themeMode = vm.themeMode) { AppContent(vm) }
            } 
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // --- 2. HANDLE NETWORK RECONNECT (AUTO REFRESH) ---
                mainViewModel?.onNetworkAvailable()
            }
        }
        val request = NetworkRequest.Builder().build()
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun setupBackgroundWork() {
        // Constraints: Requires Battery Not Low + Network Connected
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Repeat every 4 hours
        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Enqueue Unique Work (Keeps existing one if already scheduled)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MyEduGradeSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
    }
}

@Composable
fun AppContent(vm: MainViewModel) {
    AnimatedContent(
        targetState = vm.appState, 
        label = "Root",
        transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }
    ) { state ->
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
    
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            
            // --- 3. PULL TO REFRESH WRAPPER ---
            MyEduPullToRefreshBox(
                isRefreshing = vm.isLoading,
                onRefresh = { vm.refresh() },
                themeMode = vm.themeMode
            ) {
                AnimatedContent(
                    targetState = vm.currentTab,
                    label = "TabContent",
                    transitionSpec = { (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.96f, animationSpec = tween(400))).togetherWith(fadeOut(animationSpec = tween(200))) },
                    modifier = Modifier.fillMaxSize()
                ) { targetTab ->
                    when(targetTab) {
                        0 -> HomeScreen(vm)
                        1 -> ScheduleScreen(vm)
                        2 -> GradesScreen(vm)
                        3 -> ProfileScreen(vm)
                    }
                }
            }

            val showNav = vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && !vm.showSettingsScreen && vm.webDocumentUrl == null
            AnimatedVisibility(
                visible = showNav,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp).windowInsetsPadding(WindowInsets.navigationBars)
            ) { FloatingNavBar(vm) }

            AnimatedVisibility(visible = vm.showTranscriptScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(themeMode = vm.themeMode) { TranscriptView(vm) { vm.showTranscriptScreen = false } } 
            }
            AnimatedVisibility(visible = vm.showReferenceScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(themeMode = vm.themeMode) { ReferenceView(vm) { vm.showReferenceScreen = false } } 
            }
            AnimatedVisibility(visible = vm.showSettingsScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(themeMode = vm.themeMode) { SettingsScreen(vm) { vm.showSettingsScreen = false } } 
            }
            
            if (vm.webDocumentUrl != null) {
                val docType = if (vm.webDocumentUrl!!.contains("Transcript", true)) "Transcript" else "Reference"
                val lastName = vm.userData?.last_name ?: ""
                val name = vm.userData?.name ?: ""
                val cleanName = "$lastName $name".trim().replace(" ", "_").replace(".", "")
                val filePrefix = "${cleanName}_$docType.pdf"
                
                ThemedBackground(themeMode = vm.themeMode) {
                    WebDocumentScreen(
                        url = vm.webDocumentUrl!!, 
                        title = docType, 
                        fileName = filePrefix, 
                        authToken = vm.getAuthToken(),
                        themeMode = vm.themeMode, 
                        onClose = { vm.webDocumentUrl = null }
                    )
                }
            }

            if (vm.selectedClass != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val sheetColor = if (vm.themeMode == "AQUA") MilkyGlass else if(vm.isGlass) Color(0xFF0F2027) else MaterialTheme.colorScheme.surface
                
                ModalBottomSheet(
                    onDismissRequest = { vm.selectedClass = null },
                    sheetState = sheetState,
                    containerColor = sheetColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    windowInsets = WindowInsets.statusBars
                ) { vm.selectedClass?.let { ClassDetailsSheet(vm, it) } }
            }
        }
    }
}

@Composable
fun FloatingNavBar(vm: MainViewModel) {
    val containerColor = when(vm.themeMode) {
        "AQUA" -> MilkyGlass
        "GLASS" -> Color(0xFF0F2027).copy(alpha = 0.90f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    
    val border = if (vm.isGlass) BorderStroke(1.dp, if(vm.themeMode == "AQUA") Color.White.copy(0.5f) else GlassWhite) else null
    val elevation = if (vm.isGlass) 0.dp else 4.dp
    
    Surface(
        modifier = Modifier.height(72.dp).widthIn(max = 400.dp).fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = containerColor,
        border = border,
        shadowElevation = elevation
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            FloatingNavItem(vm, 0, Icons.Default.Home, "Home")
            FloatingNavItem(vm, 1, Icons.Default.DateRange, "Schedule")
            FloatingNavItem(vm, 2, Icons.Default.Description, "Grades")
            FloatingNavItem(vm, 3, Icons.Default.Person, "Profile")
        }
    }
}

@Composable
fun RowScope.FloatingNavItem(vm: MainViewModel, index: Int, icon: ImageVector, label: String) {
    val selected = vm.currentTab == index
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    val color by animateColorAsState(targetValue = if (selected) selectedColor else unselectedColor, label = "ColorAnim")
    val scale by animateFloatAsState(targetValue = if (selected) 1.1f else 1.0f, label = "ScaleAnim")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.currentTab = index }
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp).scale(scale))
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
