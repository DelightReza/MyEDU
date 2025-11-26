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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Custom colors for Glass mode input fields
    val inputColors = if (vm.isGlass) {
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00C6FF),
            unfocusedBorderColor = GlassBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF00C6FF),
            cursorColor = Color.White
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // --- 1. SETTINGS BUTTON (Top Right) ---
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding() // Ensures it doesn't overlap status bar
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        // --- 2. LOGIN FORM (Centered) ---
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
                OshSuLogo(modifier = Modifier.width(260.dp).height(100.dp))
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
                
                if (vm.errorMsg != null) { Spacer(Modifier.height(16.dp)); Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(32.dp))
                
                val btnModifier = Modifier.fillMaxWidth().height(56.dp)
                val finalBtnMod = if (vm.isGlass) btnModifier.background(AccentGradient, RoundedCornerShape(16.dp)) else btnModifier
                val btnColors = if (vm.isGlass) ButtonDefaults.buttonColors(containerColor = Color.Transparent) else ButtonDefaults.buttonColors()

                Button(
                    onClick = { vm.login(email, pass) }, 
                    modifier = finalBtnMod, 
                    enabled = !vm.isLoading,
                    colors = btnColors,
                    shape = RoundedCornerShape(16.dp)
                ) { 
                    if (vm.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text("Sign In", fontWeight = FontWeight.Bold) 
                }
            }
        }
        
        // --- 3. SETTINGS SHEET ---
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = if (vm.isGlass) Color(0xFF0F2027) else MaterialTheme.colorScheme.surface,
                contentColor = if (vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = if(vm.isGlass) Color.White.copy(0.5f) else MaterialTheme.colorScheme.onSurfaceVariant) }
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp) // Extra padding for bottom nav area
                ) {
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // --- REUSING DROPDOWNS FROM SETTINGS SCREEN ---
                    
                    SettingsDropdown(
                        label = "Appearance",
                        options = listOf(
                            "Follow System" to "SYSTEM",
                            "Light Mode" to "LIGHT",
                            "Dark Mode" to "DARK",
                            "Liquid Glass" to "GLASS"
                        ),
                        currentValue = vm.themeMode,
                        onOptionSelected = { vm.setTheme(it) },
                        isGlass = vm.isGlass
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
                        isGlass = vm.isGlass
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsDropdown(
                        label = "Language",
                        options = listOf("English" to "en"),
                        currentValue = "en",
                        onOptionSelected = { },
                        isGlass = vm.isGlass
                    )
                }
            }
        }
    }
}
