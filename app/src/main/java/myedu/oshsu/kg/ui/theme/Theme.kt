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

val LiquidBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F2027), // Deep Dark Blue
        Color(0xFF203A43), // Tealish Dark
        Color(0xFF2C5364)  // Lighter Teal/Blue
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
    tertiary = TertiaryCyan,
    onTertiary = OnTertiaryCyan,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
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
    tertiary = Color(0xFF4FD8EB),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF97F0FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
)

// Custom Glass Scheme (Forces Dark Mode styling + Transparent Backgrounds)
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

    // Dynamic Color is available on Android 12+ (S)
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        themeMode == "GLASS" -> GlassColorScheme
        dynamicColor -> {
            // Material You Dynamic Colors
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-Edge: Transparent bars
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Icon contrast logic
            // Glass Mode always has a dark background -> Light Icons (isAppearanceLightStatusBars = false)
            // Material Mode -> Depends on isDark
            val useLightIcons = if (themeMode == "GLASS") false else !isDark
            
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
