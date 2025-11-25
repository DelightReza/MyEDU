package kg.oshsu.myedu

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset 
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- UI CONSTANTS ---
val GlassWhite = Color.White.copy(alpha = 0.10f)
val GlassBorder = Color.White.copy(alpha = 0.20f)
val TextWhite = Color.White
val AccentGradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))

// Liquid Background Brush
val LiquidBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F2027), // Deep Dark Blue
        Color(0xFF203A43), // Tealish Dark
        Color(0xFF2C5364)  // Lighter Teal/Blue
    )
)

// --- UI COMPONENT: LOGO ---
@Composable
fun OshSuLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = "file:///android_asset/logo-dark4.svg"
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    
    AsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = "OshSU Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        // Optional: Tint if transparent background (Glass)
        colorFilter = if (MaterialTheme.colorScheme.background == Color.Transparent) ColorFilter.tint(Color.White) else null
    )
}

// --- ACTIVITY: MAIN ENTRY POINT ---
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

// --- UI: THEME CONFIG ---
@Composable
fun MyEduTheme(
    themeMode: String,
    systemDark: Boolean = isSystemInDarkTheme(), 
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Determine strict Dark Mode
    val isDark = when(themeMode) {
        "LIGHT" -> false
        "DARK", "GLASS" -> true
        else -> systemDark // SYSTEM
    }

    val colorScheme = when {
        themeMode == "GLASS" -> darkColorScheme(
            primary = Color(0xFF00C6FF),
            onPrimary = Color.White,
            secondary = Color(0xFF0072FF),
            onSecondary = Color.White,
            surface = Color.Transparent, 
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.7f),
            background = Color.Transparent,
            onBackground = Color.White
        )
        // Material Theme (System/Light/Dark)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            // Glass -> Always light icons (false), Dark -> Light Icons, Light -> Dark Icons
            val lightIcons = if (themeMode == "GLASS") false else !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightIcons
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// --- ABSTRACTION: THEMED BACKGROUND ---
@Composable
fun ThemedBackground(isGlass: Boolean, content: @Composable BoxScope.() -> Unit) {
    if (isGlass) {
        Box(
            modifier = Modifier.fillMaxSize().background(LiquidBackgroundBrush),
            content = content
        )
    } else {
        // In Material mode, use 'background' (usually off-white/light-grey in Light mode)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = { Box(Modifier.fillMaxSize(), content = content) }
        )
    }
}

// --- ABSTRACTION: THEMED CARD ---
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    isGlass: Boolean,
    onClick: (() -> Unit)? = null,
    glassAlpha: Float = 0.10f,
    // Default to Surface (White in Light Mode) for clean Material look
    materialColor: Color = MaterialTheme.colorScheme.surface, 
    content: @Composable ColumnScope.() -> Unit
) {
    if (isGlass) {
        Surface(
            modifier = modifier
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White.copy(alpha = glassAlpha),
            shape = RoundedCornerShape(24.dp),
            onClick = onClick ?: {},
            content = { Column(Modifier.padding(16.dp), content = content) }
        )
    } else {
        // Standard Material Card (Elevated, White Surface)
        if (onClick != null) {
            ElevatedCard(
                onClick = onClick,
                modifier = modifier,
                colors = CardDefaults.elevatedCardColors(containerColor = materialColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                content = { Column(Modifier.padding(16.dp), content = content) }
            )
        } else {
            ElevatedCard(
                modifier = modifier,
                colors = CardDefaults.elevatedCardColors(containerColor = materialColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                content = { Column(Modifier.padding(16.dp), content = content) }
            )
        }
    }
}

// --- UI: NAVIGATION HOST ---
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

// --- SCREEN: LOGIN ---
@Composable
fun LoginScreen(vm: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    
    val inputColors = if (vm.isGlass) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00C6FF),
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF00C6FF),
            cursorColor = Color.White
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).padding(24.dp).systemBarsPadding(), 
            verticalArrangement = Arrangement.Center, 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OshSuLogo(modifier = Modifier.width(260.dp).height(100.dp))
            Spacer(Modifier.height(48.dp))
            
            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("Email") }, 
                modifier = Modifier.fillMaxWidth(), 
                singleLine = true,
                colors = inputColors,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pass, 
                onValueChange = { pass = it }, 
                label = { Text("Password") }, 
                modifier = Modifier.fillMaxWidth(), 
                singleLine = true, 
                visualTransformation = PasswordVisualTransformation(),
                colors = inputColors,
                shape = RoundedCornerShape(16.dp)
            )
            
            if (vm.errorMsg != null) { Spacer(Modifier.height(16.dp)); Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(32.dp))
            
            val btnModifier = Modifier.fillMaxWidth().height(56.dp)
            val finalBtnMod = if (vm.isGlass) btnModifier.background(AccentGradient, RoundedCornerShape(16.dp)) else btnModifier
            val btnColors = if (vm.isGlass) ButtonDefaults.buttonColors(containerColor = Color.Transparent) else ButtonDefaults.buttonColors()

            Button(
                onClick = { vm.login(email, pass) }, 
                modifier = finalBtnMod, 
                enabled = !vm.isLoading,
                colors = btnColors,
                shape = RoundedCornerShape(16.dp)
            ) { 
                if (vm.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text("Sign In", fontWeight = FontWeight.Bold) 
            }
        }
    }
}

