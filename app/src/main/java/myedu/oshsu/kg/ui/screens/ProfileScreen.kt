package myedu.oshsu.kg.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.*
import myedu.oshsu.kg.ui.theme.AccentGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val user = vm.userData; val profile = vm.profileData; val pay = vm.payStatus
    val lang = vm.language
    val fullName = "${user?.last_name ?: ""} ${user?.name ?: ""}".trim().ifEmpty { stringResource(R.string.student_default) }
    val facultyName = profile?.studentMovement?.faculty?.get(lang) ?: profile?.studentMovement?.speciality?.faculty?.get(lang) ?: "-"
    val specialityName = profile?.studentMovement?.speciality?.get(lang) ?: "-"
    val dateFormat = stringResource(R.string.config_date_format)
    val dateStr = SimpleDateFormat(dateFormat, Locale.US).format(Date())

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 840.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { vm.showSettingsScreen = true }) { Icon(Icons.Outlined.Settings, stringResource(R.string.desc_settings), tint = MaterialTheme.colorScheme.onSurface) }
            }
            Spacer(Modifier.height(24.dp))
            val imgMod = Modifier.size(128.dp).background(AccentGradient, CircleShape).padding(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            Box(contentAlignment = Alignment.Center, modifier = imgMod) { AsyncImage(model = profile?.avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)) }
            Spacer(Modifier.height(16.dp))
            Text(fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if (pay != null) {
                ThemedCard(Modifier.fillMaxWidth(), vm.themeMode) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.tuition_contract), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(stringResource(R.string.paid), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.paid_summa?.toInt() ?: 0} ${stringResource(R.string.currency_kgs)}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88)) }; Column(horizontalAlignment = Alignment.End) { Text(stringResource(R.string.total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${pay.need_summa?.toInt() ?: 0} ${stringResource(R.string.currency_kgs)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) } }
                        val debt = pay.getDebt(); if (debt > 0) { Spacer(Modifier.height(8.dp)); HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.remaining, debt.toInt()), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                        if (!pay.access_message.isNullOrEmpty()) { Spacer(Modifier.height(12.dp)); HorizontalDivider(Modifier.padding(bottom=8.dp), color=MaterialTheme.colorScheme.outlineVariant); pay.access_message.forEach { msg -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) } } }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            InfoSection(stringResource(R.string.documents), vm.themeMode)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BeautifulDocButton(text = stringResource(R.string.reference), icon = Icons.Default.Description, themeMode = vm.themeMode, modifier = Modifier.weight(1f), onClick = { vm.showReferenceScreen = true })
                BeautifulDocButton(text = stringResource(R.string.transcript), icon = Icons.Default.School, themeMode = vm.themeMode, isLoading = vm.isTranscriptLoading, modifier = Modifier.weight(1f), onClick = { vm.fetchTranscript() })
            }
            Spacer(Modifier.height(24.dp)); InfoSection(stringResource(R.string.academic), vm.themeMode)
            DetailCard(Icons.Outlined.School, stringResource(R.string.faculty), facultyName, vm.themeMode); DetailCard(Icons.Outlined.Book, stringResource(R.string.speciality), specialityName, vm.themeMode)
            Spacer(Modifier.height(24.dp)); InfoSection(stringResource(R.string.personal), vm.themeMode)
            DetailCard(Icons.Outlined.Badge, stringResource(R.string.passport), profile?.pdsstudentinfo?.getFullPassport() ?: "-", vm.themeMode); DetailCard(Icons.Outlined.Phone, stringResource(R.string.phone), profile?.pdsstudentinfo?.phone ?: "-", vm.themeMode)
            Spacer(Modifier.height(32.dp)); Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.log_out)) }; Spacer(Modifier.height(130.dp))
        }
    }
}
