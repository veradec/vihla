package com.kentaro.guts.service

import android.util.Log
import com.kentaro.guts.data.TimetableResponse
import com.kentaro.guts.ui.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

data class ClassInfo(
    val courseCode: String,
    val courseTitle: String,
    val courseFaculty: String,
    val timeSlot: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dayOrder: Int
)

data class NextClassInfo(
    val classInfo: ClassInfo?,
    val minutesUntilClass: Long,
    val isToday: Boolean
)

class ClassScheduler {
    
    companion object {
        private const val TAG = "ClassScheduler"
        
        // Common class time slots (you can modify these based on your institution)
        private val TIME_SLOTS = mapOf(
            "A" to Pair("08:00", "09:00"),
            "B" to Pair("09:00", "10:00"),
            "C" to Pair("10:00", "11:00"),
            "D" to Pair("11:00", "12:00"),
            "E" to Pair("12:00", "13:00"),
            "F" to Pair("13:00", "14:00"),
            "G" to Pair("14:00", "15:00"),
            "H" to Pair("15:00", "16:00"),
            "I" to Pair("16:00", "17:00"),
            "J" to Pair("17:00", "18:00"),
            "K" to Pair("18:00", "19:00"),
            "L" to Pair("19:00", "20:00")
        )
    }
    
    fun getNextClass(
        timetableResponse: TimetableResponse?,
        courseData: String?
    ): NextClassInfo? {
        if (timetableResponse?.rawHtml == null) {
            Log.d(TAG, "No timetable data available")
            return null
        }
        
        try {
            // Parse timetable data
            val tables = parseHtmlTables(timetableResponse.rawHtml)
            val dayOrders = parseTimetableData(tables)
            val slotToCourseMapping = parseCourseDataAndCreateMapping(courseData)
            
            if (dayOrders.isEmpty()) {
                Log.d(TAG, "No day orders found in timetable")
                return null
            }
            
            // Get current day order
            val currentDayOrder = getCurrentDayOrder()
            Log.d(TAG, "Current day order: $currentDayOrder")
            
            // Find the day order data for today
            val todaySchedule = dayOrders.find { it.dayOrder == currentDayOrder }
            if (todaySchedule == null) {
                Log.d(TAG, "No schedule found for day order $currentDayOrder")
                return null
            }
            
            // Get current time
            val currentTime = LocalTime.now()
            Log.d(TAG, "Current time: $currentTime")
            
            // Find the next class
            var nextClass: ClassInfo? = null
            var minutesUntilClass = Long.MAX_VALUE
            
            for (timeSlot in todaySchedule.timeSlots) {
                if (!timeSlot.isAvailable || timeSlot.slot.isEmpty()) continue
                
                val courseInfo = slotToCourseMapping[timeSlot.slot]
                if (courseInfo == null) continue
                
                val timeRange = TIME_SLOTS[timeSlot.slot]
                if (timeRange == null) {
                    // Try to parse time from the time slot data
                    val parsedTime = parseTimeFromSlot(timeSlot.time)
                    if (parsedTime != null) {
                        val startTime = parsedTime
                        val endTime = startTime.plusHours(1)
                        
                        if (startTime.isAfter(currentTime)) {
                            val minutesUntil = java.time.Duration.between(currentTime, startTime).toMinutes()
                            if (minutesUntil < minutesUntilClass) {
                                minutesUntilClass = minutesUntil
                                nextClass = ClassInfo(
                                    courseCode = courseInfo.courseCode,
                                    courseTitle = courseInfo.courseTitle,
                                    courseFaculty = courseInfo.courseFaculty,
                                    timeSlot = timeSlot.slot,
                                    startTime = startTime,
                                    endTime = endTime,
                                    dayOrder = currentDayOrder
                                )
                            }
                        }
                    }
                } else {
                    val startTime = LocalTime.parse(timeRange.first)
                    val endTime = LocalTime.parse(timeRange.second)
                    
                    if (startTime.isAfter(currentTime)) {
                        val minutesUntil = java.time.Duration.between(currentTime, startTime).toMinutes()
                        if (minutesUntil < minutesUntilClass) {
                            minutesUntilClass = minutesUntil
                            nextClass = ClassInfo(
                                courseCode = courseInfo.courseCode,
                                courseTitle = courseInfo.courseTitle,
                                courseFaculty = courseInfo.courseFaculty,
                                timeSlot = timeSlot.slot,
                                startTime = startTime,
                                endTime = endTime,
                                dayOrder = currentDayOrder
                            )
                        }
                    }
                }
            }
            
            if (nextClass != null) {
                Log.d(TAG, "Next class: ${nextClass.courseTitle} at ${nextClass.startTime}")
                return NextClassInfo(
                    classInfo = nextClass,
                    minutesUntilClass = minutesUntilClass,
                    isToday = true
                )
            }
            
            // If no classes today, check tomorrow
            val tomorrowDayOrder = getNextDayOrder(currentDayOrder)
            val tomorrowSchedule = dayOrders.find { it.dayOrder == tomorrowDayOrder }
            
            if (tomorrowSchedule != null) {
                for (timeSlot in tomorrowSchedule.timeSlots) {
                    if (!timeSlot.isAvailable || timeSlot.slot.isEmpty()) continue
                    
                    val courseInfo = slotToCourseMapping[timeSlot.slot]
                    if (courseInfo == null) continue
                    
                    val timeRange = TIME_SLOTS[timeSlot.slot]
                    if (timeRange != null) {
                        val startTime = LocalTime.parse(timeRange.first)
                        val endTime = LocalTime.parse(timeRange.second)
                        
                        // Calculate minutes until tomorrow's first class
                        val tomorrow = LocalDate.now().plusDays(1)
                        val tomorrowFirstClass = tomorrow.atTime(startTime)
                        val currentDateTime = LocalDate.now().atTime(LocalTime.now())
                        val minutesUntil = java.time.Duration.between(currentDateTime, tomorrowFirstClass).toMinutes()
                        
                        return NextClassInfo(
                            classInfo = ClassInfo(
                                courseCode = courseInfo.courseCode,
                                courseTitle = courseInfo.courseTitle,
                                courseFaculty = courseInfo.courseFaculty,
                                timeSlot = timeSlot.slot,
                                startTime = startTime,
                                endTime = endTime,
                                dayOrder = tomorrowDayOrder
                            ),
                            minutesUntilClass = minutesUntil,
                            isToday = false
                        )
                    }
                }
            }
            
            Log.d(TAG, "No upcoming classes found")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting next class: ${e.message}")
            return null
        }
    }
    
