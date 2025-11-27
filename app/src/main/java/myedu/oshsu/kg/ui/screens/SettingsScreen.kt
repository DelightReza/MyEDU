package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.ui.components.InfoSection
import myedu.oshsu.kg.ui.components.ThemedCard
import myedu.oshsu.kg.ui.theme.MilkyGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SettingsDropdown(
                label = "Appearance",
                options = listOf("Follow System" to "SYSTEM", "Light Mode" to "LIGHT", "Dark Mode" to "DARK", "Liquid Glass" to "GLASS", "Aqua Flow" to "AQUA"),
                currentValue = vm.themeMode,
                onOptionSelected = { vm.setTheme(it) },
                themeMode = vm.themeMode
            )
            Spacer(modifier = Modifier.height(24.dp))
            SettingsDropdown(
                label = "Documents Download",
                options = listOf("In-App (PDF Generator)" to "IN_APP", "Website (Official Portal)" to "WEBSITE"),
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
            Spacer(modifier = Modifier.height(32.dp))
            InfoSection("About", vm.themeMode)
            ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode) {
                Column {
                    Text("MyEDU Mobile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Version 1.0.1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsDropdown(label: String, options: List<Pair<String, String>>, currentValue: String, onOptionSelected: (String) -> Unit, themeMode: String) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0) }
    val displayValue = options.find { it.second == currentValue }?.first ?: options.first().first

    val containerColor = if (themeMode == "AQUA") MilkyGlass else if (themeMode == "GLASS") Color(0xFF0F2027).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val menuColor = if (themeMode == "AQUA") Color.White else if (themeMode == "GLASS") Color(0xFF0F2027) else MaterialTheme.colorScheme.surfaceContainer
    val borderColor = if (themeMode == "GLASS" || themeMode == "AQUA") MaterialTheme.colorScheme.outline.copy(0.2f) else MaterialTheme.colorScheme.outline

    Column {
        InfoSection(label, themeMode)
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().onSizeChanged { size -> dropdownWidth = size.width }) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = displayValue, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(LocalDensity.current) { dropdownWidth.toDp() }).background(menuColor)
            ) {
                options.forEach { (name, value) ->
                    DropdownMenuItem(
                        text = { Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontWeight = if(value == currentValue) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onOptionSelected(value); expanded = false }
                    )
                }
            }
        }
    }
}
