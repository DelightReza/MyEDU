package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val profile = vm.profileData
    var showNewsSheet by remember { mutableStateOf(false) }
    
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = when (currentHour) {
        in 4..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else -> stringResource(R.string.good_evening)
    }
    
    // Using custom pull to refresh wrapper
    MyEduPullToRefreshBox(
        isRefreshing = vm.isRefreshing,
        onRefresh = { vm.refresh() },
        themeMode = vm.themeMode
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = 100.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                // --- HEADER SECTION ---
                item {
                    Row(
                        Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { 
                            Text(greetingText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Text(text = vm.customName ?: vm.userData?.name ?: stringResource(R.string.student_default), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OshSuLogo(modifier = Modifier.width(100.dp).height(40.dp), themeMode = vm.themeMode)
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(40.dp).clickable { showNewsSheet = true }, contentAlignment = Alignment.Center) { 
                                if (vm.newsList.isNotEmpty()) { 
                                    BadgedBox(badge = { Badge { Text("${vm.newsList.size}", color = MaterialTheme.colorScheme.onError) } }) { Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.desc_announcements), tint = MaterialTheme.colorScheme.primary) } 
                                } else { 
                                    Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.desc_announcements), tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                                } 
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // --- STATS CARDS ---
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
                        StatCard(
                            icon = Icons.Outlined.CalendarToday, 
                            label = stringResource(R.string.semester), 
                            value = profile?.active_semester?.toString() ?: "-", 
                            secondaryText = "${stringResource(R.string.stream)} ${vm.determinedStream ?: "-"}",
                            modifier = Modifier.weight(1f),
                            glassmorphismEnabled = vm.glassmorphismEnabled,
                            themeMode = vm.themeMode
                        )
                        val groupNum = vm.determinedGroup?.toString()
                        val groupName = profile?.studentMovement?.avn_group_name
                        StatCard(
                            icon = Icons.Outlined.Groups, 
                            label = stringResource(R.string.group), 
                            value = groupNum ?: groupName ?: "-", 
                            secondaryText = if (groupNum != null) groupName else null, 
                            modifier = Modifier.weight(1f),
                            glassmorphismEnabled = vm.glassmorphismEnabled,
                            themeMode = vm.themeMode
                        ) 
                    }
                    Spacer(Modifier.height(32.dp))
                }
                
                // --- WIDGET PROMOTION CARD ---
                item {
                    var showWidgetPromotion by remember { mutableStateOf(vm.loadShowWidgetPromotion()) }
                    
                    if (showWidgetPromotion) {
                        WidgetPromotionCard(
                            glassmorphismEnabled = vm.glassmorphismEnabled,
                            onAddWidget = { vm.requestAddWidget() },
                            onDismiss = {
                                showWidgetPromotion = false
                                vm.saveShowWidgetPromotion(false)
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }

                // --- TITLE ---
                item {
                    val todayString = stringResource(R.string.today)
                    val displayDay = vm.todayDayName.ifEmpty { todayString }
                    val title = if (displayDay == todayString) {
                        stringResource(R.string.todays_classes)
                    } else {
                        "$displayDay: ${stringResource(R.string.nav_schedule)}"
                    }

                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(16.dp))
                }

                // --- CLASSES LIST ---
                if (vm.todayClasses.isEmpty()) {
                    item {
                        ThemedCard(modifier = Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) { 
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Outlined.Weekend, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.no_classes_today), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) 
                            } 
                        }
                    }
                } else {
                    items(vm.todayClasses) { item ->
                        ClassItem(
                            item = item, 
                            timeString = vm.getTimeString(item.id_lesson),
                            vm = vm,
                            glassmorphismEnabled = vm.glassmorphismEnabled,
                            onClick = { vm.selectedClass = item }
                        )
                    }
                }
            }
        }
    }
    
    if (showNewsSheet) { 
        ModalBottomSheet(onDismissRequest = { showNewsSheet = false }) { 
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) { 
                Text(stringResource(R.string.announcements), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LazyColumn { 
                    items(vm.newsList) { news -> 
                        ThemedCard(
                            modifier = Modifier.padding(top=8.dp).fillMaxWidth(),
                            materialColor = MaterialTheme.colorScheme.surfaceVariant,
                            glassmorphismEnabled = vm.glassmorphismEnabled
                        ) { 
                            Column { 
                                Text(news.title?:"", fontWeight=FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(news.message?:"", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                            } 
                        } 
                    } 
                } 
            } 
        } 
    }
}

// Re-introducing StatCard locally since it was missing
@Composable
fun StatCard(
    icon: ImageVector, 
    label: String, 
    value: String, 
    secondaryText: String? = null, 
    modifier: Modifier = Modifier, 
    glassmorphismEnabled: Boolean = false, 
    themeMode: String = "SYSTEM"
) {
    ThemedCard(modifier = modifier, materialColor = MaterialTheme.colorScheme.surfaceContainer, glassmorphismEnabled = glassmorphismEnabled) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, 
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) { 
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer, 
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = value, 
                    style = MaterialTheme.typography.headlineLarge, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.primary, 
                    textAlign = TextAlign.Center
                )
                if (secondaryText != null) { 
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = secondaryText, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis, 
                        textAlign = TextAlign.Center
                    ) 
                }
            }
        }
    }
}

@Composable
fun WidgetPromotionCard(
    glassmorphismEnabled: Boolean = false,
    onAddWidget: () -> Unit,
    onDismiss: () -> Unit
) {
    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        materialColor = MaterialTheme.colorScheme.primaryContainer,
        glassmorphismEnabled = glassmorphismEnabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_widget_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.add_widget_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAddWidget,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_widget_button))
                }
            }
            // Just × icon, clickable
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 8.dp, y = (-8).dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}
