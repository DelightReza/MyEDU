package myedu.oshsu.kg.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ScheduleItem
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.ThemedCard
import myedu.oshsu.kg.ui.theme.GlassWhite
import myedu.oshsu.kg.ui.theme.MilkyGlass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val tabs = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = vm.selectedScheduleDay) { tabs.size }

    LaunchedEffect(pagerState.currentPage) { vm.selectedScheduleDay = pagerState.currentPage }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CenterAlignedTopAppBar(title = { OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp), themeMode = vm.themeMode) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp)) { pageIndex ->
                val dayClasses = vm.fullSchedule.filter { it.day == pageIndex }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    if (dayClasses.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                Icon(Icons.Outlined.Weekend, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Text("No classes", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            } 
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(horizontal = 16.dp)) { 
                            items(dayClasses) { item -> ClassItem(item, vm.getTimeString(item.id_lesson), vm.themeMode) { vm.selectedClass = item } } 
                            item { Spacer(Modifier.height(80.dp)) } 
                        }
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 16.dp, end = 16.dp)) {
                FloatingDayTabs(tabs = tabs, selectedIndex = pagerState.currentPage, themeMode = vm.themeMode, onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } })
            }
        }
    }
}

@Composable
fun FloatingDayTabs(tabs: List<String>, selectedIndex: Int, themeMode: String, onTabSelected: (Int) -> Unit) {
    val containerColor = when(themeMode) {
        "AQUA" -> MilkyGlass
        "GLASS" -> Color(0xFF0F2027).copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val border = if (themeMode == "GLASS" || themeMode == "AQUA") BorderStroke(1.dp, if(themeMode == "AQUA") Color.White.copy(0.5f) else GlassWhite) else null
    val elevation = if (themeMode == "GLASS" || themeMode == "AQUA") 0.dp else 4.dp

    Surface(modifier = Modifier.height(56.dp).fillMaxWidth().widthIn(max = 600.dp), shape = RoundedCornerShape(28.dp), color = containerColor, border = border, shadowElevation = elevation) {
        Row(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val selectedBg = MaterialTheme.colorScheme.primary
                val unselectedBg = Color.Transparent
                val selectedTxt = MaterialTheme.colorScheme.onPrimary
                val unselectedTxt = MaterialTheme.colorScheme.onSurfaceVariant
                
                val bgColor by animateColorAsState(if (isSelected) selectedBg else unselectedBg, label = "bg")
                val txtColor by animateColorAsState(if (isSelected) selectedTxt else unselectedTxt, label = "txt")
                val scale by animateFloatAsState(if (isSelected) 1f else 0.9f, label = "scale")

                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight().scale(scale).clip(CircleShape).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTabSelected(index) }) {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = txtColor)
                }
            }
        }
    }
}

@Composable
fun ClassItem(item: ScheduleItem, timeString: String, themeMode: String, onClick: () -> Unit) {
    val streamInfo = if (item.stream?.numeric != null) { val type = item.subject_type?.get(); if (type == "Lecture") "Stream ${item.stream.numeric}" else "Group ${item.stream.numeric}" } else ""
    
    ThemedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), themeMode = themeMode) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            val timeBg = if (themeMode == "AQUA") Color.White.copy(alpha = 0.5f) 
                         else if (themeMode == "GLASS") GlassWhite 
                         else MaterialTheme.colorScheme.surfaceContainerHigh
            
            // COLOR LOGIC: If Aqua, use Primary color (Teal) for Subject and Time to make it pop. Otherwise standard OnSurface.
            val contentColor = if (themeMode == "AQUA") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                modifier = Modifier
                    .width(50.dp)
                    .background(timeBg, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp)
            ) { 
                Text("${item.id_lesson}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    timeString.split("-").firstOrNull()?.trim() ?: "", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = contentColor // Applied to Start Time
                ) 
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) { 
                Text(
                    item.subject?.get() ?: "Subject", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.SemiBold, 
                    color = contentColor // Applied to Subject
                )
                
                val metaText = buildString { 
                    append(item.room?.name_en ?: "Room ?")
                    append(" • ")
                    append(item.subject_type?.get() ?: "Lesson")
                    if (streamInfo.isNotEmpty()) { 
                        append(" • ")
                        append(streamInfo) 
                    } 
                }
                Text(metaText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) 
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
        } 
    }
}
