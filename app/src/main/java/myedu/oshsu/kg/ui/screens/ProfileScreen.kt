package myedu.oshsu.kg.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import myedu.oshsu.kg.DebugLogger
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.secretDebugTrigger
import myedu.oshsu.kg.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val user = vm.userData
    val profile = vm.profileData
    val pay = vm.payStatus
    val lang = vm.language

    val displayPhoto = vm.customPhotoUri ?: profile?.avatar
    val displayName = vm.customName ?: "${user?.last_name ?: ""} ${user?.name ?: ""}".trim().ifEmpty { stringResource(R.string.student_default) }

    val facultyName = profile?.studentMovement?.faculty?.get(lang) ?: profile?.studentMovement?.speciality?.faculty?.get(lang) ?: "-"
    val specialityName = profile?.studentMovement?.speciality?.get(lang) ?: "-"

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                
                IconButton(
                    onClick = { vm.showSettingsScreen = true },
                    modifier = Modifier.secretDebugTrigger { 
                        vm.isDebugPipVisible = !vm.isDebugPipVisible
                        val msg = if(vm.isDebugPipVisible) context.getString(R.string.debug_enabled) else context.getString(R.string.debug_disabled)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        DebugLogger.log("UI", msg)
                    }
                ) { Icon(Icons.Outlined.Settings, stringResource(R.string.desc_settings), tint = MaterialTheme.colorScheme.onSurface) }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                key(displayPhoto, vm.avatarRefreshTrigger) {
                        AsyncImage(
                        model = ImageRequest.Builder(context).data(displayPhoto).crossfade(true).setParameter("retry_hash", vm.avatarRefreshTrigger).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // --- ACTION BUTTONS (Personal Info / Edit Profile) ---
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileActionButton(
                    text = stringResource(R.string.personal),
                    icon = Icons.Default.Person,
                    onClick = { vm.showPersonalInfoScreen = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                
                Spacer(Modifier.width(16.dp))
                
                ProfileActionButton(
                    text = stringResource(R.string.edit_profile),
                    icon = Icons.Default.Edit,
                    onClick = { vm.showEditProfileScreen = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(Modifier.height(32.dp))

            if (pay != null) {
                ThemedCard(Modifier.fillMaxWidth()) {
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
            
            // CHANGED: Use IntrinsicSize.Max to match heights
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max), 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BeautifulDocButton(
                    text = stringResource(R.string.reference), 
                    icon = Icons.Default.Description, 
                    themeMode = vm.themeMode, 
                    // CHANGED: Use fillMaxHeight
                    modifier = Modifier.weight(1f).fillMaxHeight(), 
                    onClick = { vm.showReferenceScreen = true }
                )
                BeautifulDocButton(
                    text = stringResource(R.string.transcript), 
                    icon = Icons.Default.School, 
                    themeMode = vm.themeMode, 
                    isLoading = vm.isTranscriptLoading, 
                    // CHANGED: Use fillMaxHeight
                    modifier = Modifier.weight(1f).fillMaxHeight(), 
                    onClick = { vm.fetchTranscript() }
                )
            }
            
            Spacer(Modifier.height(24.dp)); InfoSection(stringResource(R.string.academic), vm.themeMode)
            DetailCard(Icons.Outlined.School, stringResource(R.string.faculty), facultyName, vm.themeMode); DetailCard(Icons.Outlined.Book, stringResource(R.string.speciality), specialityName, vm.themeMode)
            Spacer(Modifier.height(24.dp)); InfoSection(stringResource(R.string.personal), vm.themeMode)
            DetailCard(Icons.Outlined.Badge, stringResource(R.string.passport), profile?.pdsstudentinfo?.getFullPassport() ?: "-", vm.themeMode); DetailCard(Icons.Outlined.Phone, stringResource(R.string.phone), profile?.pdsstudentinfo?.phone ?: "-", vm.themeMode)
            Spacer(Modifier.height(32.dp)); Button(onClick = { vm.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.log_out)) }; Spacer(Modifier.height(130.dp))
        }
    }
}

@Composable
fun ProfileActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val sizeModifier = modifier.defaultMinSize(minHeight = 48.dp)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = shape,
        modifier = sizeModifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize() 
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
