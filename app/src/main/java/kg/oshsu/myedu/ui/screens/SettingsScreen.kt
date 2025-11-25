package kg.oshsu.myedu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kg.oshsu.myedu.MainViewModel
import kg.oshsu.myedu.ui.components.InfoSection
import kg.oshsu.myedu.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = if (vm.isGlass) 
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                else TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- APPEARANCE SECTION ---
            InfoSection("Appearance", vm.isGlass)
            
            ThemedCard(modifier = Modifier.fillMaxWidth(), isGlass = vm.isGlass) {
                Column {
                    ThemeOptionItem("Follow System", vm.themeMode == "SYSTEM", vm.isGlass) { vm.setTheme("SYSTEM") }
                    HorizontalDivider(color = if (vm.isGlass) Color.White.copy(0.2f) else MaterialTheme.colorScheme.outlineVariant)
                    ThemeOptionItem("Light Mode", vm.themeMode == "LIGHT", vm.isGlass) { vm.setTheme("LIGHT") }
                    HorizontalDivider(color = if (vm.isGlass) Color.White.copy(0.2f) else MaterialTheme.colorScheme.outlineVariant)
                    ThemeOptionItem("Dark Mode", vm.themeMode == "DARK", vm.isGlass) { vm.setTheme("DARK") }
                    HorizontalDivider(color = if (vm.isGlass) Color.White.copy(0.2f) else MaterialTheme.colorScheme.outlineVariant)
                    ThemeOptionItem("Liquid Glass", vm.themeMode == "GLASS", vm.isGlass) { vm.setTheme("GLASS") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- LANGUAGE SECTION (Placeholder) ---
            InfoSection("Language", vm.isGlass)
            ThemedCard(modifier = Modifier.fillMaxWidth(), isGlass = vm.isGlass) {
                Column {
                    ThemeOptionItem("English", true, vm.isGlass) { }
                    // Add more languages here later
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- APP INFO ---
            InfoSection("About", vm.isGlass)
            ThemedCard(modifier = Modifier.fillMaxWidth(), isGlass = vm.isGlass) {
                Column {
                    Text("MyEDU Mobile", fontWeight = FontWeight.Bold, color = if(vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = if(vm.isGlass) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ThemeOptionItem(text: String, selected: Boolean, isGlass: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected, 
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = if(isGlass) Color(0xFF00C6FF) else MaterialTheme.colorScheme.primary,
                unselectedColor = if(isGlass) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
