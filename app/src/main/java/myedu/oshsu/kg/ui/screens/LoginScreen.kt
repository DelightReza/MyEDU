package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainActivity
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.theme.AccentGradient
import myedu.oshsu.kg.ui.theme.GlassBorder
import myedu.oshsu.kg.ui.theme.MilkyGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel) {
    // Note: Local email/pass state variables removed; we use VM state now.
    var showSettingsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val inputColors = if (vm.isGlass) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings, 
                contentDescription = "Settings", 
                tint = if (vm.themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.size(28.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(24.dp)
                    .systemBarsPadding(), 
                verticalArrangement = Arrangement.Center, 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OshSuLogo(modifier = Modifier.width(260.dp).height(100.dp), themeMode = vm.themeMode)
                Spacer(Modifier.height(48.dp))
                
                OutlinedTextField(
                    value = vm.loginEmail, 
                    onValueChange = { vm.loginEmail = it }, 
                    label = { Text(stringResource(R.string.email)) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true, 
                    colors = inputColors, 
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = vm.loginPass, 
                    onValueChange = { vm.loginPass = it }, 
                    label = { Text(stringResource(R.string.password)) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true, 
                    visualTransformation = PasswordVisualTransformation(), 
                    colors = inputColors, 
                    shape = RoundedCornerShape(16.dp)
                )
                
                // --- REMEMBER ME CHECKBOX ---
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chkColors = if (vm.isGlass) {
                         CheckboxDefaults.colors(
                             checkedColor = MaterialTheme.colorScheme.primary,
                             uncheckedColor = MaterialTheme.colorScheme.outline,
                             checkmarkColor = Color.White
                         )
                    } else CheckboxDefaults.colors()

                    Checkbox(
                        checked = vm.rememberMe, 
                        onCheckedChange = { vm.rememberMe = it },
                        colors = chkColors
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.remember_me),
                        color = if(vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // ----------------------------
                
                if (vm.errorMsg != null) { 
                    Spacer(Modifier.height(16.dp))
                    Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error) 
                }
                Spacer(Modifier.height(32.dp))
                
                val btnModifier = Modifier.fillMaxWidth().height(56.dp)
                val finalBtnMod = if (vm.themeMode == "GLASS") btnModifier.background(AccentGradient, RoundedCornerShape(16.dp)) else btnModifier
                
                val btnColors = if (vm.themeMode == "GLASS") {
                    ButtonDefaults.buttonColors(containerColor = Color.Transparent) 
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = { vm.login(vm.loginEmail, vm.loginPass) }, 
                    modifier = finalBtnMod, 
                    enabled = !vm.isLoading,
                    colors = btnColors,
                    shape = RoundedCornerShape(16.dp)
                ) { 
                    if (vm.isLoading) {
                        CircularProgressIndicator(color = if(vm.themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onPrimary) 
                    } else {
                        Text(stringResource(R.string.sign_in), fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
        
        if (showSettingsSheet) {
            val sheetColor = when(vm.themeMode) {
                "AQUA" -> MilkyGlass
                "GLASS" -> Color(0xFF0F2027).copy(alpha = 0.95f)
                else -> MaterialTheme.colorScheme.surface
            }
            
            val contentColor = if(vm.themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface
            val dragHandleColor = if(vm.themeMode == "GLASS") Color.White.copy(0.5f) else MaterialTheme.colorScheme.onSurfaceVariant

            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = sheetColor,
                contentColor = contentColor,
                dragHandle = { BottomSheetDefaults.DragHandle(color = dragHandleColor) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        stringResource(R.string.settings), 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.padding(bottom = 24.dp),
                        color = contentColor
                    )

                    SettingsDropdown(
                        label = stringResource(R.string.appearance),
                        options = listOf(
                            stringResource(R.string.follow_system) to "SYSTEM",
                            stringResource(R.string.light_mode) to "LIGHT",
                            stringResource(R.string.dark_mode) to "DARK",
                            stringResource(R.string.liquid_glass) to "GLASS",
                            stringResource(R.string.aqua_flow) to "AQUA"
                        ),
                        currentValue = vm.themeMode,
                        onOptionSelected = { vm.setTheme(it) },
                        themeMode = vm.themeMode
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDropdown(
                        label = stringResource(R.string.docs_download),
                        options = listOf(
                            stringResource(R.string.in_app_pdf) to "IN_APP",
                            stringResource(R.string.website_official) to "WEBSITE"
                        ),
                        currentValue = vm.downloadMode,
                        onOptionSelected = { vm.setDocMode(it) },
                        themeMode = vm.themeMode
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDropdown(
                        label = stringResource(R.string.language),
                        options = listOf("English" to "en", "Русский" to "ru"),
                        currentValue = vm.language,
                        onOptionSelected = { selectedLang ->
                            if (vm.language != selectedLang) {
                                vm.setAppLanguage(selectedLang)
                                (context as? MainActivity)?.restartApp()
                            }
                        },
                        themeMode = vm.themeMode
                    )
                }
            }
        }
    }
}
