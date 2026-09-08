package com.dynamicbookreader.data.model

data class DailyReadingStats(
    val date: String, // YYYY-MM-DD
    val minutesRead: Int
)

data class ReadingAnalyticsData(
    val todaySecondsRead: Long = 0,
    val dailyGoalMinutes: Int = 20,
    val currentStreakDays: Int = 1,
    val longestStreakDays: Int = 1,
    val lastReadDate: String = "",
    val totalSecondsRead: Long = 0,
    val totalChaptersCompleted: Int = 0,
    val weeklyMinutesMap: Map<String, Int> = emptyMap() // e.g. "শনি" to 15, "রবি" to 30, etc.
) {
    val todayMinutesRead: Int
        get() = (todaySecondsRead / 60).toInt()

    val goalProgressFraction: Float
        get() = if (dailyGoalMinutes > 0) {
            (todayMinutesRead.toFloat() / dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
        } else 1f

    val isGoalCompleted: Boolean
        get() = todayMinutesRead >= dailyGoalMinutes

    val totalHoursReadFormatted: String
        get() {
            val totalMinutes = totalSecondsRead / 60
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours} ঘণ্টা ${mins} মি." else "${mins} মিনিট"
        }
}
