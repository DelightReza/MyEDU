package kg.oshsu.myedu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kg.oshsu.myedu.MainViewModel
import kg.oshsu.myedu.ui.components.OshSuLogo
import kg.oshsu.myedu.ui.theme.AccentGradient
import kg.oshsu.myedu.ui.theme.GlassBorder

@Composable
fun LoginScreen(vm: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).padding(24.dp).systemBarsPadding(), 
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
}
