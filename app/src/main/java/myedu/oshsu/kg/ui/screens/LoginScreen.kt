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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.theme.AccentGradient
import myedu.oshsu.kg.ui.theme.GlassBorder
import myedu.oshsu.kg.ui.theme.MilkyGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Input styling: Aqua uses Primary (Teal) colors, Glass uses White/Accent
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
        
        // --- SETTINGS BUTTON ---
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
                // Aqua -> Dark Icon, Glass -> White Icon
                tint = if (vm.themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onSurface, 
                modifier = Modifier.size(28.dp)
            )
        }

        // --- LOGIN FORM ---
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
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("Email") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true, 
                    colors = inputColors, 
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pass, 
                    onValueChange = { pass = it }, 
                    label = { Text("Password") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true, 
                    visualTransformation = PasswordVisualTransformation(), 
                    colors = inputColors, 
                    shape = RoundedCornerShape(16.dp)
                )
                
                if (vm.errorMsg != null) { 
                    Spacer(Modifier.height(16.dp))
                    Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error) 
                }
                Spacer(Modifier.height(32.dp))
                
                // Button styling
                val btnModifier = Modifier.fillMaxWidth().height(56.dp)
                // In Aqua, we want a solid Primary color. In Glass, we use the Gradient.
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
                    onClick = { vm.login(email, pass) }, 
                    modifier = finalBtnMod, 
                    enabled = !vm.isLoading,
                    colors = btnColors,
                    shape = RoundedCornerShape(16.dp)
                ) { 
                    if (vm.isLoading) {
                        CircularProgressIndicator(color = if(vm.themeMode == "GLASS") Color.White else MaterialTheme.colorScheme.onPrimary) 
                    } else {
                        Text("Sign In", fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
        
        // --- SETTINGS SHEET ---
        if (showSettingsSheet) {
            // FIX: Explicit color logic to ensure Aqua gets MilkyGlass (Light) and Glass gets Dark
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
                        "Settings", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.padding(bottom = 24.dp),
                        color = contentColor
                    )

                    SettingsDropdown(
                        label = "Appearance",
                        options = listOf(
                            "Follow System" to "SYSTEM",
                            "Light Mode" to "LIGHT",
                            "Dark Mode" to "DARK",
                            "Liquid Glass (Dark)" to "GLASS",
                            "Aqua Flow (Light)" to "AQUA"
                        ),
                        currentValue = vm.themeMode,
                        onOptionSelected = { vm.setTheme(it) },
                        themeMode = vm.themeMode
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDropdown(
                        label = "Documents Download",
                        options = listOf(
                            "In-App (PDF Generator)" to "IN_APP",
                            "Website (Official Portal)" to "WEBSITE"
                        ),
                        currentValue = vm.downloadMode,
                        onOptionSelected = { vm.setDocMode(it) },
                        themeMode = vm.themeMode
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDropdown(
                        label = "Language",
                        options = listOf("English" to "en"),
                        currentValue = "en",
                        onOptionSelected = { },
                        themeMode = vm.themeMode
                    )
                }
            }
        }
    }
}
