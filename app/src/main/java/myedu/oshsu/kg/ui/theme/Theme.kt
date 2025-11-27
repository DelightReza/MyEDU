package myedu.oshsu.kg.ui.theme

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

// --- GRADIENTS ---
val AccentGradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))

// Liquid Glass (Dark Mode)
val LiquidBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F2027),
        Color(0xFF203A43),
        Color(0xFF2C5364)
    )
)

// Aqua Flow (Light Mode)
val AquaBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        AquaLightStart,
        AquaLightEnd
    )
)

// --- COLOR SCHEMES ---

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryBlue,
    onSecondary = OnSecondaryBlue,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = OnPrimaryBlueDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryBlueDark,
    onSecondary = OnSecondaryBlueDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
)

// 1. LIQUID GLASS (Dark Theme + Transparency)
private val GlassColorScheme = darkColorScheme(
    primary = Color(0xFF00C6FF),
    onPrimary = Color.White,
    secondary = Color(0xFF0072FF),
    onSecondary = Color.White,
    background = Color.Transparent, 
    onBackground = Color.White,
    surface = Color.Transparent, 
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C5364),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

// 2. AQUA FLOW (Light Theme + Transparency)
private val AquaColorScheme = lightColorScheme(
    primary = Color(0xFF00796B), // Teal
    onPrimary = Color.White,
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    background = Color.Transparent, 
    onBackground = TextDarkTeal,
    surface = Color.Transparent, 
    onSurface = TextDarkTeal,
    surfaceVariant = Color(0xFFB2DFDB), // Used for fallback card colors
    onSurfaceVariant = TextDarkTeal.copy(alpha = 0.7f)
)

@Composable
fun MyEduTheme(
    themeMode: String,
    systemDark: Boolean = isSystemInDarkTheme(), 
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Determine strict Dark Mode logic
    val isDark = when(themeMode) {
        "LIGHT", "AQUA" -> false // Aqua is Light
        "DARK", "GLASS" -> true  // Glass is Dark
        else -> systemDark
    }

    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        themeMode == "GLASS" -> GlassColorScheme
        themeMode == "AQUA" -> AquaColorScheme
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Icon Contrast Logic
            // If theme is Light (Aqua), we need Dark Icons (true).
            // If theme is Dark (Glass), we need Light Icons (false).
            // Standard themes follow isDark.
            val useLightIcons = when(themeMode) {
                "GLASS" -> false // Dark background -> Light icons
                "AQUA" -> true   // Light background -> Dark icons
                else -> !isDark
            }
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = useLightIcons
            insetsController.isAppearanceLightNavigationBars = useLightIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
