package myedu.oshsu.kg.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ScheduleItem
import myedu.oshsu.kg.ui.components.MyEduPullToRefreshBox
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.ThemedCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: MainViewModel) {
    val tabs = listOf(
        stringResource(R.string.mon), stringResource(R.string.tue), stringResource(R.string.wed),
        stringResource(R.string.thu), stringResource(R.string.fri), stringResource(R.string.sat)
    )
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = vm.selectedScheduleDay) { tabs.size }
    LaunchedEffect(pagerState.currentPage) { vm.selectedScheduleDay = pagerState.currentPage }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CenterAlignedTopAppBar(title = { OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp), themeMode = vm.themeMode) }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            
            // Floating Tabs Header
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                FloatingDayTabs(
                    tabs = tabs, 
                    selectedIndex = pagerState.currentPage, 
                    onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }, 
                    glassmorphismEnabled = vm.glassmorphismEnabled
                )
            }

            // Scrollable Content with Pull to Refresh
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                val dayClasses = vm.fullSchedule.filter { it.day == pageIndex }
                
                MyEduPullToRefreshBox(
                    isRefreshing = vm.isRefreshing,
                    onRefresh = { vm.refresh() },
                    themeMode = vm.themeMode
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        if (dayClasses.isEmpty()) {
                            // Empty State
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                                    Icon(Icons.Outlined.Weekend, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                                    Spacer(Modifier.height(16.dp))
                                    Text(stringResource(R.string.no_classes), color = MaterialTheme.colorScheme.onSurfaceVariant) 
                                } 
                            }
                        } else {
                            // Scrollable List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().widthIn(max = 840.dp), 
                                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp)
                            ) { 
                                items(dayClasses) { item -> 
                                    ClassItem(item, vm.getTimeString(item.id_lesson), vm, glassmorphismEnabled = vm.glassmorphismEnabled) { vm.selectedClass = item } 
                                } 
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingDayTabs(tabs: List<String>, selectedIndex: Int, onTabSelected: (Int) -> Unit, glassmorphismEnabled: Boolean = false) {
    val containerColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val elevation = if (glassmorphismEnabled) 0.dp else 4.dp

    Surface(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .then(
                if (glassmorphismEnabled) {
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        shadowElevation = elevation
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index
                val selectedBg = MaterialTheme.colorScheme.primary; val unselectedBg = Color.Transparent
                val selectedTxt = MaterialTheme.colorScheme.onPrimary; val unselectedTxt = MaterialTheme.colorScheme.onSurfaceVariant
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
fun ClassItem(item: ScheduleItem, timeString: String, vm: MainViewModel, glassmorphismEnabled: Boolean = false, onClick: () -> Unit) {
    val streamLabel = stringResource(R.string.stream); val groupLabel = stringResource(R.string.group_short); val roomLabel = stringResource(R.string.auditorium)
    
    val typeResId = vm.getSubjectTypeResId(item)
    val typeName = if (typeResId != null) stringResource(typeResId) else item.subject_type?.get(vm.language) ?: stringResource(R.string.lesson_default)

    val streamInfo = if (item.stream?.numeric != null) { 
        if (item.subject_type?.name_en?.contains("Lection", true) == true) "$streamLabel ${item.stream.numeric}" else "$groupLabel ${item.stream.numeric}" 
    } else ""
    val fullTime = if (!timeString.contains(":") && !timeString.contains("-")) stringResource(R.string.pair) + " $timeString" else timeString

    ThemedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), glassmorphismEnabled = glassmorphismEnabled) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            val timeBg = MaterialTheme.colorScheme.surfaceContainerHigh
            val contentColor = MaterialTheme.colorScheme.onSurface
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp).background(timeBg, RoundedCornerShape(8.dp)).padding(vertical = 8.dp)) { 
                Text("${item.id_lesson}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(fullTime.split("-").firstOrNull()?.trim() ?: "", style = MaterialTheme.typography.labelSmall, color = contentColor) 
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { 
                Text(item.subject?.get(vm.language) ?: stringResource(R.string.subject_default), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
                val metaText = buildString { append(item.room?.name_en ?: "$roomLabel ?"); append(" • "); append(typeName); if (streamInfo.isNotEmpty()) { append(" • "); append(streamInfo) } }
                Text(metaText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fullTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) 
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
        } 
    }
}
