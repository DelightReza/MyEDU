package myedu.oshsu.kg

import java.util.Calendar

object AcademicYearHelper {
    /**
     * Calculates the default active year ID based on the current date.
     * Academic year starts in September.
     * 
     * Examples:
     * - January 2024 → returns 23 (still in 2023-2024 academic year)
     * - September 2024 → returns 24 (new 2024-2025 academic year starts)
     * - August 2024 → returns 23 (still in 2023-2024 academic year)
     * 
     * Note: This assumes years 2000-2099. For years beyond 2099, 
     * the system would need updates to handle 3-digit year IDs.
     * 
     * @return Last 2 digits of the academic year
     */
    fun getDefaultActiveYearId(): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-based (0 = January, 8 = September)
        
        // If we're in September (month 8) or later, use current year
        // Otherwise, use previous year
        val academicYear = if (currentMonth >= Calendar.SEPTEMBER) {
            currentYear
        } else {
            currentYear - 1
        }
        
        // Return last 2 digits (works for years 2000-2099)
        return academicYear % 100
    }
}
