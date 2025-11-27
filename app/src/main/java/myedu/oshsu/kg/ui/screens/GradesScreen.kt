package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ui.components.ScoreColumn
import myedu.oshsu.kg.ui.components.ThemedCard

@Composable
fun GradesScreen(vm: MainViewModel) {
    val session = vm.sessionData
    val activeSemId = vm.profileData?.active_semester
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (vm.isGradesLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)) {
                item { Spacer(Modifier.height(16.dp)); Text("Current Session", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(16.dp)) }
                if (session.isEmpty()) {
                    item { Text("No grades available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    val currentSem = session.find { it.semester?.id == activeSemId } ?: session.lastOrNull()
                    if (currentSem != null) {
                        item { Text(currentSem.semester?.name_en ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
                        items(currentSem.subjects ?: emptyList()) { sub ->
                            ThemedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column {
                                    Text(sub.subject?.get() ?: "Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        ScoreColumn("M1", sub.marklist?.point1)
                                        ScoreColumn("M2", sub.marklist?.point2)
                                        ScoreColumn("Exam", sub.marklist?.finally)
                                        ScoreColumn("Total", sub.marklist?.total, true)
                                    }
                                }
                            }
                        }
                    } else { item { Text("Semester data not found.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
