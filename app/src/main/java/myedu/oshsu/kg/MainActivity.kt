package myedu.oshsu.kg

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var mainViewModel: MainViewModel? = null

    // --- IMMEDIATE INSTALL RECEIVER ---
    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                
                // Only proceed if this matches our update download ID
                if (mainViewModel != null && id == mainViewModel?.downloadId) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)
                    var cursor: Cursor? = null
                    
                    try {
                        cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(statusIndex)

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                // 1. Get the local file URI from DownloadManager
                                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                val uriString = cursor.getString(uriIndex)
                                
                                if (uriString != null) {
                                    val localFile = File(Uri.parse(uriString).path!!)
                                    
                                    // 2. Create the FileProvider URI (Secure Content URI)
                                    val contentUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        localFile
                                    )

                                    // 3. Build the Intent
                                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(contentUri, "application/vnd.android.package-archive")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                                    }

                                    // 4. Launch immediately
                                    context.startActivity(installIntent)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        cursor?.close()
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("myedu_offline_cache", Context.MODE_PRIVATE)
        val rawLang = prefs.getString("language_pref", "en") ?: "en"
        val lang = rawLang.replace("\"", "")
        val locale = Locale(lang)
        val config = Configuration(newBase.resources.configuration)
        Locale.setDefault(locale); config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { 
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) { 
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) 
            } 
        }
        
        setupNetworkMonitoring()
        setupBackgroundWork()
        
        // Register the install receiver
        registerReceiver(installReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)

        setContent { 
            val vm: MainViewModel = viewModel(); mainViewModel = vm
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.onAppResume() }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(Unit) { vm.initSession(context) }
            LaunchedEffect(vm.fullSchedule, vm.timeMap, vm.language) { 
                if (vm.fullSchedule.isNotEmpty() && vm.timeMap.isNotEmpty()) { 
                    ScheduleAlarmManager(context).scheduleNotifications(vm.fullSchedule, vm.timeMap, vm.language) 
                } 
            }
            MyEduTheme(themeMode = vm.themeMode) { ThemedBackground(themeMode = vm.themeMode) { AppContent(vm) } } 
        }
    }

    fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        startActivity(intent); finish()
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() { override fun onAvailable(network: Network) { super.onAvailable(network); mainViewModel?.onNetworkAvailable() } }
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback!!)
    }

    private fun setupBackgroundWork() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(4, TimeUnit.HOURS).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("MyEduGradeSync", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }

    override fun onDestroy() { 
        super.onDestroy()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        unregisterReceiver(installReceiver)
    }
}

