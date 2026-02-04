package myedu.oshsu.kg.widget

import myedu.oshsu.kg.ScheduleItem
import java.util.Calendar

object WidgetHelper {
    /**
     * Finds the next class based on current time.
     * 
     * After 8 PM, shows next day's first class.
     * If next day is Sunday (day 6), shows Monday's (day 0) first class instead.
     * 
     * @param schedule List of schedule items
     * @param timeMap Map of lesson IDs to time strings (e.g., "08:00 - 09:30")
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return Pair of ScheduleItem and its time string, or null if no classes found
     */
    fun findNextClass(
        schedule: List<ScheduleItem>,
        timeMap: Map<Int, String>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Pair<ScheduleItem, String>? {
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute // in minutes
        
        // Get current day (API format: 0 = Monday, 1 = Tuesday, ..., 5 = Saturday, 6 = Sunday)
        val javaDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        var apiDay = when (javaDayOfWeek) {
            Calendar.SUNDAY -> 6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 0
        }
        
        // After 8 PM (20:00), show next day
        if (currentHour >= 20) {
            apiDay = (apiDay + 1) % 7
            // If next day is Sunday (6), show Monday (0)
            if (apiDay == 6) {
                apiDay = 0
            }
            
            // Find the first class of the target day
            val nextDayClasses = schedule.filter { it.day == apiDay }.sortedBy { it.id_lesson }
            return nextDayClasses.firstOrNull()?.let { classItem ->
                val timeString = timeMap[classItem.id_lesson] ?: "N/A"
                Pair(classItem, timeString)
            }
        }
        
        // Before 8 PM, find next class today
        val todayClasses = schedule.filter { it.day == apiDay }.sortedBy { it.id_lesson }
        
        for (classItem in todayClasses) {
            val timeString = timeMap[classItem.id_lesson] ?: continue
            val startTime = timeString.split("-").firstOrNull()?.trim() ?: continue
            val parts = startTime.split(":")
            if (parts.size < 2) continue
            
            val classHour = parts[0].toIntOrNull() ?: continue
            val classMinute = parts[1].toIntOrNull() ?: continue
            val classTime = classHour * 60 + classMinute
            
            // If class is upcoming or started within the last 10 minutes
            if (classTime > currentTime - 10) {
                return Pair(classItem, timeString)
            }
        }
        
        // No more classes today, show next day's first class
        val nextApiDay = (apiDay + 1) % 7
        val targetDay = if (nextApiDay == 6) 0 else nextApiDay // Skip Sunday
        
        val nextDayClasses = schedule.filter { it.day == targetDay }.sortedBy { it.id_lesson }
        return nextDayClasses.firstOrNull()?.let { classItem ->
            val timeString = timeMap[classItem.id_lesson] ?: "N/A"
            Pair(classItem, timeString)
        }
    }
    
    /**
     * Gets all classes for a specific day.
     * 
     * After 8 PM, returns next day's classes.
     * If next day is Sunday (day 6), returns Monday's (day 0) classes instead.
     * 
     * @param schedule List of schedule items
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return List of schedule items for the target day, sorted by lesson ID
     */
    fun getTodayClasses(
        schedule: List<ScheduleItem>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<ScheduleItem> {
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val javaDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        
        var apiDay = when (javaDayOfWeek) {
            Calendar.SUNDAY -> 6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 0
        }
        
        // After 8 PM, show next day
        if (currentHour >= 20) {
            apiDay = (apiDay + 1) % 7
            // If next day is Sunday (6), show Monday (0)
            if (apiDay == 6) {
                apiDay = 0
            }
        }
        
        return schedule.filter { it.day == apiDay }.sortedBy { it.id_lesson }
    }
    
    /**
     * Determines if we're showing tomorrow's classes (after 8 PM).
     * 
     * @param currentTimeMillis Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return true if after 8 PM, false otherwise
     */
    fun isShowingTomorrow(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val now = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        return currentHour >= 20
    }
}
