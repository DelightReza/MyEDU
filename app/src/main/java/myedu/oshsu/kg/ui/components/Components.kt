package myedu.oshsu.kg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import myedu.oshsu.kg.ui.theme.*

// --- UI COMPONENT: LOGO ---
@Composable
fun OshSuLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = "file:///android_asset/logo-dark4.svg"
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    
    AsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = "OshSU Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        // Tint white in Glass mode, otherwise rely on the SVG colors (or tint onPrimaryContainer for Dark Mode)
        colorFilter = if (MaterialTheme.colorScheme.background == Color.Transparent) ColorFilter.tint(Color.White) else null
    )
}

// --- ABSTRACTION: THEMED BACKGROUND ---
@Composable
fun ThemedBackground(isGlass: Boolean, content: @Composable BoxScope.() -> Unit) {
    if (isGlass) {
        Box(
            modifier = Modifier.fillMaxSize().background(LiquidBackgroundBrush),
            content = content
        )
    } else {
        // Material Mode: Standard background (reacts to Light/Dark)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = { Box(Modifier.fillMaxSize(), content = content) }
        )
    }
}

// --- ABSTRACTION: THEMED CARD ---
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    isGlass: Boolean,
    onClick: (() -> Unit)? = null,
    glassAlpha: Float = 0.10f,
    // Default to a tonal surface color for MD3 (Surface Container Low/High/etc)
    // In M3 1.2+, we can use MaterialTheme.colorScheme.surfaceContainer if available,
    // or stick to explicit overrides in usage. Default to surfaceVariant logic here.
    materialColor: Color = MaterialTheme.colorScheme.surfaceContainer, // M3 1.2+ property
    content: @Composable ColumnScope.() -> Unit
) {
    if (isGlass) {
        // GLASS MODE
        Surface(
            modifier = modifier
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White.copy(alpha = glassAlpha),
            shape = RoundedCornerShape(24.dp),
            onClick = onClick ?: {},
            content = { Column(Modifier.padding(16.dp), content = content) }
        )
    } else {
        // MATERIAL 3 MODE (Standard Elevated Card)
        val shape = RoundedCornerShape(16.dp) // MD3 Card Shape
        
        if (onClick != null) {
            ElevatedCard(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.elevatedCardColors(containerColor = materialColor),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                content = { Column(Modifier.padding(16.dp), content = content) }
            )
        } else {
            ElevatedCard(
                modifier = modifier,
                shape = shape,
                colors = CardDefaults.elevatedCardColors(containerColor = materialColor),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                content = { Column(Modifier.padding(16.dp), content = content) }
            )
        }
    }
}

// --- COMPONENT: BEAUTIFUL DOC BUTTON ---
@Composable
fun BeautifulDocButton(
    text: String,
    icon: ImageVector,
    isGlass: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    
    val containerModifier = if (isGlass) {
        modifier
            .height(56.dp)
            .background(GlassWhite, shape)
            .border(1.dp, AccentGradient, shape)
            .clip(shape)
            .clickable(enabled = !isLoading, onClick = onClick)
    } else {
        // Material 3 Style: Secondary Container
        modifier
            .height(56.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .clip(shape)
            .clickable(enabled = !isLoading, onClick = onClick)
    }
    
    Box(contentAlignment = Alignment.Center, modifier = containerModifier) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), 
                color = if(isGlass) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    null, 
                    tint = if(isGlass) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text, 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if(isGlass) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// --- HELPER UI: TEXT SECTION ---
@Composable
fun InfoSection(title: String, isGlass: Boolean) { 
    Text(
        title, 
        style = MaterialTheme.typography.labelLarge, 
        color = if(isGlass) Color.White.copy(0.8f) else MaterialTheme.colorScheme.primary, 
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
    ) 
}

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String, isGlass: Boolean) {
    ThemedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
        isGlass = isGlass,
        // Use a lower surface container for simple lists in MD3
        materialColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            Icon(
                icon, 
                null, 
                tint = if(isGlass) Color(0xFF00C6FF) else MaterialTheme.colorScheme.secondary, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column { 
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if(isGlass) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = if(isGlass) Color.White else MaterialTheme.colorScheme.onSurface
                ) 
            } 
        } 
    }
}
