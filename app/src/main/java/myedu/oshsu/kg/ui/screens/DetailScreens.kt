package myedu.oshsu.kg.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ScheduleItem
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.ScoreColumn
import myedu.oshsu.kg.ui.components.ThemedCard
import myedu.oshsu.kg.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClassDetailsSheet(vm: MainViewModel, item: ScheduleItem) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val groupLabel = if (item.subject_type?.get() == "Lecture") "Stream" else "Group"
    val groupValue = item.stream?.numeric?.toString() ?: "?"
    val timeString = vm.getTimeString(item.id_lesson)

    val activeSemester = vm.profileData?.active_semester
    val session = vm.sessionData
    val currentSemSession = session.find { it.semester?.id == activeSemester } ?: session.lastOrNull()
    val subjectGrades = currentSemSession?.subjects?.find { it.subject?.get() == item.subject?.get() }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
            
            if (vm.isGlass) {
                // Header Glass
                ThemedCard(Modifier.fillMaxWidth().background(brush = AccentGradient, shape = RoundedCornerShape(24.dp), alpha = 0.2f), themeMode = vm.themeMode) { 
                    
                    // COLOR FIX: 
                    // Aqua (Milky Background) -> Needs Dark Text (onSurface)
                    // Glass (Dark Background) -> Needs White Text (onPrimary)
                    val headerContentColor = if (vm.themeMode == "AQUA") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    
                    Column { 
                        Text(
                            item.subject?.get() ?: "Subject", 
                            style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.Bold, 
                            color = headerContentColor // Applied here
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime, 
                                contentDescription = null, 
                                tint = headerContentColor.copy(alpha=0.8f), // Applied here
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                timeString, 
                                style = MaterialTheme.typography.titleMedium, 
                                color = headerContentColor.copy(alpha=0.9f) // Applied here
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                            // Chips usually handle their own contrast, but we ensure label color matches the theme intent
                            AssistChip(
                                onClick = {}, 
                                label = { Text(item.subject_type?.get() ?: "Lesson", color = MaterialTheme.colorScheme.onSurface) }, 
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                            )
                            if (item.stream?.numeric != null) { 
                                AssistChip(
                                    onClick = {}, 
                                    label = { Text("$groupLabel $groupValue", color = MaterialTheme.colorScheme.onSurface) }, 
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) 
                            } 
                        } 
                    } 
                }
            } else {
                // Standard Material Mode
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { 
                     Column(Modifier.padding(24.dp)) { 
                        Text(item.subject?.get() ?: "Subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Time", tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                            AssistChip(onClick = {}, label = { Text(item.subject_type?.get() ?: "Lesson") })
                            if (item.stream?.numeric != null) { AssistChip(onClick = {}, label = { Text("$groupLabel $groupValue") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)) } 
                        } 
                    } 
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Current Performance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                if (subjectGrades != null) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ScoreColumn("M1", subjectGrades.marklist?.point1)
                            ScoreColumn("M2", subjectGrades.marklist?.point2)
                            ScoreColumn("Exam", subjectGrades.marklist?.finally)
                            ScoreColumn("Total", subjectGrades.marklist?.total, true)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(16.dp))
                        Text("No grades available for this subject yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Teacher", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerLow) { 
                Row(verticalAlignment = Alignment.CenterVertically) { 
                    Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Text(item.teacher?.get() ?: "Unknown", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(item.teacher?.get() ?: "")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }) { 
                        Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.outline) 
                    } 
                } 
            }

            Spacer(Modifier.height(16.dp))
            Text("Location", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            
            ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerLow) { 
                Column { 
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        Icon(Icons.Outlined.MeetingRoom, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) { 
                            Text("Room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(item.room?.name_en ?: "Unknown", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) 
                        } 
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical=12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) { 
                            val address = item.classroom?.building?.getAddress()
                            val displayAddress = if (address.isNullOrBlank()) "Building" else address
                            Text(displayAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Text(item.classroom?.building?.getName() ?: "Campus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(item.classroom?.building?.getName() ?: "")); Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show() }) { 
                            Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.outline) 
                        }
                        IconButton(onClick = { 
                            val locationName = item.classroom?.building?.getName() ?: ""
                            if (locationName.isNotEmpty()) { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(locationName)}")); try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No map app found", Toast.LENGTH_SHORT).show() }
                            } 
                        }) { Icon(Icons.Outlined.Map, "Map", tint = MaterialTheme.colorScheme.primary) } 
                    } 
                } 
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun FloatingPdfBar(vm: MainViewModel, onGenerateRu: () -> Unit, onGenerateEn: () -> Unit) {
    if (vm.downloadMode == "WEBSITE") return
    val containerColor = if (vm.themeMode == "AQUA") MilkyGlass else if (vm.isGlass) Color(0xFF0F2027).copy(alpha = 0.90f) else MaterialTheme.colorScheme.surface
    // Use Colors directly.
    val border = if (vm.isGlass) BorderStroke(1.dp, if(vm.themeMode=="AQUA") MilkyBorder else GlassWhite) else null
    val elevation = if (vm.isGlass) 0.dp else 12.dp

    Surface(
        modifier = Modifier.padding(bottom = 24.dp, start = 16.dp, end = 16.dp).height(72.dp).widthIn(max = 400.dp).fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        color = containerColor,
        border = border,
        shadowElevation = elevation
    ) {
        if (vm.isPdfGenerating) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text("Generating PDF...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val buttonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                Button(onClick = onGenerateRu, colors = buttonColors, modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(48.dp)) { Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("PDF (RU)") }
                Button(onClick = onGenerateEn, colors = buttonColors, modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(48.dp)) { Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("PDF (EN)") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceView(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current; val user = vm.userData; val profile = vm.profileData; val mov = profile?.studentMovement
    val activeSemester = profile?.active_semester ?: 1; val course = (activeSemester + 1) / 2
    val facultyName = mov?.faculty?.let { it.name_en ?: it.name_ru } ?: mov?.speciality?.faculty?.let { it.name_en ?: it.name_ru } ?: "-"
    val isWebsiteMode = vm.downloadMode == "WEBSITE"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Reference (Form 8)") }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, actions = { if (isWebsiteMode) { IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/studentCertificate" }) { Icon(Icons.Default.Print, "Print / Download") } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ThemedCard(Modifier.fillMaxWidth(), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.padding(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { OshSuLogo(modifier = Modifier.width(180.dp).height(60.dp), themeMode = vm.themeMode); Spacer(Modifier.height(16.dp)); Text("CERTIFICATE OF STUDY", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.height(24.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(24.dp))
                        Text("This is to certify that", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${user?.last_name} ${user?.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(24.dp))
                        RefDetailRow("Student ID", "${user?.id}")
                        RefDetailRow("Faculty", facultyName)
                        RefDetailRow("Speciality", mov?.speciality?.name_en ?: "-")
                        RefDetailRow("Year of Study", "$course ($activeSemester Semester)")
                        RefDetailRow("Education Form", mov?.edu_form?.name_en ?: "-")
                        RefDetailRow("Payment", if (mov?.id_payment_form == 2) "Contract" else "Budget")
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = Color(0xFF00FF88), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Active Student • ${SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00FF88)) }
                    }
                }
                Spacer(Modifier.height(24.dp)); Text("This is a preview.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Click the download button below for official PDF.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.pdfStatusMessage != null) { Spacer(Modifier.height(16.dp)); Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)) { Text(vm.pdfStatusMessage!!, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(16.dp)) } }
                Spacer(Modifier.height(100.dp))
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) { FloatingPdfBar(vm = vm, onGenerateRu = { vm.generateReferencePdf(context, "ru") }, onGenerateEn = { vm.generateReferencePdf(context, "en") }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptView(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current; val isWebsiteMode = vm.downloadMode == "WEBSITE"
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Full Transcript") }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, null) } }, actions = { if (isWebsiteMode) { IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/Transcript" }) { Icon(Icons.Default.Print, "Print / Download") } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (vm.isTranscriptLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (vm.transcriptData.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No transcript data.", color = MaterialTheme.colorScheme.onSurface) }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopCenter).widthIn(max = 840.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)) {
                    vm.transcriptData.forEach { yearData ->
                        item { Spacer(Modifier.height(16.dp)); Text(yearData.eduYear ?: "Unknown Year", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        yearData.semesters?.forEach { sem ->
                            item { Spacer(Modifier.height(12.dp)); Text(sem.semesterName ?: "Semester", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)) }
                            items(sem.subjects ?: emptyList()) { sub ->
                                ThemedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainer) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { Text(sub.subjectName ?: "Subject", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface); Text("Code: ${sub.code ?: "-"} • Credits: ${sub.credit?.toInt() ?: 0}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        Column(horizontalAlignment = Alignment.End) { val total = sub.markList?.total?.toInt() ?: 0; Text("$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (total >= 50) Color(0xFF00FF88) else MaterialTheme.colorScheme.error); Text(sub.examRule?.alphabetic ?: "-", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (vm.pdfStatusMessage != null) Card(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)) { Text(vm.pdfStatusMessage!!, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(16.dp)) }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) { FloatingPdfBar(vm = vm, onGenerateRu = { vm.generateTranscriptPdf(context, "ru") }, onGenerateEn = { vm.generateTranscriptPdf(context, "en") }) }
        }
    }
}

@Composable
fun RefDetailRow(label: String, value: String) { 
    Column(Modifier.padding(bottom = 16.dp)) { 
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) 
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) 
    } 
}
