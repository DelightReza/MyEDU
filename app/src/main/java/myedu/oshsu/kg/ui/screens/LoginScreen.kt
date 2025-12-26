package myedu.oshsu.kg.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.DebugLogger
import myedu.oshsu.kg.MainActivity
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.secretDebugTrigger
import myedu.oshsu.kg.ui.components.OshSuLogo
import myedu.oshsu.kg.ui.components.SettingsDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val inputColors = OutlinedTextFieldDefaults.colors()

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .secretDebugTrigger {
                    vm.isDebugPipVisible = !vm.isDebugPipVisible
                    val msg = if(vm.isDebugPipVisible) context.getString(R.string.debug_enabled) else context.getString(R.string.debug_disabled)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    DebugLogger.log("UI", msg)
                }
        ) { 
            Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.desc_settings), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp)) 
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).padding(24.dp).systemBarsPadding(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                OshSuLogo(modifier = Modifier.width(260.dp).height(100.dp), themeMode = vm.themeMode); Spacer(Modifier.height(48.dp))
                OutlinedTextField(value = vm.loginEmail, onValueChange = { vm.loginEmail = it }, label = { Text(stringResource(R.string.email)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = inputColors, shape = RoundedCornerShape(16.dp)); Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = vm.loginPass, onValueChange = { vm.loginPass = it }, label = { Text(stringResource(R.string.password)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), colors = inputColors, shape = RoundedCornerShape(16.dp)); Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.rememberMe, onCheckedChange = { vm.rememberMe = it }); Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.remember_me), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                }
                if (vm.errorMsg != null) { Spacer(Modifier.height(16.dp)); Text(vm.errorMsg!!, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { vm.login(vm.loginEmail, vm.loginPass) }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp), 
                    enabled = !vm.isLoading, 
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) { 
                    if (vm.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text(stringResource(R.string.sign_in), fontWeight = FontWeight.Bold) 
                }
            }
        }
        if (showSettingsSheet) {
            ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
                    Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp), color = MaterialTheme.colorScheme.onSurface)
                    // Using Shared Component
                    SettingsDropdown(label = stringResource(R.string.appearance), options = listOf(stringResource(R.string.follow_system) to "SYSTEM", stringResource(R.string.light_mode) to "LIGHT", stringResource(R.string.dark_mode) to "DARK"), currentValue = vm.themeMode, onOptionSelected = { vm.setTheme(it) }, themeMode = vm.themeMode)
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsDropdown(label = stringResource(R.string.language), options = listOf("English" to "en", "Русский" to "ru", "Кыргызча" to "ky"), currentValue = vm.language, onOptionSelected = { selectedLang -> if (vm.language != selectedLang) { vm.setAppLanguage(selectedLang); (context as? MainActivity)?.restartApp() } }, themeMode = vm.themeMode)
                }
            }
        }
    }
}
