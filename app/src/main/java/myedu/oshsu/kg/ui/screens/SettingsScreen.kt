package myedu.oshsu.kg.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainActivity
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.InfoSection
import myedu.oshsu.kg.ui.components.ThemedCard
import myedu.oshsu.kg.ui.theme.MilkyGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    
    // FIX: Retrieve resource string outside of remember block
    val unknownStr = stringResource(R.string.unknown)
    
    val appVersion = remember {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else { context.packageManager.getPackageInfo(context.packageName, 0) }
            pInfo.versionName
        } catch (e: Exception) { unknownStr }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.desc_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            SettingsDropdown(
                label = stringResource(R.string.appearance),
                options = listOf(
                    stringResource(R.string.follow_system) to "SYSTEM", 
                    stringResource(R.string.light_mode) to "LIGHT", 
                    stringResource(R.string.dark_mode) to "DARK", 
                    stringResource(R.string.liquid_glass) to "GLASS", 
                    stringResource(R.string.aqua_flow) to "AQUA"
                ),
                currentValue = vm.themeMode, onOptionSelected = { vm.setTheme(it) }, themeMode = vm.themeMode
            )
            Spacer(modifier = Modifier.height(24.dp))
            SettingsDropdown(
                label = stringResource(R.string.docs_download),
                options = listOf(
                    stringResource(R.string.in_app_pdf) to "IN_APP", 
                    stringResource(R.string.website_official) to "WEBSITE"
                ),
                currentValue = vm.downloadMode, onOptionSelected = { vm.setDocMode(it) }, themeMode = vm.themeMode
            )
            Spacer(modifier = Modifier.height(24.dp))
            SettingsDropdown(
                label = stringResource(R.string.language),
                options = listOf("English" to "en", "Русский" to "ru", "Кыргызча" to "ky"),
                currentValue = vm.language,
                onOptionSelected = { selectedLang -> if (vm.language != selectedLang) { vm.setAppLanguage(selectedLang); (context as? MainActivity)?.restartApp() } },
                themeMode = vm.themeMode
            )
            Spacer(modifier = Modifier.height(32.dp))
            InfoSection(stringResource(R.string.about), vm.themeMode)
            ThemedCard(modifier = Modifier.fillMaxWidth(), themeMode = vm.themeMode) {
                Column {
                    Text(stringResource(R.string.app_name_display), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${stringResource(R.string.version_prefix)} $appVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                modifier = Modifier.fillMaxWidth().clickable { expanded = true }, shape = RoundedCornerShape(12.dp), color = containerColor, border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = displayValue, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                    Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.desc_dropdown), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(
                expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.width(with(LocalDensity.current) { dropdownWidth.toDp() }).background(menuColor)
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
