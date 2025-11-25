package kg.oshsu.myedu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kg.oshsu.myedu.MainViewModel
import kg.oshsu.myedu.ui.components.*
import kg.oshsu.myedu.ui.theme.AccentGradient
import kg.oshsu.myedu.ui.theme.GlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val user = vm.userData; val profile = vm.profileData; val pay = vm.payStatus
    val fullName = "${user?.last_name ?: ""} ${user?.name ?: ""}".trim().ifEmpty { "Student" }
    val facultyName = profile?.studentMovement?.faculty?.let { it.name_en ?: it.name_ru } ?: profile?.studentMovement?.speciality?.faculty?.let { it.name_en ?: it.name_ru } ?: "-"
    val specialityName = profile?.studentMovement?.speciality?.let { it.name_en ?: it.name_ru } ?: "-"

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Header with Settings Icon
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { vm.showSettingsScreen = true }) {
                    Icon(Icons.Outlined.Settings, "Settings")
                }
            }
            
            Spacer(Modifier.height(24.dp))
            val imgMod = if(vm.isGlass) Modifier.size(128.dp).background(AccentGradient, CircleShape).padding(3.dp).clip(CircleShape).background(Color(0xFF0F2027)) else Modifier.size(128.dp).background(MaterialTheme.colorScheme.primary, CircleShape).padding(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            Box(contentAlignment = Alignment.Center, modifier = imgMod) { AsyncImage(model = profile?.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)) }
            
            Spacer(Modifier.height(16.dp))
            Text(fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(24.dp))
            if (pay != null) {
                ThemedCard(Modifier.fillMaxWidth(), vm.isGlass) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Tuition Contract", fontWeight = FontWeight.Bold); Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.paid_summa?.toInt() ?: 0} KGS", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88)) }; Column(horizontalAlignment = Alignment.End) { Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.need_summa?.toInt() ?: 0} KGS", style = MaterialTheme.typography.titleMedium) } }
                        val debt = pay.getDebt(); if (debt > 0) { Spacer(Modifier.height(8.dp)); if(vm.isGlass) HorizontalDivider(color=GlassBorder) else HorizontalDivider(); Spacer(Modifier.height(8.dp)); Text("Remaining: ${debt.toInt()} KGS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                        if (!pay.access_message.isNullOrEmpty()) { Spacer(Modifier.height(12.dp)); if(vm.isGlass) HorizontalDivider(Modifier.padding(bottom=8.dp), color=GlassBorder) else HorizontalDivider(Modifier.padding(bottom=8.dp)); pay.access_message.forEach { msg -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) } } }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            InfoSection("Documents", vm.isGlass)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BeautifulDocButton(
                    text = "Reference",
                    icon = Icons.Default.Description,
                    isGlass = vm.isGlass,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.showReferenceScreen = true }
                )
                BeautifulDocButton(
                    text = "Transcript",
                    icon = Icons.Default.School,
                    isGlass = vm.isGlass,
                    isLoading = vm.isTranscriptLoading,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.fetchTranscript() }
                )
            }
            Spacer(Modifier.height(24.dp)); InfoSection("Academic", vm.isGlass)
            DetailCard(Icons.Outlined.School, "Faculty", facultyName, vm.isGlass)
            DetailCard(Icons.Outlined.Book, "Speciality", specialityName, vm.isGlass)
            Spacer(Modifier.height(24.dp)); InfoSection("Personal", vm.isGlass)
            DetailCard(Icons.Outlined.Badge, "Passport", profile?.pdsstudentinfo?.getFullPassport() ?: "-", vm.isGlass); DetailCard(Icons.Outlined.Phone, "Phone", profile?.pdsstudentinfo?.phone ?: "-", vm.isGlass)
            Spacer(Modifier.height(32.dp)); Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) { Text("Log Out") }; Spacer(Modifier.height(80.dp))
        }
    }
}
