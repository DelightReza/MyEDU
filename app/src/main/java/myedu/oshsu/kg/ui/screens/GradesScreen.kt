package myedu.oshsu.kg.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import myedu.oshsu.kg.GraphicInfo
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.SortOption
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.ScoreColumn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(vm: MainViewModel) {
    val session = vm.sessionData
    val activeSemId = vm.profileData?.active_semester

    LaunchedEffect(session, activeSemId) {
        if (vm.selectedSemesterId == null && session.isNotEmpty()) {
            vm.selectedSemesterId = activeSemId ?: session.lastOrNull()?.semester?.id
        }
    }

    val currentSem = session.find { it.semester?.id == vm.selectedSemesterId }
        ?: session.find { it.semester?.id == activeSemId }
        ?: session.lastOrNull()
        
    val rawSubjects = currentSem?.subjects ?: emptyList()
    
    val sortedSubjects = remember(rawSubjects, vm.gradesSortOption) {
        when (vm.gradesSortOption) {
            SortOption.DEFAULT -> rawSubjects
            SortOption.ALPHABETICAL -> rawSubjects.sortedBy { it.subject?.get(vm.language) ?: "" }
            SortOption.LOWEST_FIRST -> rawSubjects.sortedBy { it.marklist?.total ?: 0.0 }
            SortOption.HIGHEST_FIRST -> rawSubjects.sortedByDescending { it.marklist?.total ?: 0.0 }
            SortOption.UPDATED_TIME -> rawSubjects.sortedByDescending { it.marklist?.updated_at ?: "" }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    OshSuLogo(modifier = Modifier.width(80.dp).height(32.dp), themeMode = vm.themeMode)
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 140.dp, bottom = 100.dp)
            ) {
                if (vm.isGradesLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (session.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_grades), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    if (sortedSubjects.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.semester_not_found), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        items(sortedSubjects) { sub ->
                            GradeItemCard(
                                subjectName = sub.subject?.get(vm.language) ?: stringResource(R.string.subject_default),
                                p1 = sub.marklist?.point1,
                                p2 = sub.marklist?.point2,
                                exam = sub.marklist?.finalScore,
                                total = sub.marklist?.total,
                                updatedAt = sub.marklist?.updated_at,
                                graphic = sub.graphic,
                                themeMode = vm.themeMode
                            )
                        }
                    }
                }
            }

            FloatingGradeHeader(
                vm = vm,
                session = session,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun FloatingGradeHeader(
    vm: MainViewModel, 
    session: List<myedu.oshsu.kg.SessionResponse>, 
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val elevation = 4.dp
    val activeSemId = vm.profileData?.active_semester
    val sortedSession = remember(session, activeSemId) {
        session.sortedWith(compareByDescending<myedu.oshsu.kg.SessionResponse> { 
            it.semester?.id == activeSemId 
        }.thenByDescending { 
            it.semester?.id 
        })
    }
    
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = Color.Transparent, 
        labelColor = MaterialTheme.colorScheme.onSurface
    )
    val chipBorder = FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = false,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha=0.3f),
        borderWidth = 1.dp
    )

    Surface(
        modifier = modifier.wrapContentHeight().fillMaxWidth().widthIn(max = 600.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sortedSession) { item ->
                        val semId = item.semester?.id ?: 0
                        val isSelected = semId == (vm.selectedSemesterId ?: 0)
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.selectedSemesterId = semId },
                            label = { Text(stringResource(R.string.semester_format, semId)) },
                            colors = chipColors,
                            border = if(isSelected) null else chipBorder,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                            enabled = true
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf(
                    SortOption.DEFAULT to R.string.sort_default,
                    SortOption.ALPHABETICAL to R.string.sort_az,
                    SortOption.UPDATED_TIME to R.string.sort_date,
                    SortOption.LOWEST_FIRST to R.string.sort_low,
                    SortOption.HIGHEST_FIRST to R.string.sort_high
                )
                items(options) { (option, labelRes) ->
                    val isSelected = vm.gradesSortOption == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.gradesSortOption = option },
                        label = { Text(stringResource(labelRes)) },
                        colors = chipColors,
                        border = if(isSelected) null else chipBorder,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(32.dp).padding(horizontal = 4.dp),
                        enabled = true
                    )
                }
            }
        }
    }
}

@Composable
fun GradeItemCard(
    subjectName: String,
    p1: Double?,
    p2: Double?,
    exam: Double?,
    total: Double?,
    updatedAt: String?,
    graphic: GraphicInfo?,
    themeMode: String
) {
    val isUploadActive = remember(graphic) {
        try {
            if (graphic?.begin != null && graphic.end != null) {
                val p = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                p.timeZone = TimeZone.getTimeZone("UTC")
                val s = p.parse(graphic.begin)
                val e = p.parse(graphic.end)
                val n = Date()
                n.after(s) && n.before(e)
            } else false
        } catch (e: Exception) { false }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val textColor = MaterialTheme.colorScheme.onSurface
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = dividerColor,
                thickness = 1.dp
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreColumn(stringResource(R.string.m1), p1)
                ScoreColumn(stringResource(R.string.m2), p2)
                ScoreColumn(stringResource(R.string.exam_short), exam)

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.total_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${total?.toInt() ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = getGradeColor(total)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUploadActive && graphic != null) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${formatDateShort(graphic.begin)} - ${formatDateShort(graphic.end)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val inactiveColor = MaterialTheme.colorScheme.outline
                        Icon(Icons.Outlined.EventBusy, null, tint = inactiveColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.status_not_active),
                            style = MaterialTheme.typography.bodySmall,
                            color = inactiveColor
                        )
                    }
                }

                if (updatedAt != null) {
                    Text(
                        text = "${formatDateFull(updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun getGradeColor(s: Double?): Color {
    val score = s ?: 0.0
    val isDark = isSystemInDarkTheme()
    return when {
        score >= 90 -> if(isDark) Color(0xFF82B1FF) else Color(0xFF2962FF) // Blue/Light Blue
        score >= 70 -> if(isDark) Color(0xFF81C784) else Color(0xFF2E7D32) // Green/Light Green
        score >= 60 -> if(isDark) Color(0xFFFFB74D) else Color(0xFFEF6C00) // Orange
        else -> if(isDark) Color(0xFFE57373) else Color(0xFFC62828)       // Red for <60 and 0
    }
}

private fun formatDateShort(d: String?) = try {
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(d!!)!!
    )
} catch (e: Exception) { "" }

private fun formatDateFull(d: String?) = try {
    val cleanDate = d!!.replace("T", " ").substringBefore(".")
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(cleanDate)!!
    SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(date)
} catch (e: Exception) { d ?: "" }
