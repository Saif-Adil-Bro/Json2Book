package com.dynamicbookreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dynamicbookreader.data.model.ReadingAnalyticsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private val Context.analyticsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reading_analytics"
)

class ReadingAnalyticsRepository(private val context: Context) {

    companion object {
        private val KEY_TODAY_SECONDS = longPreferencesKey("today_seconds")
        private val KEY_LAST_READ_DATE = stringPreferencesKey("last_read_date")
        private val KEY_DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        private val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        private val KEY_LONGEST_STREAK = intPreferencesKey("longest_streak")
        private val KEY_TOTAL_SECONDS = longPreferencesKey("total_seconds")
        private val KEY_TOTAL_CHAPTERS = intPreferencesKey("total_chapters_completed")
        private val KEY_WEEKLY_JSON = stringPreferencesKey("weekly_minutes_json")

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val DAY_NAME_FORMAT = SimpleDateFormat("EEE", Locale("bn", "BD"))
    }

    private fun getTodayDateString(): String = DATE_FORMAT.format(Date())

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return DATE_FORMAT.format(cal.time)
    }

    val analyticsData: Flow<ReadingAnalyticsData> = context.analyticsDataStore.data.map { prefs ->
        val todayStr = getTodayDateString()
        val lastDate = prefs[KEY_LAST_READ_DATE] ?: ""
        
        // If last recorded date is not today, todaySeconds starts at 0
        val isToday = lastDate == todayStr
        val todaySeconds = if (isToday) (prefs[KEY_TODAY_SECONDS] ?: 0L) else 0L

        val weeklyJson = prefs[KEY_WEEKLY_JSON] ?: "{}"
        val weeklyMap = parseWeeklyJson(weeklyJson)

        ReadingAnalyticsData(
            todaySecondsRead = todaySeconds,
            dailyGoalMinutes = prefs[KEY_DAILY_GOAL_MINUTES] ?: 20,
            currentStreakDays = prefs[KEY_CURRENT_STREAK] ?: 1,
            longestStreakDays = prefs[KEY_LONGEST_STREAK] ?: 1,
            lastReadDate = lastDate,
            totalSecondsRead = prefs[KEY_TOTAL_SECONDS] ?: 0L,
            totalChaptersCompleted = prefs[KEY_TOTAL_CHAPTERS] ?: 0,
            weeklyMinutesMap = weeklyMap
        )
    }

    suspend fun recordReadingSession(seconds: Long) {
        if (seconds <= 0) return
        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()
        val dayName = getDayNameBengali(Date())

        context.analyticsDataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_READ_DATE] ?: ""
            var currentStreak = prefs[KEY_CURRENT_STREAK] ?: 0
            var longestStreak = prefs[KEY_LONGEST_STREAK] ?: 0
            var todaySec = prefs[KEY_TODAY_SECONDS] ?: 0L
            val totalSec = (prefs[KEY_TOTAL_SECONDS] ?: 0L) + seconds

            if (lastDate != todayStr) {
                // New day!
                if (lastDate == yesterdayStr) {
                    // Continued streak
                    currentStreak += 1
                } else if (lastDate.isEmpty()) {
                    // First time ever
                    currentStreak = 1
                } else {
                    // Broken streak (more than 1 day missed)
                    currentStreak = 1
                }
                todaySec = seconds
            } else {
                // Same day
                todaySec += seconds
                if (currentStreak == 0) currentStreak = 1
            }

            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }

            // Update weekly JSON
            val weeklyJson = prefs[KEY_WEEKLY_JSON] ?: "{}"
            val weeklyMap = parseWeeklyJson(weeklyJson).toMutableMap()
            val existingMinutes = weeklyMap[dayName] ?: 0
            weeklyMap[dayName] = existingMinutes + (seconds / 60).toInt().coerceAtLeast(1)

            prefs[KEY_TODAY_SECONDS] = todaySec
            prefs[KEY_LAST_READ_DATE] = todayStr
            prefs[KEY_CURRENT_STREAK] = currentStreak
            prefs[KEY_LONGEST_STREAK] = longestStreak
            prefs[KEY_TOTAL_SECONDS] = totalSec
            prefs[KEY_WEEKLY_JSON] = encodeWeeklyJson(weeklyMap)
        }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.analyticsDataStore.edit { prefs ->
            prefs[KEY_DAILY_GOAL_MINUTES] = minutes.coerceIn(5, 180)
        }
    }

    suspend fun incrementCompletedChapters() {
        context.analyticsDataStore.edit { prefs ->
            val count = prefs[KEY_TOTAL_CHAPTERS] ?: 0
            prefs[KEY_TOTAL_CHAPTERS] = count + 1
        }
    }

    private fun getDayNameBengali(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "শনি"
            Calendar.SUNDAY -> "রবি"
            Calendar.MONDAY -> "সোম"
            Calendar.TUESDAY -> "মঙ্গল"
            Calendar.WEDNESDAY -> "বুধ"
            Calendar.THURSDAY -> "বৃহঃ"
            Calendar.FRIDAY -> "শুক্র"
            else -> "আজ"
        }
    }

    private fun parseWeeklyJson(jsonStr: String): Map<String, Int> {
        val map = linkedMapOf<String, Int>(
            "শনি" to 0,
            "রবি" to 0,
            "সোম" to 0,
            "মঙ্গল" to 0,
            "বুধ" to 0,
            "বৃহঃ" to 0,
            "শুক্র" to 0
        )
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
        } catch (_: Exception) {}
        return map
    }

    private fun encodeWeeklyJson(map: Map<String, Int>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }
}
