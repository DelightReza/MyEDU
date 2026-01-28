package myedu.oshsu.kg.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainActivity
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.InfoSection
import myedu.oshsu.kg.ui.components.SettingsDropdown
import myedu.oshsu.kg.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
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
            // Using Shared Component
            SettingsDropdown(
                label = stringResource(R.string.appearance),
                options = listOf(
                    stringResource(R.string.follow_system) to "SYSTEM", 
                    stringResource(R.string.light_mode) to "LIGHT", 
                    stringResource(R.string.dark_mode) to "DARK",
                    stringResource(R.string.glass_mode) to "GLASS",
                    stringResource(R.string.glass_dark_mode) to "GLASS_DARK"
                ),
                currentValue = vm.themeMode, 
                onOptionSelected = { vm.setTheme(it) },
                themeMode = vm.themeMode,
                glassmorphismEnabled = vm.glassmorphismEnabled
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
                themeMode = vm.themeMode,
                glassmorphismEnabled = vm.glassmorphismEnabled
            )
            Spacer(modifier = Modifier.height(24.dp))
            SettingsDropdown(
                label = stringResource(R.string.language),
                options = listOf("English" to "en", "Русский" to "ru", "Кыргызча" to "ky"),
                currentValue = vm.language,
                onOptionSelected = { selectedLang -> if (vm.language != selectedLang) { vm.setAppLanguage(selectedLang); (context as? MainActivity)?.restartApp() } },
                themeMode = vm.themeMode,
                glassmorphismEnabled = vm.glassmorphismEnabled
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            InfoSection(stringResource(R.string.dict_tools_section), vm.themeMode)
            ThemedCard(
                modifier = Modifier.fillMaxWidth(), 
                onClick = { vm.showDictionaryScreen = true },
                glassmorphismEnabled = vm.glassmorphismEnabled
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.dict_open_btn), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.dict_open_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            InfoSection(stringResource(R.string.about), vm.themeMode)
            ThemedCard(modifier = Modifier.fillMaxWidth(), glassmorphismEnabled = vm.glassmorphismEnabled) {
                Column {
                    Text(stringResource(R.string.app_name_display), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${stringResource(R.string.version_prefix)} $appVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
