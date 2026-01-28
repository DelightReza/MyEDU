package myedu.oshsu.kg.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ScheduleItem
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.ScoreColumn 
import myedu.oshsu.kg.ui.components.ThemedCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun isValid(value: String?): Boolean {
    val s = value?.trim()
    return !s.isNullOrEmpty() && !s.equals("null", true) && s != "-" && s != "Unknown" && s != "?"
}

@Composable
fun ClassDetailsSheet(vm: MainViewModel, item: ScheduleItem) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val lang = vm.language
    
    val typeResId = vm.getSubjectTypeResId(item)
    val typeName = if (typeResId != null) stringResource(typeResId) else item.subject_type?.get(lang) ?: stringResource(R.string.lesson_default)
    
    val groupLabel = if (item.subject_type?.name_en?.contains("Lection", true) == true) stringResource(R.string.stream) else stringResource(R.string.group)
    val groupValue = item.stream?.numeric?.toString() ?: "?"
    val timeString = vm.getTimeString(item.id_lesson)
    val activeSemester = vm.profileData?.active_semester
    val session = vm.sessionData
    val currentSemSession = session.find { it.semester?.id == activeSemester } ?: session.lastOrNull()
    val subjectGrades = currentSemSession?.subjects?.find { it.subject?.get(lang) == item.subject?.get(lang) }
    
    val copiedStr = stringResource(R.string.copied)
    val noMapAppStr = stringResource(R.string.no_map_app)
    val subjectDefaultStr = stringResource(R.string.subject_default)
    val descTimeStr = stringResource(R.string.desc_time)

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ThemedCard(Modifier.fillMaxWidth(), materialColor = MaterialTheme.colorScheme.surfaceContainer, glassmorphismEnabled = vm.glassmorphismEnabled) { 
                 Column(Modifier.padding(24.dp)) { 
                    Text(item.subject?.get(lang) ?: subjectDefaultStr, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = descTimeStr, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                        AssistChip(onClick = {}, label = { Text(typeName) })
                        if (item.stream?.numeric != null) { AssistChip(onClick = {}, label = { Text("$groupLabel $groupValue") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) } 
                    } 
                } 
            }
            
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.current_performance), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            ThemedCard(modifier = Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) {
                if (subjectGrades != null) {
                    Column { 
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                            ScoreColumn(stringResource(R.string.m1), subjectGrades.marklist?.point1)
                            ScoreColumn(stringResource(R.string.m2), subjectGrades.marklist?.point2)
                            ScoreColumn(stringResource(R.string.exam_short), subjectGrades.marklist?.finalScore)
                            ScoreColumn(stringResource(R.string.total_short), subjectGrades.marklist?.total, true) 
                        } 
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.no_grades), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) 
                    }
                }
            }

            val teacherName = item.teacher?.get()
            if (isValid(teacherName)) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.teacher), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) { 
                    Row(verticalAlignment = Alignment.CenterVertically) { 
                        Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp))
                        Text(teacherName!!, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(teacherName)); Toast.makeText(context, copiedStr, Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy), tint = MaterialTheme.colorScheme.outline) } 
                    } 
                }
            }

            val roomName = item.room?.name_en
            val bName = item.classroom?.building?.getName(lang)
            val bAddr = item.classroom?.building?.getAddress(lang)

            if (isValid(roomName) || isValid(bName) || isValid(bAddr)) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.location), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                ThemedCard(modifier = Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) { 
                    Column { 
                        if (isValid(roomName)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Outlined.MeetingRoom, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) { Text(stringResource(R.string.room), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline); Text(roomName!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) } 
                            }
                        }
                        if (isValid(roomName) && (isValid(bName) || isValid(bAddr))) {
                            HorizontalDivider(modifier = Modifier.padding(vertical=12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if (isValid(bName) || isValid(bAddr)) {
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) { 
                                    if (isValid(bAddr)) Text(bAddr!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    if (isValid(bName)) Text(bName!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) 
                                }
                                if (isValid(bName)) {
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(bName!!)); Toast.makeText(context, copiedStr, Toast.LENGTH_SHORT).show() }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy), tint = MaterialTheme.colorScheme.outline) }
                                    IconButton(onClick = { 
                                        val locationName = bName
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(locationName)}")); try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, noMapAppStr, Toast.LENGTH_SHORT).show() } 
                                    }) { Icon(Icons.Outlined.Map, stringResource(R.string.map), tint = MaterialTheme.colorScheme.primary) }
                                }
                            } 
                        }
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
    val context = LocalContext.current
    val glassmorphismEnabled = vm.glassmorphismEnabled
    val containerColor = if (glassmorphismEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    val elevation = if (glassmorphismEnabled) 0.dp else 12.dp
    val shape = RoundedCornerShape(36.dp)

    Surface(
        modifier = Modifier
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
            .defaultMinSize(minHeight = 72.dp)
            .widthIn(max = 400.dp)
            .fillMaxWidth()
            .then(
                if (glassmorphismEnabled) {
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = containerColor,
        shadowElevation = elevation
    ) {
        AnimatedContent(
            targetState = when {
                vm.generatedPdfUri != null -> "SUCCESS"
                vm.isPdfGenerating -> "LOADING"
                else -> "DEFAULT"
            },
            label = "PdfBarAnimation",
            transitionSpec = { fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith fadeOut(animationSpec = tween(90)) }
        ) { state ->
            when (state) {
                "SUCCESS" -> {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .height(IntrinsicSize.Max),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { 
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(vm.generatedPdfUri, "application/pdf"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK }
                                    context.startActivity(intent)
                                } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.error_no_pdf_viewer), Toast.LENGTH_SHORT).show() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).fillMaxHeight()
                        ) { Text(stringResource(R.string.open), textAlign = TextAlign.Center) }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        IconButton(
                            onClick = { vm.clearPdfState() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxHeight()
                        ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.desc_close)) }
                    }
                }
                "LOADING" -> {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 16.dp, horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(text = vm.pdfStatusMessage ?: stringResource(R.string.generating_pdf), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = { vm.cancelPdfGeneration() }, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel)) }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val buttonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        
                        Button(
                            onClick = onGenerateRu, 
                            colors = buttonColors, 
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).defaultMinSize(minHeight = 48.dp).fillMaxHeight()
                        ) { 
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.pdf_ru), textAlign = TextAlign.Center) 
                        }
                        
                        Button(
                            onClick = onGenerateEn, 
                            colors = buttonColors, 
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).defaultMinSize(minHeight = 48.dp).fillMaxHeight()
                        ) { 
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.pdf_en), textAlign = TextAlign.Center) 
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceView(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current; val user = vm.userData; val profile = vm.profileData; val mov = profile?.studentMovement
    val activeSemester = profile?.active_semester ?: 1; val course = (activeSemester + 1) / 2
    val lang = vm.language; val isWebsiteMode = vm.downloadMode == "WEBSITE"
    val facultyName = mov?.faculty?.get(lang) ?: mov?.speciality?.faculty?.get(lang) ?: "-"

    LaunchedEffect(vm.pdfStatusMessage) { vm.pdfStatusMessage?.let { msg -> if (!vm.isPdfGenerating) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.reference_title)) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back)) } }, actions = { if (isWebsiteMode) { IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/studentCertificate" }) { Icon(Icons.Default.Print, stringResource(R.string.print_download)) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.widthIn(max = 840.dp).align(Alignment.TopCenter).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ThemedCard(Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) {
                    Column(Modifier.padding(8.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { OshSuLogo(modifier = Modifier.width(180.dp).height(60.dp), themeMode = vm.themeMode); Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.cert_header), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface) }
                        Spacer(Modifier.height(24.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.cert_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${user?.last_name} ${user?.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(24.dp))
                        RefDetailRow(stringResource(R.string.student_id), "${user?.id}"); RefDetailRow(stringResource(R.string.faculty), facultyName); RefDetailRow(stringResource(R.string.speciality), mov?.speciality?.get(lang) ?: "-")
                        RefDetailRow(stringResource(R.string.year_of_study), "$course ($activeSemester ${stringResource(R.string.sem_short)})"); RefDetailRow(stringResource(R.string.edu_form), mov?.edu_form?.get(lang) ?: "-"); RefDetailRow(stringResource(R.string.payment), if (mov?.id_payment_form == 2) stringResource(R.string.contract) else stringResource(R.string.budget))
                        Spacer(Modifier.height(32.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = Color(0xFF00FF88), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("${stringResource(R.string.active_student)} • ${SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00FF88)) }
                    }
                }
                Spacer(Modifier.height(24.dp)); Text(stringResource(R.string.preview_msg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(stringResource(R.string.download_msg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
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
    LaunchedEffect(vm.pdfStatusMessage) { vm.pdfStatusMessage?.let { msg -> if (!vm.isPdfGenerating) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.transcript_title)) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back)) } }, actions = { if (isWebsiteMode) { IconButton(onClick = { vm.webDocumentUrl = "https://myedu.oshsu.kg/#/Transcript" }) { Icon(Icons.Default.Print, stringResource(R.string.print_download)) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (vm.isTranscriptLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else if (vm.transcriptData.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_grades), color = MaterialTheme.colorScheme.onSurface) }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize().align(Alignment.TopCenter).widthIn(max = 840.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)) {
                    vm.transcriptData.forEach { yearData ->
                        item { Spacer(Modifier.height(16.dp)); Text(yearData.eduYear ?: stringResource(R.string.unknown_year), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        yearData.semesters?.forEach { sem ->
                            item { Spacer(Modifier.height(12.dp)); Text(sem.semesterName ?: stringResource(R.string.semester), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)) }
                            items(sem.subjects ?: emptyList()) { sub ->
                                // Fixed: Removed themeMode
                                ThemedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) { 
                                            Text(sub.subjectName ?: stringResource(R.string.subject_default), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            val codeLabel = stringResource(R.string.code); val creditsLabel = stringResource(R.string.credits)
                                            Text(text = "$codeLabel: ${sub.code ?: "-"} • $creditsLabel: ${sub.credit?.toInt() ?: 0}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) { val total = sub.markList?.total?.toInt() ?: 0; Text("$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (total >= 50) Color(0xFF00FF88) else MaterialTheme.colorScheme.error); Text(sub.examRule?.alphabetic ?: "-", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) { FloatingPdfBar(vm = vm, onGenerateRu = { vm.generateTranscriptPdf(context, "ru") }, onGenerateEn = { vm.generateTranscriptPdf(context, "en") }) }
        }
    }
}

@Composable
fun RefDetailRow(label: String, value: String?) { 
    if (!isValid(value)) return
    Column(Modifier.padding(bottom = 16.dp)) { 
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) 
    } 
}
