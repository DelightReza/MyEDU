package myedu.oshsu.kg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import myedu.oshsu.kg.R

@Composable
fun OshSuLogo(modifier: Modifier = Modifier, themeMode: String = "SYSTEM") {
    val context = LocalContext.current
    // In Glass mode, use dark logo without tint for better visibility on light/colorful background
    // In Glass Dark mode, use white logo for visibility on dark background
    val url = "file:///android_asset/logo-dark4.svg"
    val imageLoader = remember { ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build() }
    
    val isDark = when(themeMode) {
        "DARK" -> true
        "GLASS" -> false  // Use dark logo (no tint) in Glass mode
        "GLASS_DARK" -> true  // Use white logo in Glass Dark mode
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    AsyncImage(
        model = url, 
        imageLoader = imageLoader, 
        contentDescription = stringResource(R.string.desc_logo), 
        modifier = modifier, 
        contentScale = ContentScale.Fit,
        colorFilter = if (isDark) ColorFilter.tint(Color.White) else null
    )
}

@Composable
fun ThemedBackground(themeMode: String = "SYSTEM", glassmorphismEnabled: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background surface
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            // Add darker gradient overlay for Glass mode
            if (glassmorphismEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
            }
        }
        
        // Content
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyEduPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    themeMode: String = "SYSTEM",
    content: @Composable BoxScope.() -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        content()
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    materialColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    glassmorphismEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp) // More rounded corners
    
    // For glassmorphism: higher transparency, subtle border, no blur on content
    val cardColor = if (glassmorphismEnabled) {
        materialColor.copy(alpha = 0.3f) // More transparent for glass effect
    } else {
        materialColor
    }
    
    val cardModifier = if (glassmorphismEnabled) {
        modifier.then(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape))
    } else {
        modifier
    }
    
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (glassmorphismEnabled) 0.dp else 4.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = cardColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(20.dp), content = content) // Increased padding
        }
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape = shape,
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (glassmorphismEnabled) 0.dp else 4.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = cardColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(20.dp), content = content) // Increased padding
        }
    }
}

@Composable
fun BeautifulDocButton(
    text: String,
    icon: ImageVector,
    themeMode: String = "SYSTEM",
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    glassmorphismEnabled: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val elevation = if (glassmorphismEnabled) 0.dp else 4.dp
    
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .then(
                if (glassmorphismEnabled) {
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        enabled = !isLoading,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp) // Slightly larger icon
                )
                Spacer(Modifier.width(12.dp)) // More spacing
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, // Bolder text
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun InfoSection(title: String, themeMode: String = "SYSTEM") {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String?, themeMode: String = "SYSTEM", glassmorphismEnabled: Boolean = false) {
    val cleaned = value?.trim()
    if (cleaned.isNullOrEmpty() || 
        cleaned.equals("null", true) || 
        cleaned == "-" || 
        cleaned == "0" || 
        cleaned == "Unknown" || 
        cleaned == "Неизвестно" || 
        cleaned == "Белгисиз"
    ) return

    ThemedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        materialColor = MaterialTheme.colorScheme.surfaceContainerLow,
        glassmorphismEnabled = glassmorphismEnabled
    ) { 
        Row(verticalAlignment = Alignment.CenterVertically) { 
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp)) 
            Column { 
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    cleaned,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ) 
            } 
        } 
    }
}

@Composable
fun ScoreColumn(label: String, score: Double?, isTotal: Boolean = false) {
    val text = score?.toInt()?.toString() ?: "-"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isTotal && (score ?: 0.0) >= 50) Color(0xFF4CAF50)
                  else if (isTotal) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsDropdown(
    label: String, 
    options: List<Pair<String, String>>, 
    currentValue: String, 
    onOptionSelected: (String) -> Unit,
    themeMode: String = "SYSTEM",
    glassmorphismEnabled: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val displayValue = options.find { it.second == currentValue }?.first ?: options.first().first
    
    val containerColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val menuColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (glassmorphismEnabled) 0.2f else 1f)
    val shape = RoundedCornerShape(16.dp)

    Column {
        InfoSection(label, themeMode)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { dropdownWidth = it.width }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (glassmorphismEnabled) {
                            Modifier.border(0.5.dp, borderColor, shape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { expanded = true },
                shape = shape,
                color = containerColor,
                border = if (!glassmorphismEnabled) BorderStroke(2.dp, borderColor) else null,
                shadowElevation = if (glassmorphismEnabled) 0.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp), // More padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayValue,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.desc_dropdown),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { dropdownWidth.toDp() })
                    .background(menuColor)
            ) {
                options.forEach { (name, value) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = name, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontWeight = if(value == currentValue) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        onClick = { 
                            onOptionSelected(value)
                            expanded = false 
                        }
                    )
                }
            }
        }
    }
}