// --- UI: MAIN SCAFFOLD & BOTTOM NAV ---
@Composable
fun MainAppStructure(vm: MainViewModel) {
    BackHandler(enabled = vm.selectedClass != null || vm.showTranscriptScreen || vm.showReferenceScreen || vm.webDocumentUrl != null) { 
        when {
            vm.webDocumentUrl != null -> vm.webDocumentUrl = null
            vm.selectedClass != null -> vm.selectedClass = null
            vm.showTranscriptScreen -> vm.showTranscriptScreen = false
            vm.showReferenceScreen -> vm.showReferenceScreen = false
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent, 
        bottomBar = {
            if (vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && vm.webDocumentUrl == null) {
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
                    // Standard Material Nav Bar
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { 
                        NavItems(vm, NavigationBarItemDefaults.colors()) 
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (vm.selectedClass == null && !vm.showTranscriptScreen && !vm.showReferenceScreen && vm.webDocumentUrl == null) {
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
            AnimatedVisibility(visible = vm.selectedClass != null, enter = slideInVertically{it}, exit = slideOutVertically{it}, modifier = Modifier.fillMaxSize()) { 
                ThemedBackground(vm.isGlass) { vm.selectedClass?.let { ClassDetailsScreen(it, vm) { vm.selectedClass = null } } } 
            }
            
            if (vm.webDocumentUrl != null) {
                WebDocumentScreen(
                    url = vm.webDocumentUrl!!,
                    title = "Document",
                    authToken = vm.getAuthToken(),
                    onClose = { vm.webDocumentUrl = null }
                )
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

// --- SCREEN: DOCUMENT REFERENCE (FORM 8) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceView(vm: MainViewModel, onClose: () -> Unit) {
    val user = vm.userData; val profile = vm.profileData; val mov = profile?.studentMovement
    val activeSemester = profile?.active_semester ?: 1; val course = (activeSemester + 1) / 2
    val facultyName = mov?.faculty?.name_en ?: mov?.speciality?.faculty?.name_en ?: mov?.faculty?.name_ru ?: "-"
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { 
            TopAppBar(
                title = { Text("Reference (Form 8)") }, 
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, 
                actions = { 
                    IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/studentCertificate" }) { 
                        Icon(Icons.Default.Print, "Print / Download") 
                    } 
                },
                colors = if (vm.isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White) else TopAppBarDefaults.topAppBarColors()
            ) 
        }
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.padding(padding).fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ThemedCard(Modifier.fillMaxWidth(), vm.isGlass) {
                    Column(Modifier.padding(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { 
                            OshSuLogo(modifier = Modifier.width(180.dp).height(60.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("CERTIFICATE OF STUDY", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(24.dp)) 
                        if (vm.isGlass) HorizontalDivider(color = GlassBorder) else HorizontalDivider()
                        Spacer(Modifier.height(24.dp))
                        Text("This is to certify that", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${user?.last_name} ${user?.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        RefDetailRow("Student ID", "${user?.id}"); RefDetailRow("Faculty", facultyName); RefDetailRow("Speciality", mov?.speciality?.name_en ?: "-"); RefDetailRow("Year of Study", "$course ($activeSemester Semester)"); RefDetailRow("Education Form", mov?.edu_form?.name_en ?: "-"); RefDetailRow("Payment", if (mov?.id_payment_form == 2) "Contract" else "Budget")
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = Color(0xFF00FF88), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Active Student • ${SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00FF88)) }
                    }
                }
                Spacer(Modifier.height(24.dp)); Text("This is a preview.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Click the printer icon to download official PDF.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun RefDetailRow(label: String, value: String) { Column(Modifier.padding(bottom = 16.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge) } }

// --- SCREEN: TRANSCRIPT PREVIEW ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptView(vm: MainViewModel, onClose: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { 
            TopAppBar(
                title = { Text("Full Transcript") }, 
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, 
                actions = { IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/Transcript" }) { Icon(Icons.Default.Print, "Print / Download") } },
                colors = if (vm.isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White) else TopAppBarDefaults.topAppBarColors()
            ) 
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (vm.isTranscriptLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (vm.transcriptData.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No transcript data.") }
            else {
                LazyColumn(Modifier.widthIn(max = 840.dp).padding(horizontal = 16.dp)) {
                    vm.transcriptData.forEach { yearData ->
                        item { Spacer(Modifier.height(16.dp)); Text(yearData.eduYear ?: "Unknown Year", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        yearData.semesters?.forEach { sem ->
                            item { Spacer(Modifier.height(12.dp)); Text(sem.semesterName ?: "Semester", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)) }
                            items(sem.subjects ?: emptyList()) { sub ->
                                ThemedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), vm.isGlass) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(sub.subjectName ?: "Subject", fontWeight = FontWeight.SemiBold); Text("Code: ${sub.code ?: "-"} • Credits: ${sub.credit?.toInt() ?: 0}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        Column(horizontalAlignment = Alignment.End) { val total = sub.markList?.total?.toInt() ?: 0; Text("$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (total >= 50) Color(0xFF00FF88) else MaterialTheme.colorScheme.error); Text(sub.examRule?.alphabetic ?: "-", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// --- SCREEN: PROFILE TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val user = vm.userData; val profile = vm.profileData; val pay = vm.payStatus
    val fullName = "${user?.last_name ?: ""} ${user?.name ?: ""}".trim().ifEmpty { "Student" }
    val facultyName = profile?.studentMovement?.faculty?.let { it.name_en ?: it.name_ru } ?: profile?.studentMovement?.speciality?.faculty?.let { it.name_en ?: it.name_ru } ?: "-"
    val specialityName = profile?.studentMovement?.speciality?.let { it.name_en ?: it.name_ru } ?: "-"
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Header with Settings Icon
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(Icons.Outlined.Settings, "Settings")
                }
            }
            
            Spacer(Modifier.height(24.dp))
            val imgMod = if(vm.isGlass) Modifier.size(128.dp).background(AccentGradient, CircleShape).padding(3.dp).clip(CircleShape).background(Color(0xFF0F2027)) else Modifier.size(128.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            Box(contentAlignment = Alignment.Center, modifier = imgMod) { AsyncImage(model = profile?.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)) }
            
            Spacer(Modifier.height(16.dp))
            Text(fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(24.dp))
            if (pay != null) {
                ThemedCard(Modifier.fillMaxWidth(), vm.isGlass) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tuition Contract", fontWeight = FontWeight.Bold); Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.paid_summa?.toInt() ?: 0} KGS", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88)) }; Column(horizontalAlignment = Alignment.End) { Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.need_summa?.toInt() ?: 0} KGS", style = MaterialTheme.typography.titleMedium) } }
                        val debt = pay.getDebt(); if (debt > 0) { Spacer(Modifier.height(8.dp)); if(vm.isGlass) HorizontalDivider(color=GlassBorder) else HorizontalDivider(); Spacer(Modifier.height(8.dp)); Text("Remaining: ${debt.toInt()} KGS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                        if (!pay.access_message.isNullOrEmpty()) { Spacer(Modifier.height(12.dp)); if(vm.isGlass) HorizontalDivider(Modifier.padding(bottom=8.dp), color=GlassBorder) else HorizontalDivider(Modifier.padding(bottom=8.dp)); pay.access_message.forEach { msg -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) } } }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            InfoSection("Documents", vm.isGlass)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BeautifulDocButton(
                    text = "Reference",
                    icon = Icons.Default.Description,
                    isGlass = vm.isGlass,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.showReferenceScreen = true }
                )
                BeautifulDocButton(
                    text = "Transcript",
                    icon = Icons.Default.School,
                    isGlass = vm.isGlass,
                    isLoading = vm.isTranscriptLoading,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.fetchTranscript() }
                )
            }
            Spacer(Modifier.height(24.dp)); InfoSection("Academic", vm.isGlass)
            DetailCard(Icons.Outlined.School, "Faculty", facultyName, vm.isGlass)
            DetailCard(Icons.Outlined.Book, "Speciality", specialityName, vm.isGlass)
            Spacer(Modifier.height(24.dp)); InfoSection("Personal", vm.isGlass)
            DetailCard(Icons.Outlined.Badge, "Passport", profile?.pdsstudentinfo?.getFullPassport() ?: "-", vm.isGlass); DetailCard(Icons.Outlined.Phone, "Phone", profile?.pdsstudentinfo?.phone ?: "-", vm.isGlass)
            Spacer(Modifier.height(32.dp)); Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) { Text("Log Out") }; Spacer(Modifier.height(80.dp))
        }
    }
    
    // Settings Sheet
    if (showSettingsSheet) {
        val containerColor = if(vm.isGlass) Color(0xFF16213E) else BottomSheetDefaults.ContainerColor
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, containerColor = containerColor) {
            Column(Modifier.padding(24.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                
                InfoSection("Appearance", vm.isGlass)
                ThemeOptionRow("Follow System", vm.themeMode == "SYSTEM") { vm.setTheme("SYSTEM") }
                ThemeOptionRow("Light Mode", vm.themeMode == "LIGHT") { vm.setTheme("LIGHT") }
                ThemeOptionRow("Dark Mode", vm.themeMode == "DARK") { vm.setTheme("DARK") }
                ThemeOptionRow("Liquid Glass", vm.themeMode == "GLASS") { vm.setTheme("GLASS") }
                
                Spacer(Modifier.height(24.dp))
                InfoSection("Language", vm.isGlass)
                ThemeOptionRow("English", true) { } // Placeholder
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ThemeOptionRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

// --- NEW COMPONENT: BEAUTIFUL DOC BUTTON ---
@Composable
fun BeautifulDocButton(
    text: String,
    icon: ImageVector,
    isGlass: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    
    val containerModifier = if (isGlass) {
        // Glass Version
        modifier
            .height(56.dp)
            .background(GlassWhite, shape)
            .border(1.dp, AccentGradient, shape)
            .clip(shape)
            .clickable(enabled = !isLoading, onClick = onClick)
    } else {
        // Beautiful Material Version (White Surface + Gradient Border + Shadow)
        modifier
            .height(56.dp)
            .shadow(4.dp, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, AccentGradient, shape)
            .clip(shape)
            .clickable(enabled = !isLoading, onClick = onClick)
    }
    
    Box(contentAlignment = Alignment.Center, modifier = containerModifier) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), 
                color = if(isGlass) Color.White else MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    null, 
                    tint = if(isGlass) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text, 
                    fontWeight = FontWeight.SemiBold,
                    color = if(isGlass) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// --- SCREEN: HOME TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val user = vm.userData; val profile = vm.profileData; var showNewsSheet by remember { mutableStateOf(false) }
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) { if(currentHour in 4..11) "Good Morning," else if(currentHour in 12..16) "Good Afternoon," else "Good Evening," }
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { 
                    Text(greetingText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = user?.name ?: "Student", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(40.dp).clickable { showNewsSheet = true }, contentAlignment = Alignment.Center) {
                        if (vm.newsList.isNotEmpty()) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) { Text("${vm.newsList.size}") } }) { Icon(Icons.Outlined.Notifications, contentDescription = "Announcements", tint = MaterialTheme.colorScheme.onSurface) }
                        } else {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Announcements", tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
                StatCard(Icons.Outlined.CalendarToday, "Semester", profile?.active_semester?.toString() ?: "-", vm.isGlass, Modifier.weight(1f))
                StatCard(Icons.Outlined.Groups, "Group", if (vm.determinedGroup != null) "Group ${vm.determinedGroup}" else profile?.studentMovement?.avn_group_name ?: "-", vm.isGlass, Modifier.weight(1f)) 
            }
            Spacer(Modifier.height(32.dp))
            Text("${vm.todayDayName}'s Classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (vm.todayClasses.isEmpty()) {
                ThemedCard(modifier = Modifier.fillMaxWidth(), vm.isGlass) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Weekend, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Text("No classes today!", style = MaterialTheme.typography.bodyLarge) } } 
            } else {
                vm.todayClasses.forEach { item -> ClassItem(item, vm.getTimeString(item.id_lesson), vm.isGlass) { vm.selectedClass = item } } 
            }
            Spacer(Modifier.height(80.dp))
        }
    }    
    if (showNewsSheet) {
        val containerColor = if(vm.isGlass) Color(0xFF16213E) else BottomSheetDefaults.ContainerColor
        ModalBottomSheet(onDismissRequest = { showNewsSheet = false }, containerColor = containerColor) { 
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { 
                Text("Announcements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if(vm.isGlass) Color.White else Color.Unspecified)
                LazyColumn { items(vm.newsList) { news -> ThemedCard(Modifier.padding(top=8.dp).fillMaxWidth(), vm.isGlass) { Column { Text(news.title?:"", fontWeight=FontWeight.Bold); Text(news.message?:"", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } 
            } 
        } 
    }
}

// --- SCREEN: SCHEDULE TAB ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val tabs = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val scope = rememberCoroutineScope()
    val initialPage = remember {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 0 else (dow - 2).coerceIn(0, 5)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CenterAlignedTopAppBar(title = { OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp)) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary)
                    }
                }
            ) {
                tabs.forEachIndexed { index, title -> Tab(selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.animateScrollToPage(index) } }, text = { Text(title, color = if(pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }) }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                val dayClasses = vm.fullSchedule.filter { it.day == pageIndex }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    if (dayClasses.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Weekend, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant); Spacer(Modifier.height(16.dp)); Text("No classes", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(16.dp)) { items(dayClasses) { item -> ClassItem(item, vm.getTimeString(item.id_lesson), vm.isGlass) { vm.selectedClass = item } }; item { Spacer(Modifier.height(80.dp)) } }
                    }
                }
            }
        }
    }
}

// --- SCREEN: CLASS DETAILS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsScreen(item: ScheduleItem, vm: MainViewModel, onClose: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current; val context = LocalContext.current; val groupLabel = if (item.subject_type?.get() == "Lecture") "Stream" else "Group"; val groupValue = item.stream?.numeric?.toString() ?: "?"
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Class Details") }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, colors = if (vm.isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White) else TopAppBarDefaults.topAppBarColors()) }
    ) { padding -> 
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.padding(padding).fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(16.dp)) { 
                if (vm.isGlass) {
                    ThemedCard(Modifier.fillMaxWidth().background(brush = AccentGradient, shape = RoundedCornerShape(24.dp), alpha = 0.2f), true) { 
                        Column { 
                            Text(item.subject?.get() ?: "Subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                                AssistChip(onClick = {}, label = { Text(item.subject_type?.get() ?: "Lesson", color = TextWhite) }, colors = AssistChipDefaults.assistChipColors(containerColor = GlassWhite))
                                if (item.stream?.numeric != null) { AssistChip(onClick = {}, label = { Text("$groupLabel $groupValue", color = TextWhite) }, colors = AssistChipDefaults.assistChipColors(containerColor = GlassWhite)) } 
                            } 
                        } 
                    }
                } else {
                    // Material Mode: Use Primary Container for Main Header Card
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { 
                         Column(Modifier.padding(24.dp)) { 
                            Text(item.subject?.get() ?: "Subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text(item.subject_type?.get() ?: "Lesson") }); if (item.stream?.numeric != null) { AssistChip(onClick = {}, label = { Text("$groupLabel $groupValue") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)) } } 
                        } 
                    }
                }
                
                Spacer(Modifier.height(24.dp)); Text("Teacher", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), vm.isGlass) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Text(item.teacher?.get() ?: "Unknown", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { clipboardManager.setText(AnnotatedString(item.teacher?.get() ?: "")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                Spacer(Modifier.height(24.dp)); Text("Location", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), vm.isGlass) { Column { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.MeetingRoom, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("Room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.room?.name_en ?: "Unknown", style = MaterialTheme.typography.bodyLarge) } }; if(vm.isGlass) HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp)) else HorizontalDivider(modifier = Modifier.padding(vertical=12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(item.classroom?.building?.getAddress() ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.classroom?.building?.getName() ?: "Campus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; IconButton(onClick = { val address = item.classroom?.building?.getAddress() ?: ""; if (address.isNotEmpty()) { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")); context.startActivity(intent) } }) { Icon(Icons.Outlined.Map, "Map", tint = MaterialTheme.colorScheme.primary) } } } } 
            }
        }
    }
}

// --- HELPER UI: STAT CARD ---
@Composable
fun StatCard(icon: ImageVector, label: String, value: String, isGlass: Boolean, modifier: Modifier = Modifier) {
    if (isGlass) {
        ThemedCard(modifier = modifier, isGlass = true) { Column { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(text = value, style = if(value.length > 15) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
    } else {
        // Material Mode: White Card with Primary Icon/Text
        ElevatedCard(modifier = modifier, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { 
            Column(Modifier.padding(16.dp)) { 
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, style = if(value.length > 15) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis) 
            } 
        }
    }
}

// --- HELPER UI: CLASS LIST ITEM ---
@Composable
fun ClassItem(item: ScheduleItem, timeString: String, isGlass: Boolean, onClick: () -> Unit) {
    val streamInfo = if (item.stream?.numeric != null) { val type = item.subject_type?.get(); if (type == "Lecture") "Stream ${item.stream.numeric}" else "Group ${item.stream.numeric}" } else ""
    
    // Use default white material color
    ThemedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), isGlass = isGlass) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            val timeBg = if(isGlass) GlassWhite else MaterialTheme.colorScheme.surfaceContainerHigh
            val timeShape = RoundedCornerShape(8.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp).background(timeBg, timeShape).padding(vertical = 8.dp)) { Text("${item.id_lesson}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(timeString.split("-").firstOrNull()?.trim() ?: "", style = MaterialTheme.typography.labelSmall) }
            
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(item.subject?.get() ?: "Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); val metaText = buildString { append(item.room?.name_en ?: "Room ?"); append(" • "); append(item.subject_type?.get() ?: "Lesson"); if (streamInfo.isNotEmpty()) { append(" • "); append(streamInfo) } }; Text(metaText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(timeString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
        } 
    }
}

// --- HELPER UI: TEXT SECTION ---
@Composable
fun InfoSection(title: String, isGlass: Boolean) { Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)) }

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String, isGlass: Boolean) {
    // Use default white material color
    ThemedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), isGlass = isGlass) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Column { Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) } } }
}

