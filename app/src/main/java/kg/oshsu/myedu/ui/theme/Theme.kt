package kg.oshsu.myedu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- UI CONSTANTS ---
val GlassWhite = Color.White.copy(alpha = 0.10f)
val GlassBorder = Color.White.copy(alpha = 0.20f)
val TextWhite = Color.White
val AccentGradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))

// Liquid Background Brush
val LiquidBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F2027), // Deep Dark Blue
        Color(0xFF203A43), // Tealish Dark
        Color(0xFF2C5364)  // Lighter Teal/Blue
    )
)

@Composable
fun MyEduTheme(
    themeMode: String,
    systemDark: Boolean = isSystemInDarkTheme(), 
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Determine strict Dark Mode based on user preference
    val isDark = when(themeMode) {
        "LIGHT" -> false
        "DARK", "GLASS" -> true
        else -> systemDark // SYSTEM
    }

    val colorScheme = when {
        themeMode == "GLASS" -> darkColorScheme(
            primary = Color(0xFF00C6FF),
            onPrimary = Color.White,
            secondary = Color(0xFF0072FF),
            onSecondary = Color.White,
            surface = Color.Transparent, 
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.7f),
            background = Color.Transparent,
            onBackground = Color.White
        )
        // Material Theme (System/Light/Dark) - Android 12+ Dynamic Colors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Icons: 
            // GLASS -> Always Light icons (false) because bg is dark
            // Material -> Depends on isDark
            val lightIcons = if (themeMode == "GLASS") false else !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightIcons
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