@Composable
fun AppContent(vm: MainViewModel) {
    val context = LocalContext.current
    
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = vm.appState, label = "Root", transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }) { state ->
            when (state) { "LOGIN" -> LoginScreen(vm); "APP" -> MainAppStructure(vm); else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        
        if (vm.isDebugPipVisible) {
            DebugPipButton(
                onClick = { vm.isDebugConsoleOpen = true },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            )
        }
        
        if (vm.isDebugConsoleOpen) {
            DebugConsoleOverlay(onDismiss = { vm.isDebugConsoleOpen = false })
        }

        // --- UPDATE DIALOG ---
        if (vm.updateAvailableRelease != null) {
            val release = vm.updateAvailableRelease!!
            AlertDialog(
                onDismissRequest = { vm.updateAvailableRelease = null },
                title = { Text(stringResource(R.string.update_available_title)) },
                text = { Text(stringResource(R.string.update_available_msg, release.tagName, release.body)) },
                confirmButton = {
                    Button(onClick = { vm.downloadUpdate(context) }) {
                        Text(stringResource(R.string.update_btn_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vm.updateAvailableRelease = null }) {
                        Text(stringResource(R.string.update_btn_later))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(vm: MainViewModel) {
    BackHandler(enabled = vm.selectedClass != null || vm.showTranscriptScreen || vm.showReferenceScreen || vm.showSettingsScreen || vm.webDocumentUrl != null || vm.showDictionaryScreen) { 
        when { 
            vm.webDocumentUrl != null -> vm.webDocumentUrl = null
            vm.showDictionaryScreen -> vm.showDictionaryScreen = false 
            vm.showSettingsScreen -> vm.showSettingsScreen = false 
            vm.selectedClass != null -> vm.selectedClass = null
            vm.showTranscriptScreen -> vm.showTranscriptScreen = false
            vm.showReferenceScreen -> vm.showReferenceScreen = false
        }
    }
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            MyEduPullToRefreshBox(isRefreshing = vm.isLoading, onRefresh = { vm.refresh() }, themeMode = vm.themeMode) {
                AnimatedContent(targetState = vm.currentTab, label = "TabContent", transitionSpec = { (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.96f, animationSpec = tween(400))).togetherWith(fadeOut(animationSpec = tween(200))) }, modifier = Modifier.fillMaxSize()) { targetTab ->
                    when(targetTab) { 0 -> HomeScreen(vm); 1 -> ScheduleScreen(vm); 2 -> GradesScreen(vm); 3 -> ProfileScreen(vm) }
                }
            }
            val showNav = vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && !vm.showSettingsScreen && vm.webDocumentUrl == null && !vm.showDictionaryScreen
            AnimatedVisibility(visible = showNav, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp).windowInsetsPadding(WindowInsets.navigationBars)) { FloatingNavBar(vm) }
            
            AnimatedVisibility(visible = vm.showTranscriptScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { ThemedBackground(themeMode = vm.themeMode) { TranscriptView(vm) { vm.showTranscriptScreen = false } } }
            AnimatedVisibility(visible = vm.showReferenceScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { ThemedBackground(themeMode = vm.themeMode) { ReferenceView(vm) { vm.showReferenceScreen = false } } }
            AnimatedVisibility(visible = vm.showSettingsScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { ThemedBackground(themeMode = vm.themeMode) { SettingsScreen(vm) { vm.showSettingsScreen = false } } }
            AnimatedVisibility(visible = vm.showDictionaryScreen, enter = slideInHorizontally{it}, exit = slideOutHorizontally{it}, modifier = Modifier.fillMaxSize()) { ThemedBackground(themeMode = vm.themeMode) { DictionaryScreen(vm) { vm.showDictionaryScreen = false } } }

            if (vm.webDocumentUrl != null) {
                val isTranscript = vm.webDocumentUrl!!.contains("Transcript", true)
                val docTitle = if (isTranscript) stringResource(R.string.transcript) else stringResource(R.string.reference)
                val filePrefix = if (isTranscript) stringResource(R.string.transcript_filename) else stringResource(R.string.reference_filename)
                val cleanName = "${vm.userData?.last_name?:""} ${vm.userData?.name?:""}".trim().replace(" ", "_").replace(".", "")
                val fileName = "${cleanName}_$filePrefix.pdf"
                ThemedBackground(themeMode = vm.themeMode) { WebDocumentScreen(url = vm.webDocumentUrl!!, title = docTitle, fileName = fileName, authToken = vm.getAuthToken(), themeMode = vm.themeMode, onClose = { vm.webDocumentUrl = null }) }
            }
            
            if (vm.selectedClass != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val sheetColor = if (vm.themeMode == "AQUA") MilkyGlass else if(vm.isGlass) Color(0xFF0F2027) else MaterialTheme.colorScheme.surface
                ModalBottomSheet(onDismissRequest = { vm.selectedClass = null }, sheetState = sheetState, containerColor = sheetColor, contentColor = MaterialTheme.colorScheme.onSurface, dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }, windowInsets = WindowInsets.statusBars) { vm.selectedClass?.let { ClassDetailsSheet(vm, it) } }
            }
        }
    }
}

@Composable
fun FloatingNavBar(vm: MainViewModel) {
    val containerColor = when(vm.themeMode) { "AQUA" -> MilkyGlass; "GLASS" -> Color(0xFF0F2027).copy(alpha = 0.90f); else -> MaterialTheme.colorScheme.surfaceContainer }
    val border = if (vm.isGlass) BorderStroke(1.dp, if(vm.themeMode == "AQUA") Color.White.copy(0.5f) else GlassWhite) else null
    val elevation = if (vm.isGlass) 0.dp else 4.dp
    Surface(modifier = Modifier.height(72.dp).widthIn(max = 400.dp).fillMaxWidth(), shape = RoundedCornerShape(36.dp), color = containerColor, border = border, shadowElevation = elevation) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            FloatingNavItem(vm, 0, Icons.Default.Home, stringResource(R.string.nav_home))
            FloatingNavItem(vm, 1, Icons.Default.DateRange, stringResource(R.string.nav_schedule))
            FloatingNavItem(vm, 2, Icons.Default.Description, stringResource(R.string.nav_grades))
            FloatingNavItem(vm, 3, Icons.Default.Person, stringResource(R.string.nav_profile))
        }
    }
}

@Composable
fun RowScope.FloatingNavItem(vm: MainViewModel, index: Int, icon: ImageVector, label: String) {
    val selected = vm.currentTab == index; val selectedColor = MaterialTheme.colorScheme.primary; val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val color by animateColorAsState(targetValue = if (selected) selectedColor else unselectedColor, label = "ColorAnim")
    val scale by animateFloatAsState(targetValue = if (selected) 1.1f else 1.0f, label = "ScaleAnim")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.currentTab = index }) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp).scale(scale))
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Medium, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
