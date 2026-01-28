package myedu.oshsu.kg.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Shape System
 * 
 * MD3 defines shape tokens for different UI components:
 * - Extra Small: Small components like chips, buttons
 * - Small: Cards, dialogs
 * - Medium: Larger cards, sheets
 * - Large: Navigation drawers, bottom sheets
 * - Extra Large: Large modals, special surfaces
 */
val AppShapes = Shapes(
    // Extra small shape (4dp) - Used for small components
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small shape (8dp) - Used for chips, small buttons
    small = RoundedCornerShape(8.dp),
    
    // Medium shape (12dp) - Used for cards, elevated buttons
    medium = RoundedCornerShape(12.dp),
    
    // Large shape (16dp) - Used for larger cards, FABs
    large = RoundedCornerShape(16.dp),
    
    // Extra large shape (28dp) - Used for bottom sheets, dialogs
    extraLarge = RoundedCornerShape(28.dp)
)
