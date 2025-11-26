package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ui.components.InfoSection
import myedu.oshsu.kg.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = if (vm.isGlass) 
                    TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
            // --- APPEARANCE DROPDOWN ---
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

            // --- DOCUMENTS DROPDOWN ---
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

            // --- LANGUAGE DROPDOWN ---
            SettingsDropdown(
                label = "Language",
                options = listOf("English" to "en"), // Placeholder for future languages
                currentValue = "en",
                onOptionSelected = { },
                isGlass = vm.isGlass
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- ABOUT SECTION ---
            InfoSection("About", vm.isGlass)
            ThemedCard(modifier = Modifier.fillMaxWidth(), isGlass = vm.isGlass) {
                Column {
                    Text(
                        "MyEDU Mobile", 
                        fontWeight = FontWeight.Bold, 
                        color = if(vm.isGlass) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Version 1.0.1", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = if(vm.isGlass) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    options: List<Pair<String, String>>, // Pair(DisplayName, InternalValue)
    currentValue: String,
    onOptionSelected: (String) -> Unit,
    isGlass: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Find display name for current value
    val displayValue = options.find { it.second == currentValue }?.first ?: options.first().first

    val containerColor = if (isGlass) Color(0xFF0F2027).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val textColor = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurface
    val menuColor = if (isGlass) Color(0xFF0F2027) else MaterialTheme.colorScheme.surfaceContainer

    Column {
        InfoSection(label, isGlass)
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = if(isGlass) Color.White.copy(0.5f) else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if(isGlass) Color.White.copy(0.2f) else MaterialTheme.colorScheme.outline,
                    focusedTrailingIconColor = if(isGlass) Color.White else MaterialTheme.colorScheme.primary,
                    unfocusedTrailingIconColor = if(isGlass) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(menuColor)
            ) {
                options.forEach { (name, value) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = name, 
                                color = textColor,
                                fontWeight = if(value == currentValue) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        onClick = {
                            onOptionSelected(value)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