    private fun parseTimeFromSlot(timeString: String): LocalTime? {
        return try {
            // Try different time formats
            val formats = listOf(
                "HH:mm",
                "h:mm a",
                "HH:mm:ss"
            )
            
            for (format in formats) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
                    return LocalTime.parse(timeString, formatter)
                } catch (e: Exception) {
                    continue
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getCurrentDayOrder(): Int {
        // This is a simplified implementation
        // You might need to adjust this based on your institution's day order system
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Map day of week to day order (adjust as needed)
        return when (dayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
    
    private fun getNextDayOrder(currentDayOrder: Int): Int {
        return if (currentDayOrder < 7) currentDayOrder + 1 else 1
    }
    
    // Reuse the parsing functions from TimetableScreen
    private fun parseHtmlTables(html: String): List<TableData> {
        return try {
            val doc = org.jsoup.Jsoup.parse(html)
            val tables = doc.select("table")
            
            tables.mapIndexed { index, table ->
                val rows = table.select("tr").map { row ->
                    val cells = row.select("td, th").map { cell ->
                        cell.text().trim()
                    }
                    TableRow(cells)
                }
                TableData(index + 1, rows)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseTimetableData(tables: List<TableData>): List<DayOrderData> {
        val dayOrders = mutableListOf<DayOrderData>()
        
        tables.forEach { table ->
            if (table.rows.size >= 7) {
                val timeRow = table.rows[0]
                val dayOrderRows = table.rows.drop(3)
                
                dayOrderRows.forEach { dayRow ->
                    if (dayRow.cells.size > 1) {
                        val dayOrderText = dayRow.cells[0]
                        val dayOrderMatch = Regex("Day\\s*(\\d+)").find(dayOrderText)
                        
                        if (dayOrderMatch != null) {
                            val dayOrderNumber = dayOrderMatch.groupValues[1].toIntOrNull()
                            if (dayOrderNumber != null) {
                                val timeSlots = mutableListOf<TimeSlotData>()
                                
                                for (i in 1 until minOf(dayRow.cells.size, timeRow.cells.size)) {
                                    val time = if (i < timeRow.cells.size) timeRow.cells[i] else ""
                                    val slot = dayRow.cells[i]
                                    val isAvailable = slot.isNotEmpty() && slot != "A" && slot != "X"
                                    
                                    timeSlots.add(TimeSlotData(time, slot, isAvailable))
                                }
                                
                                dayOrders.add(DayOrderData(dayOrderNumber, timeSlots))
                            }
                        }
                    }
                }
            }
        }
        
        return dayOrders.sortedBy { it.dayOrder }
    }
    
    private fun parseCourseDataAndCreateMapping(courseData: String?): Map<String, CourseInfo> {
        if (courseData == null) return emptyMap()
        
        return try {
            val gson = com.google.gson.Gson()
            val allTablesResult = gson.fromJson(courseData, com.kentaro.guts.data.AllTablesResult::class.java)
            
            val slotToCourseMapping = mutableMapOf<String, CourseInfo>()
            
            allTablesResult?.tables?.forEach { table ->
                table.rows.forEach { row ->
                    if (row.size >= 11) {
                        val courseCode = row.getOrNull(1) ?: ""
                        val courseTitle = row.getOrNull(2) ?: ""
                        val courseCategory = row.getOrNull(5) ?: ""
                        val courseFaculty = row.getOrNull(7) ?: ""
                        val courseSlot = row.getOrNull(8) ?: ""
                        
                        if (courseSlot.isNotEmpty() && courseCode.isNotEmpty()) {
                            val slots = courseSlot.split("-").map { it.trim() }
                            slots.forEach { slot ->
                                if (slot.isNotEmpty()) {
                                    slotToCourseMapping[slot] = CourseInfo(
                                        courseCode = courseCode,
                                        courseTitle = courseTitle,
                                        courseFaculty = courseFaculty,
                                        courseCategory = courseCategory
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            slotToCourseMapping
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing course data: ${e.message}")
            emptyMap()
        }
    }
} 