// --- SCREEN: GRADES TAB ---
@Composable
fun GradesScreen(vm: MainViewModel) {
    val session = vm.sessionData
    val activeSemId = vm.profileData?.active_semester
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (vm.isGradesLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else {
            LazyColumn(Modifier.fillMaxSize().widthIn(max = 840.dp).padding(16.dp)) {
                item { Spacer(Modifier.height(32.dp)); Text("Current Session", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)) }
                if (session.isEmpty()) item { Text("No grades available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else {
                    val currentSem = session.find { it.semester?.id == activeSemId } ?: session.lastOrNull()
                    if (currentSem != null) {
                        item { Text(currentSem.semester?.name_en ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
                        items(currentSem.subjects ?: emptyList()) { sub ->
                            // Default white card in Material mode
                            ThemedCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), vm.isGlass) {
                                Column {
                                    Text(sub.subject?.get() ?: "Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if(vm.isGlass) HorizontalDivider(Modifier.padding(vertical = 8.dp), color=GlassBorder) else HorizontalDivider(Modifier.padding(vertical=8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        ScoreColumn("M1", sub.marklist?.point1, vm.isGlass); ScoreColumn("M2", sub.marklist?.point2, vm.isGlass)
                                        ScoreColumn("Exam", sub.marklist?.finally, vm.isGlass); ScoreColumn("Total", sub.marklist?.total, vm.isGlass, true)
                                    }
                                }
                            }
                        }
                    } else item { Text("Semester data not found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ScoreColumn(label: String, score: Double?, isGlass: Boolean, isTotal: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${score?.toInt() ?: 0}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isTotal && (score ?: 0.0) >= 50) Color(0xFF00FF88) else if (isTotal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}
