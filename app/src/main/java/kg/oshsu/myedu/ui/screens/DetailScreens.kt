package kg.oshsu.myedu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kg.oshsu.myedu.MainViewModel
import kg.oshsu.myedu.ScheduleItem
import kg.oshsu.myedu.ui.components.OshSuLogo
import kg.oshsu.myedu.ui.components.ThemedCard
import kg.oshsu.myedu.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { 
                         Column(Modifier.padding(24.dp)) { 
                            Text(item.subject?.get() ?: "Subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text(item.subject_type?.get() ?: "Lesson") }); if (item.stream?.numeric != null) { AssistChip(onClick = {}, label = { Text("$groupLabel $groupValue") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)) } } 
                        } 
                    }
                }
                
                Spacer(Modifier.height(24.dp)); Text("Teacher", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), vm.isGlass) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Text(item.teacher?.get() ?: "Unknown", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); IconButton(onClick = { clipboardManager.setText(AnnotatedString(item.teacher?.get() ?: "")); }) { Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                Spacer(Modifier.height(24.dp)); Text("Location", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), vm.isGlass) { Column { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.MeetingRoom, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("Room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.room?.name_en ?: "Unknown", style = MaterialTheme.typography.bodyLarge) } }; if(vm.isGlass) HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp)) else HorizontalDivider(modifier = Modifier.padding(vertical=12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(item.classroom?.building?.getAddress() ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(item.classroom?.building?.getName() ?: "Campus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; IconButton(onClick = { val address = item.classroom?.building?.getAddress() ?: ""; if (address.isNotEmpty()) { val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")); context.startActivity(intent) } }) { Icon(Icons.Outlined.Map, "Map", tint = MaterialTheme.colorScheme.primary) } } } } 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceView(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current; val user = vm.userData; val profile = vm.profileData; val mov = profile?.studentMovement
    val activeSemester = profile?.active_semester ?: 1; val course = (activeSemester + 1) / 2
    val facultyName = mov?.faculty?.name_en ?: mov?.speciality?.faculty?.name_en ?: mov?.faculty?.name_ru ?: "-"
    
    val isWebsiteMode = vm.downloadMode == "WEBSITE"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { 
            TopAppBar(
                title = { Text("Reference (Form 8)") }, 
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, 
                actions = { 
                    if (isWebsiteMode) {
                        IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/studentCertificate" }) { 
                            Icon(Icons.Default.Print, "Print / Download") 
                        }
                    }
                },
                colors = if (vm.isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White) else TopAppBarDefaults.topAppBarColors()
            ) 
        },
        bottomBar = {
            if (!isWebsiteMode) {
                Surface(
                    color = if(vm.isGlass) Color(0xFF0F2027).copy(alpha=0.9f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (vm.isPdfGenerating) {
                             Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                 CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                 Spacer(Modifier.width(16.dp))
                                 Text("Generating PDF...", color = MaterialTheme.colorScheme.primary)
                             }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { vm.generateReferencePdf(context, "ru") }, 
                                    modifier = Modifier.weight(1f),
                                    colors = if(vm.isGlass) ButtonDefaults.buttonColors(containerColor = GlassWhite) else ButtonDefaults.buttonColors()
                                ) { 
                                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("PDF (RU)") 
                                }
                                Button(
                                    onClick = { vm.generateReferencePdf(context, "en") }, 
                                    modifier = Modifier.weight(1f),
                                    colors = if(vm.isGlass) ButtonDefaults.buttonColors(containerColor = GlassWhite) else ButtonDefaults.buttonColors()
                                ) { 
                                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("PDF (EN)") 
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                
                if (vm.pdfStatusMessage != null) { 
                    Spacer(Modifier.height(16.dp)); 
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)) { 
                        Text(vm.pdfStatusMessage!!, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(16.dp)) 
                    } 
                }
                
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun RefDetailRow(label: String, value: String) { Column(Modifier.padding(bottom = 16.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(value, style = MaterialTheme.typography.bodyLarge) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptView(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    
    // LOGIC: IF WEBSITE MODE -> SHOW TOP ICON. IF IN-APP -> SHOW BOTTOM BUTTONS.
    val isWebsiteMode = vm.downloadMode == "WEBSITE"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { 
            TopAppBar(
                title = { Text("Full Transcript") }, 
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, 
                actions = { 
                    if (isWebsiteMode) {
                        IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/Transcript" }) { 
                            Icon(Icons.Default.Print, "Print / Download") 
                        }
                    }
                },
                colors = if (vm.isGlass) TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White) else TopAppBarDefaults.topAppBarColors()
            ) 
        },
        bottomBar = {
            if (!isWebsiteMode) {
                Surface(
                    color = if(vm.isGlass) Color(0xFF0F2027).copy(alpha=0.9f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                         if (vm.isPdfGenerating) {
                             Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                 CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                 Spacer(Modifier.width(16.dp))
                                 Text("Generating PDF...", color = MaterialTheme.colorScheme.primary)
                             }
                         } else {
                             Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { vm.generateTranscriptPdf(context, "ru") }, 
                                    modifier = Modifier.weight(1f),
                                    colors = if(vm.isGlass) ButtonDefaults.buttonColors(containerColor = GlassWhite) else ButtonDefaults.buttonColors()
                                ) { 
                                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("PDF (RU)") 
                                }
                                Button(
                                    onClick = { vm.generateTranscriptPdf(context, "en") }, 
                                    modifier = Modifier.weight(1f),
                                    colors = if(vm.isGlass) ButtonDefaults.buttonColors(containerColor = GlassWhite) else ButtonDefaults.buttonColors()
                                ) { 
                                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("PDF (EN)") 
                                }
                            }
                         }
                    }
                }
            }
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
            
            if (vm.pdfStatusMessage != null) { 
                Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)) { 
                    Text(vm.pdfStatusMessage!!, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(16.dp)) 
                } 
            }
        }
    }
}
