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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val user = vm.userData
    val profile = vm.profileData
    var showNewsSheet by remember { mutableStateOf(false) }
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) { if (currentHour in 4..11) R.string.good_morning else if (currentHour in 12..16) R.string.good_afternoon else R.string.good_evening }
    
    val displayName = vm.customName ?: user?.name ?: stringResource(R.string.student_default)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { 
                    Text(stringResource(greetingText), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = displayName, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis, 
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp), themeMode = vm.themeMode)
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(40.dp).clickable { showNewsSheet = true }, contentAlignment = Alignment.Center) {
                        if (vm.newsList.isNotEmpty()) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) { Text("${vm.newsList.size}") } }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.desc_announcements), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.desc_announcements), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            val streamLabel = stringResource(R.string.stream)
            val activeSemesterNum = profile?.active_semester?.toString() ?: "-"
            val streamText = vm.determinedStream?.let { "$streamLabel $it" }
            val rawGroupNum = vm.determinedGroup
            val rawAvnName = profile?.studentMovement?.avn_group_name
            val displayGroupValue = rawGroupNum?.toString() ?: rawAvnName ?: "-"
            val groupSecondaryText = if (rawGroupNum != null && rawAvnName != null && rawAvnName != rawGroupNum.toString() && rawAvnName != "0") rawAvnName else null

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
                StatCard(icon = Icons.Outlined.CalendarToday, label = stringResource(R.string.semester), value = activeSemesterNum, secondaryText = streamText, modifier = Modifier.weight(1f).fillMaxHeight())
                StatCard(icon = Icons.Outlined.Groups, label = stringResource(R.string.group), value = displayGroupValue, secondaryText = groupSecondaryText, modifier = Modifier.weight(1f).fillMaxHeight()) 
            }
            Spacer(Modifier.height(32.dp))
            Text("${vm.todayDayName}: ${stringResource(R.string.todays_classes)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            if (vm.todayClasses.isEmpty()) {
                ThemedCard(modifier = Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Weekend, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(16.dp)); Text(stringResource(R.string.no_classes_today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) } } 
            } else {
                vm.todayClasses.forEach { item -> ClassItem(item, vm.getTimeString(item.id_lesson), vm) { vm.selectedClass = item } } 
            }
            Spacer(Modifier.height(80.dp))
        }
    }    
    if (showNewsSheet) {
        ModalBottomSheet(onDismissRequest = { showNewsSheet = false }, containerColor = BottomSheetDefaults.ContainerColor) { 
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { 
                Text(stringResource(R.string.announcements), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyColumn { items(vm.newsList) { news -> ThemedCard(Modifier.padding(top=8.dp).fillMaxWidth(), materialColor = MaterialTheme.colorScheme.surfaceVariant) { Column { Text(news.title?:"", fontWeight=FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(news.message?:"", color = MaterialTheme.colorScheme.onSurfaceVariant) } } } } 
            } 
        } 
    }
}

@Composable
fun StatCard(icon: ImageVector, label: String, value: String, secondaryText: String? = null, modifier: Modifier = Modifier) {
    ThemedCard(modifier = modifier) { 
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { 
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp))
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center); Spacer(Modifier.height(4.dp))
                Text(text = value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                if (secondaryText != null) { Spacer(Modifier.height(4.dp)); Text(text = secondaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
            }
        }
    }
}
