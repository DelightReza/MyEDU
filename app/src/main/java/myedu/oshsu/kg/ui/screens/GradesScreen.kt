package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.ScoreColumn
import myedu.oshsu.kg.ui.components.ThemedCard
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun GradesScreen(vm: MainViewModel) {
    val session = vm.sessionData
    val activeSemId = vm.profileData?.active_semester
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (vm.isGradesLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } } else {
            LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)) {
                item { Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.current_session), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(16.dp)) }
                if (session.isEmpty()) { item { Text(stringResource(R.string.no_grades), color = MaterialTheme.colorScheme.onSurfaceVariant) } } else {
                    val currentSem = session.find { it.semester?.id == activeSemId } ?: session.lastOrNull()
                    if (currentSem != null) {
                        item { Text(currentSem.semester?.name_en ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
                        items(currentSem.subjects ?: emptyList()) { sub ->
                            ThemedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column {
                                    // SUBJECT NAME
                                    Text(
                                        sub.subject?.get(vm.language) ?: stringResource(R.string.subject_default), 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    
                                    // SCORES ROW
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        ScoreColumn(stringResource(R.string.m1), sub.marklist?.point1)
                                        ScoreColumn(stringResource(R.string.m2), sub.marklist?.point2)
                                        ScoreColumn(stringResource(R.string.exam_short), sub.marklist?.finally)
                                        ScoreColumn(stringResource(R.string.total_short), sub.marklist?.total, true)
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    // FOOTER ROW (Active Date & Updated At)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Side: Active Status
                                        if (sub.graphic != null && !sub.graphic.begin.isNullOrEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE91E63), // Pinkish Red like screenshot
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                val range = formatActiveDateRange(sub.graphic.begin, sub.graphic.end)
                                                Text(
                                                    text = stringResource(R.string.label_active, range),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFE91E63),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.width(1.dp)) // Spacer to keep alignment if left side empty
                                        }

                                        // Right Side: Updated At
                                        val updatedStr = sub.marklist?.updatedAt
                                        if (!updatedStr.isNullOrEmpty()) {
                                            Text(
                                                text = stringResource(R.string.label_updated, formatUpdatedDate(updatedStr)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else { item { Text(stringResource(R.string.semester_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// Parses "2025-10-27 00:00:00" -> "27 Oct"
private fun formatActiveDateRange(start: String?, end: String?): String {
    if (start == null || end == null) return "?"
    try {
        // Input format from graphic
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        // Output format (e.g. 27 Oct)
        val formatter = SimpleDateFormat("dd MMM", Locale.US)
        
        val startDate = parser.parse(start)
        val endDate = parser.parse(end)
        
        return "${formatter.format(startDate!!)} - ${formatter.format(endDate!!)}"
    } catch (e: Exception) {
        return "?"
    }
}

// Parses "2024-11-06T08:43:51.000000Z" -> "06 Nov 14:43"
private fun formatUpdatedDate(isoDate: String): String {
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC") // Input is UTC
        
        val date = parser.parse(isoDate)
        
        val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) // Output in Local Time
        return formatter.format(date!!)
    } catch (e: Exception) {
        return isoDate.take(10)
    }
}
