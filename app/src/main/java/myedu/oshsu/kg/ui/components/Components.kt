package myedu.oshsu.kg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import myedu.oshsu.kg.R

@Composable
fun OshSuLogo(modifier: Modifier = Modifier, themeMode: String = "SYSTEM") {
    val context = LocalContext.current
    val url = "file:///android_asset/logo-dark4.svg"
    val imageLoader = remember { ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build() }
    
    val isDark = when(themeMode) {
        "DARK" -> true
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
fun ThemedBackground(themeMode: String = "SYSTEM", content: @Composable BoxScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
        content = { Box(Modifier.fillMaxSize(), content = content) }
    )
}

@Composable
fun MyEduPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    themeMode: String = "SYSTEM",
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    // Removed themeMode parameter entirely to prevent errors
    onClick: (() -> Unit)? = null,
    materialColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = materialColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        ElevatedCard(
            modifier = modifier,
            shape = shape,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = materialColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(Modifier.padding(16.dp), content = content)
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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
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
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun DetailCard(icon: ImageVector, title: String, value: String?, themeMode: String = "SYSTEM") {
    val cleaned = value?.trim()
    if (cleaned.isNullOrEmpty() || 
        cleaned.equals("null", true) || 
        cleaned == "-" || 
        cleaned == "0" || 
        cleaned == "Unknown" || 
        cleaned == "Неизвестно" || 
        cleaned == "Белгисиз"
    ) return

    ThemedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), materialColor = MaterialTheme.colorScheme.surfaceContainerLow) { 
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
                    cleaned,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
    themeMode: String = "SYSTEM"
) {
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val displayValue = options.find { it.second == currentValue }?.first ?: options.first().first
    
    val containerColor = MaterialTheme.colorScheme.surface
    val menuColor = MaterialTheme.colorScheme.surfaceContainer
    val borderColor = MaterialTheme.colorScheme.outline

    Column {
        InfoSection(label, themeMode)
        
        // Use Box with onSizeChanged to effectively behave like a "Full Drop-Down Card"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { dropdownWidth = it.width }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayValue,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
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
