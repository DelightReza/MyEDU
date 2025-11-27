package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ui.components.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val user = vm.userData; val profile = vm.profileData; var showNewsSheet by remember { mutableStateOf(false) }
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) { if(currentHour in 4..11) "Good Morning," else if(currentHour in 12..16) "Good Afternoon," else "Good Evening," }
    
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { 
                    Text(greetingText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = user?.name ?: "Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface) 
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp), themeMode = vm.themeMode)
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
                StatCard(Icons.Outlined.CalendarToday, "Semester", profile?.active_semester?.toString() ?: "-", vm.themeMode, Modifier.weight(1f))
                StatCard(Icons.Outlined.Groups, "Group", if (vm.determinedGroup != null) "Group ${vm.determinedGroup}" else profile?.studentMovement?.avn_group_name ?: "-", vm.themeMode, Modifier.weight(1f)) 
            }
            Spacer(Modifier.height(32.dp))
            Text("${vm.todayDayName}'s Classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            if (vm.todayClasses.isEmpty()) {
                ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Weekend, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Text("No classes today!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) } } 
            } else {
                vm.todayClasses.forEach { item -> ClassItem(item, vm.getTimeString(item.id_lesson), vm.themeMode) { vm.selectedClass = item } } 
            }
            Spacer(Modifier.height(80.dp))
        }
    }    
    if (showNewsSheet) {
        val containerColor = if(vm.isGlass) Color(0xFF16213E) else BottomSheetDefaults.ContainerColor
        ModalBottomSheet(onDismissRequest = { showNewsSheet = false }, containerColor = containerColor) { 
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { 
                Text("Announcements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyColumn { items(vm.newsList) { news -> ThemedCard(Modifier.padding(top=8.dp).fillMaxWidth(), themeMode = vm.themeMode, materialColor = MaterialTheme.colorScheme.surfaceVariant) { Column { Text(news.title?:"", fontWeight=FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(news.message?:"", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } 
            } 
        } 
    }
}

@Composable
fun StatCard(icon: ImageVector, label: String, value: String, themeMode: String, modifier: Modifier = Modifier) {
    ThemedCard(modifier = modifier, themeMode = themeMode) { 
        Column { 
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = if(value.length > 15) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface) 
        } 
    }
}
