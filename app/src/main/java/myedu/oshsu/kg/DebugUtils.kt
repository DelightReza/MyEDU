package myedu.oshsu.kg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- 1. SINGLETON LOGGER ---
object DebugLogger {
    // Observable list for Compose UI
    val logs = mutableStateListOf<String>()

    fun log(tag: String, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val logEntry = "[$time] $tag: $msg"
        
        synchronized(logs) {
            logs.add(0, logEntry)
            // Keep max 1000 lines to prevent memory issues in UI
            if (logs.size > 1000) logs.removeRange(1000, logs.size)
        }
        // Also print to Android Logcat
        Log.d("MyEduDebug", "[$tag] $msg")
    }

    fun clear() {
        logs.clear()
    }
}

// --- 2. CUSTOM MODIFIER (5-SECOND HOLD) ---
// Uses awaitEachGesture to spy on touches without consuming them.
fun Modifier.secretDebugTrigger(onTrigger: () -> Unit): Modifier = this.pointerInput(Unit) {
    coroutineScope {
        val currentInputScope = this@pointerInput
        currentInputScope.awaitEachGesture {
            // Wait for touch down
            awaitFirstDown(requireUnconsumed = false)
            
            // Start the timer
            val job = launch {
                delay(3000) // Wait exactly 3 seconds
                onTrigger()
            }
            
            try {
                // Wait for the finger to go up or cancel
                waitForUpOrCancellation()
            } finally {
                // If finger goes up before 5s, cancel the timer
                job.cancel()
            }
        }
    }
}

// --- 3. PIP BUTTON ---
@Composable
fun DebugPipButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Red,
        contentColor = Color.White,
        modifier = modifier.size(48.dp)
    ) {
        Icon(Icons.Default.BugReport, contentDescription = "Open Debug Console")
    }
}

// --- 4. CONSOLE UI OVERLAY ---
@Composable
fun DebugConsoleOverlay(onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    
    // Full screen semi-transparent background that catches clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                // Catch clicks to prevent interacting with the app behind the console
            }
            .padding(top = 48.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E), // Dark grey background
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
            shadowElevation = 24.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D2D))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DEBUG CONSOLE", 
                        color = Color(0xFF00E676), // Matrix Green
                        fontWeight = FontWeight.Bold, 
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Row {
                        IconButton(onClick = { 
                            val text = DebugLogger.logs.joinToString("\n")
                            clipboardManager.setText(AnnotatedString(text))
                        }) { 
                            Icon(Icons.Default.ContentCopy, "Copy", tint = Color.Cyan) 
                        }
                        
                        IconButton(onClick = { DebugLogger.clear() }) { 
                            Icon(Icons.Default.Delete, "Clear", tint = Color(0xFFFF5252)) 
                        }
                        
                        IconButton(onClick = onDismiss) { 
                            Icon(Icons.Default.Close, "Close", tint = Color.White) 
                        }
                    }
                }
                
                Divider(color = Color.Gray, thickness = 1.dp)
                
                // Logs List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(DebugLogger.logs) { log ->
                        val color = when {
                            log.contains("API_FAIL") || log.contains("NET_FAIL") || log.contains("ERROR") || log.contains("Exception") -> Color(0xFFFF5252) // Red
                            log.contains("API_REQ") || log.contains("NET_REQ") -> Color(0xFF64B5F6) // Blue
                            log.contains("API_RES") || log.contains("NET_RES") -> Color(0xFF81C784) // Green
                            log.contains("JS") || log.contains("WEB") -> Color(0xFFFFD54F) // Yellow
                            else -> Color(0xFFE0E0E0) // White/Grey
                        }
                        
                        Text(
                            text = log,
                            color = color,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
