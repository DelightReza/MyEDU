package myedu.oshsu.kg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
fun OshSuLogo(modifier: Modifier = Modifier, themeMode: String = "SYSTEM") {
    val context = LocalContext.current
    val url = "file:///android_asset/logo-dark4.svg"
    
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    
    val isGlassDark = themeMode == "GLASS"
    val isDarkMode = themeMode == "DARK"
    
    AsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = "OshSU Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = if (isGlassDark || isDarkMode) ColorFilter.tint(Color.White) else null
    )
}

// --- ABSTRACTION: THEMED BACKGROUND ---
@Composable
fun ThemedBackground(themeMode: String, content: @Composable BoxScope.() -> Unit) {
    when (themeMode) {
        "GLASS" -> Box(Modifier.fillMaxSize().background(LiquidBackgroundBrush), content = content)
        "AQUA" -> Box(Modifier.fillMaxSize().background(AquaBackgroundBrush), content = content)
        else -> Surface(
            Modifier.fillMaxSize(), 
            color = MaterialTheme.colorScheme.background,
            content = { Box(Modifier.fillMaxSize(), content = content) }
        )
    }
}

// --- COMPONENT: PULL TO REFRESH BOX ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEduPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    themeMode: String,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    
    // Determine colors based on theme
    val indicatorColor = if (themeMode == "GLASS" || themeMode == "AQUA") {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val containerColor = if (themeMode == "GLASS") {
        Color(0xFF0F2027) 
    } else if (themeMode == "AQUA") {
        Color(0xFFE0F7FA)
    } else {
        MaterialTheme.colorScheme.surface
    }

    if (state.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }

    // Sync state with ViewModel
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            state.startRefresh()
        } else {
            state.endRefresh()
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(state.nestedScrollConnection)
            .fillMaxSize()
    ) {
        content()

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
            containerColor = containerColor,
            contentColor = indicatorColor
        )
    }
}

// --- ABSTRACTION: THEMED CARD ---
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    themeMode: String,
    onClick: (() -> Unit)? = null,
    materialColor: Color = MaterialTheme.colorScheme.surfaceContainer, 
    content: @Composable ColumnScope.() -> Unit
) {
    val isGlassMode = themeMode == "GLASS" || themeMode == "AQUA"
    
    if (isGlassMode) {
        // GLASS LOGIC
        val glassColor = if (themeMode == "AQUA") MilkyGlass else GlassWhite
        val borderColor = if (themeMode == "AQUA") MilkyBorder else GlassBorder
        
        Surface(
            modifier = modifier
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            color = glassColor,
            shape = RoundedCornerShape(24.dp),
            onClick = onClick ?: {},
            content = { Column(Modifier.padding(16.dp), content = content) }
        )
    } else {
        // STANDARD MATERIAL LOGIC
        val shape = RoundedCornerShape(16.dp)
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
    themeMode: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val isGlassMode = themeMode == "GLASS" || themeMode == "AQUA"
    val shape = RoundedCornerShape(16.dp)
    
    val containerModifier = if (isGlassMode) {
        val bgColor = if(themeMode == "AQUA") MilkyGlass else GlassWhite
        val borderBrush = if(themeMode == "AQUA") SolidColor(MilkyBorder) else AccentGradient
        
        modifier
            .height(56.dp)
            .background(bgColor, shape)
            .border(1.dp, borderBrush, shape)
            .clip(shape)
            .clickable(enabled = !isLoading, onClick = onClick)
    } else {
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
                color = MaterialTheme.colorScheme.onSurface,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    null, 
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text, 
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// --- HELPER UI ---
@Composable
fun InfoSection(title: String, themeMode: String) { 
    Text(
        title, 
        style = MaterialTheme.typography.labelLarge, 
        color = MaterialTheme.colorScheme.primary, 
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
    ) 
}

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String, themeMode: String) {
    ThemedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
        themeMode = themeMode,
        materialColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            Icon(
                icon, 
                null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column { 
                Text(
                    title, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            } 
        } 
    }
}

@Composable
fun ScoreColumn(label: String, score: Double?, isTotal: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${score?.toInt() ?: 0}", 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold, 
            color = if (isTotal && (score ?: 0.0) >= 50) Color(0xFF4CAF50) else if (isTotal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
