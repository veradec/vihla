package com.kentaro.guts.ui

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kentaro.guts.data.TimetableResponse
import com.kentaro.guts.data.ParsedCalendarResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import com.kentaro.guts.service.NotificationDataManager
import kotlinx.coroutines.delay
import android.content.Context

// Helper function to extract start time from time range
private fun extractStartTime(timeString: String): String {
    return timeString.split("-").firstOrNull()?.trim() ?: timeString
}

// Helper function to parse time range string like "08:00 - 08:50"
private fun parseTimeRangeString(timeString: String): Pair<java.time.LocalTime, java.time.LocalTime>? {
    return try {
        val parts = timeString.split("-").map { it.trim() }
        if (parts.size == 2) {
            val startTime = java.time.LocalTime.parse(parts[0])
            val endTime = java.time.LocalTime.parse(parts[1])
            Pair(startTime, endTime)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w("TimetableScreen", "Failed to parse time range: $timeString", e)
        null
    }
}

// Function to send notification for current slot or "Classes are over"
private fun sendCurrentSlotNotification(
    context: Context,
    currentDayOrder: DayOrderData,
    slotToCourseMapping: Map<String, CourseInfo>
) {
    val currentTime = java.time.LocalTime.now()
    var foundCurrentSlot = false
    
    Log.d("TimetableScreen", "Checking current time: $currentTime")
    
    // Find the current or next slot based on time
    for ((index, slot) in currentDayOrder.timeSlots.withIndex()) {
        if (slot.isAvailable && slot.slot.isNotEmpty()) {
            val courseInfo = slotToCourseMapping[slot.slot]
            if (courseInfo != null) {
                // Parse the slot time to compare with current time
                val timeRange = parseTimeRangeString(slot.time)
                if (timeRange != null) {
                    val (startTime, endTime) = timeRange
                    Log.d("TimetableScreen", "Checking slot $index: ${courseInfo.courseTitle} at $startTime-$endTime")
                    
                    // Check if this is the current or next slot
                    if (currentTime.isBefore(endTime)) {
                        // This is the current or next slot
                        val startTimeStr = extractStartTime(slot.time)
                        Log.d("TimetableScreen", "Found current/next slot: ${courseInfo.courseTitle} at $startTimeStr")
                        
                        NotificationDataManager.sendNextSlotNotification(
                            context = context,
                            slotTitle = courseInfo.courseTitle,
                            slotTime = startTimeStr
                        )
                        foundCurrentSlot = true
                        break
                    }
                }
            }
        }
    }
    
    if (!foundCurrentSlot) {
        Log.d("TimetableScreen", "No more classes today - sending 'Classes are over' notification")
        NotificationDataManager.sendNextSlotNotification(
            context = context,
            slotTitle = "Classes are over",
            slotTime = "for today"
        )
    }
}

@Composable
fun TimetableScreen(
    parsedTimetable: TimetableResponse?,
    courseData: String? = null,
    parsedCalendarData: ParsedCalendarResult? = null,
    modifier: Modifier = Modifier
) {
    var currentDayOrderIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (parsedTimetable == null) {
            Text("No timetable data available.")
            return@Column
        }

        if (parsedTimetable.error != null) {
            Text("Error: ${parsedTimetable.error}")
            return@Column
        }

        // Parse timetable data
        val timetableData = parsedTimetable.rawHtml?.let { html ->
            val tables = parseHtmlTables(html)
            
            // Debug: Extract and log day orders to Logcat
            val dayOrders = extractDayOrders(tables)
            logDayOrdersToLogcat(dayOrders, tables)
            
            parseTimetableData(tables)
        } ?: emptyList()
        
        // Determine today's day order from the parsed calendar and set the initial index
        LaunchedEffect(timetableData, parsedCalendarData) {
            if (timetableData.isNotEmpty()) {
                val todayOrder = getTodayDayOrderFromCalendar(parsedCalendarData)
                if (todayOrder != null) {
                    val idx = timetableData.indexOfFirst { it.dayOrder == todayOrder }
                    if (idx >= 0) {
                        currentDayOrderIndex = idx
                    } else {
                        val fallbackIdx = (todayOrder - 1).coerceIn(0, timetableData.size - 1)
                        currentDayOrderIndex = fallbackIdx
                    }
                }
            }
        }
        
        // Parse course data and create slot mapping
        val slotToCourseMapping = parseCourseDataAndCreateMapping(courseData)
        
        // Debug logging for slot mapping
        Log.d("TimetableScreen", "Slot to course mapping: $slotToCourseMapping")
        Log.d("TimetableScreen", "Available slots: ${slotToCourseMapping.keys.joinToString()}")
        
        // Send notification for current slot when screen is first displayed
        LaunchedEffect(timetableData, currentDayOrderIndex) {
            if (timetableData.isNotEmpty()) {
                val currentDayOrder = timetableData[currentDayOrderIndex]
                sendCurrentSlotNotification(context, currentDayOrder, slotToCourseMapping)
            }
        }
        
        if (timetableData.isNotEmpty()) {
            // Navigation header
            TimetableNavigationHeader(
                currentDayOrderIndex = currentDayOrderIndex,
                totalDayOrders = timetableData.size,
                onPrevious = {
                    if (currentDayOrderIndex > 0) {
                        currentDayOrderIndex--
                    }
                },
                onNext = {
                    if (currentDayOrderIndex < timetableData.size - 1) {
                        currentDayOrderIndex++
                        // Send notification for the next slot
                        val nextDayOrder = timetableData[currentDayOrderIndex]
                        var firstSlotWithCourse: TimeSlotData? = null
                        var courseInfo: CourseInfo? = null
                        
                        Log.d("TimetableScreen", "Searching for first slot with course in day order ${currentDayOrderIndex + 1}")
                        
                        // Iterate through slots until we find one with a course
                        for ((index, slot) in nextDayOrder.timeSlots.withIndex()) {
                            Log.d("TimetableScreen", "Checking slot $index: slot='${slot.slot}', time='${slot.time}', isAvailable=${slot.isAvailable}")
                            
                            if (slot.isAvailable && slot.slot.isNotEmpty()) {
                                val tempCourseInfo = slotToCourseMapping[slot.slot]
                                Log.d("TimetableScreen", "Slot $index has content, course info: $tempCourseInfo")
                                
                                if (tempCourseInfo != null) {
                                    firstSlotWithCourse = slot
                                    courseInfo = tempCourseInfo
                                    Log.d("TimetableScreen", "Found first slot with course at index $index: '${tempCourseInfo.courseTitle}'")
                                    break
                                } else {
                                    Log.d("TimetableScreen", "Slot $index has content but no course mapping")
                                }
                            } else {
                                Log.d("TimetableScreen", "Slot $index is empty or unavailable")
                            }
                        }
                        
                        if (firstSlotWithCourse != null && courseInfo != null) {
                            Log.d("TimetableScreen", "Found slot with course: slot='${firstSlotWithCourse.slot}', course='${courseInfo.courseTitle}', time='${firstSlotWithCourse.time}'")
                            
                            // Extract start time from time range (e.g., "08:00 - 08:50" -> "08:00")
                            val startTime = extractStartTime(firstSlotWithCourse.time)
                            Log.d("TimetableScreen", "Sending notification for next available course: '${courseInfo.courseTitle}' at time: '$startTime'")
                            
                            NotificationDataManager.sendNextSlotNotification(
                                context = context,
                                slotTitle = courseInfo.courseTitle,
                                slotTime = startTime
                            )
                            
                            // Also send current slot notification for the new day order
                            sendCurrentSlotNotification(context, nextDayOrder, slotToCourseMapping)
                        } else {
                            Log.d("TimetableScreen", "No slots with courses found in this day order")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display current day order
            val currentDayOrder = timetableData[currentDayOrderIndex]
            DayOrderSection(
                dayOrder = currentDayOrder,
                slotToCourseMapping = slotToCourseMapping
            )
        } else {
            Text("No HTML tables available.")
        }
    }
}

fun getTodayDayOrderFromCalendar(parsedCalendarData: ParsedCalendarResult?): Int? {
    val months = parsedCalendarData?.months ?: return null
    if (months.isEmpty()) return null
    val today = java.time.LocalDate.now()
    val dayStr = today.dayOfMonth.toString()
    val currentMonthShort = today.month.getDisplayName(
        java.time.format.TextStyle.SHORT,
        java.util.Locale.ENGLISH
    ).take(3)
    val currentYearFull = today.year.toString()
    val currentYearShort = currentYearFull.takeLast(2)

    // Try to find matching month
    val month = months.firstOrNull { m ->
        val name = m.month
        name.contains(currentMonthShort, ignoreCase = true) &&
            (name.contains(currentYearFull) || name.contains("'$currentYearShort"))
    } ?: months.firstOrNull { m ->
        m.month.contains(currentMonthShort, ignoreCase = true)
    } ?: months.firstOrNull() ?: return null

    val dayData = month.days.firstOrNull { it.date == dayStr } ?: return null
    val orderText = dayData.dayOrder.trim()
    if (orderText.isEmpty() || orderText == "-") {
        return 1
    }
    val match = Regex("(\\d+)").find(orderText)
    val parsed = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    // Clamp to 1..5 since there are only 5 day orders
    return parsed.coerceIn(1, 5)
}

@Composable
fun TableCard(table: TableData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Table ${table.index}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Display table rows
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                table.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.cells.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TableData(
    val index: Int,
    val rows: List<TableRow>
)

data class TableRow(
    val cells: List<String>
)

data class DayOrderData(
    val dayOrder: Int,
    val timeSlots: List<TimeSlotData>
)

data class TimeSlotData(
    val time: String,
    val slot: String,
    val isAvailable: Boolean
)

data class CourseInfo(
    val courseCode: String,
    val courseTitle: String,
    val courseFaculty: String,
    val courseCategory: String
)

fun parseHtmlTables(html: String): List<TableData> {
    return try {
        val doc: Document = Jsoup.parse(html)
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

fun extractDayOrders(tables: List<TableData>): List<String> {
    val dayOrders = mutableListOf<String>()
    
    tables.forEach { table ->
        table.rows.forEach { row ->
            row.cells.forEach { cell ->
                // Look for patterns that might indicate day orders
                val cellText = cell.lowercase()
                when {
                    cellText.contains("monday") || cellText.contains("mon") -> dayOrders.add("Monday")
                    cellText.contains("tuesday") || cellText.contains("tue") -> dayOrders.add("Tuesday")
                    cellText.contains("wednesday") || cellText.contains("wed") -> dayOrders.add("Wednesday")
                    cellText.contains("thursday") || cellText.contains("thu") -> dayOrders.add("Thursday")
                    cellText.contains("friday") || cellText.contains("fri") -> dayOrders.add("Friday")
                    cellText.contains("saturday") || cellText.contains("sat") -> dayOrders.add("Saturday")
                    cellText.contains("sunday") || cellText.contains("sun") -> dayOrders.add("Sunday")
                    // Look for numeric day orders (1, 2, 3, etc.)
                    cellText.matches(Regex("\\b[1-7]\\b")) -> {
                        val dayNumber = cellText.toIntOrNull()
                        if (dayNumber != null) {
                            val dayName = when (dayNumber) {
                                1 -> "Day 1"
                                2 -> "Day 2"
                                3 -> "Day 3"
                                4 -> "Day 4"
                                5 -> "Day 5"
                                6 -> "Day 6"
                                7 -> "Day 7"
                                else -> "Day $dayNumber"
                            }
                            dayOrders.add(dayName)
                        }
                    }
                    // Look for "day" followed by a number
                    cellText.matches(Regex("day\\s*[1-7]")) -> {
                        val match = Regex("day\\s*([1-7])").find(cellText)
                        if (match != null) {
                            val dayNumber = match.groupValues[1].toIntOrNull()
                            if (dayNumber != null) {
                                dayOrders.add("Day $dayNumber")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Remove duplicates and sort
    return dayOrders.distinct().sorted()
}

fun logDayOrdersToLogcat(dayOrders: List<String>, tables: List<TableData>) {
    Log.d("TIMETABLE_DEBUG", "=== DAY ORDERS DEBUG INFO ===")
    Log.d("TIMETABLE_DEBUG", "Day Orders Found: ${dayOrders.joinToString(", ")}")
    Log.d("TIMETABLE_DEBUG", "Total Day Orders: ${dayOrders.size}")
    Log.d("TIMETABLE_DEBUG", "")
     
    Log.d("TIMETABLE_DEBUG", "")
}

@Composable
fun TimetableNavigationHeader(
    currentDayOrderIndex: Int,
    totalDayOrders: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = onPrevious,
            enabled = currentDayOrderIndex > 0
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Previous Day Order",
                tint = if (currentDayOrderIndex > 0) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        
        // Day Order indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Day Order ${currentDayOrderIndex + 1}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${currentDayOrderIndex + 1} of $totalDayOrders",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Next button
        IconButton(
            onClick = onNext,
            enabled = currentDayOrderIndex < totalDayOrders - 1
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Next Day Order",
                tint = if (currentDayOrderIndex < totalDayOrders - 1) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        

    }
}

fun parseTimetableData(tables: List<TableData>): List<DayOrderData> {
    val dayOrders = mutableListOf<DayOrderData>()
    
    tables.forEach { table ->
        if (table.rows.size >= 7) { // We need at least 7 rows for the timetable structure
            val timeRow = table.rows[0] // Row 0 contains times
            val dayOrderRows = table.rows.drop(3) // Rows 3-7 contain day orders
            
            dayOrderRows.forEach { dayRow ->
                if (dayRow.cells.size > 1) {
                    val dayOrderText = dayRow.cells[0]
                    val dayOrderMatch = Regex("Day\\s*(\\d+)").find(dayOrderText)
                    
                    if (dayOrderMatch != null) {
                        val dayOrderNumber = dayOrderMatch.groupValues[1].toIntOrNull()
                        if (dayOrderNumber != null) {
                            val timeSlots = mutableListOf<TimeSlotData>()
                            
                            // Start from index 1 to skip the "Day X" cell
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

fun parseCourseDataAndCreateMapping(courseData: String?): Map<String, CourseInfo> {
    if (courseData == null) return emptyMap()
    
    return try {
        val gson = com.google.gson.Gson()
        val allTablesResult = gson.fromJson(courseData, com.kentaro.guts.data.AllTablesResult::class.java)
        
        val slotToCourseMapping = mutableMapOf<String, CourseInfo>()
        
        allTablesResult?.tables?.forEach { table ->
            // Process each row in the table
            table.rows.forEach { row ->
                if (row.size >= 11) { // Ensure we have enough columns
                    val courseCode = row.getOrNull(1) ?: ""
                    val courseTitle = row.getOrNull(2) ?: ""
                    val courseCategory = row.getOrNull(5) ?: ""
                    val courseFaculty = row.getOrNull(7) ?: ""
                    val courseSlot = row.getOrNull(8) ?: "" // Slot is in column 8
                    
                    if (courseSlot.isNotEmpty() && courseCode.isNotEmpty()) {
                        // Handle multiple slots (e.g., "A-B-C")
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
        
        Log.d("TIMETABLE_DEBUG", "Created slot mapping with ${slotToCourseMapping.size} entries")
        slotToCourseMapping.forEach { (slot, course) ->
            Log.d("TIMETABLE_DEBUG", "Slot $slot -> ${course.courseCode}: ${course.courseTitle}")
        }
        
        slotToCourseMapping
    } catch (e: Exception) {
        Log.e("TIMETABLE_DEBUG", "Error parsing course data: ${e.message}")
        emptyMap()
    }
}



@Composable
fun DayOrderSection(
    dayOrder: DayOrderData,
    slotToCourseMapping: Map<String, CourseInfo>
) {
    val context = LocalContext.current
    val removableEmptyTimes = remember { setOf("04:50 - 05:30", "05:30 - 06:10") }
    val filteredSlots = remember(dayOrder.timeSlots, slotToCourseMapping) {
        dayOrder.timeSlots.filter { ts ->
            val isRemovableTime = removableEmptyTimes.contains(ts.time)
            val hasCourse = slotToCourseMapping[ts.slot] != null
            !(isRemovableTime && !hasCourse)
        }
    }

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    // Periodically update currentTime every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(30_000)
        }
    }

    val currentSlotIndex = remember(filteredSlots, currentTime) {
        var idx = -1
        filteredSlots.forEachIndexed { i, ts ->
            val range = parseTimeRangeString(ts.time)
            if (range != null) {
                val (start, end) = range
                if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                    idx = i
                    return@forEachIndexed
                }
            }
        }
        idx
    }

    // Track previous slot index to detect changes
    var previousSlotIndex by remember { mutableStateOf(currentSlotIndex) }
    LaunchedEffect(currentSlotIndex) {
        if (previousSlotIndex != currentSlotIndex && currentSlotIndex >= 0 && currentSlotIndex < filteredSlots.size) {
            val currentSlot = filteredSlots[currentSlotIndex]
            Log.d("TimetableScreen", "Processing slot change: slot='${currentSlot.slot}', time='${currentSlot.time}', isAvailable=${currentSlot.isAvailable}")
            
            // Find the next slot with a course starting from current position
            var nextSlotWithCourse: TimeSlotData? = null
            var courseInfo: CourseInfo? = null
            
            Log.d("TimetableScreen", "Searching for next slot with course starting from current slot index $currentSlotIndex")
            
            // Start searching from current slot index
            for (i in currentSlotIndex until filteredSlots.size) {
                val slot = filteredSlots[i]
                Log.d("TimetableScreen", "Checking slot $i: slot='${slot.slot}', time='${slot.time}', isAvailable=${slot.isAvailable}")
                
                if (slot.isAvailable && slot.slot.isNotEmpty()) {
                    val tempCourseInfo = slotToCourseMapping[slot.slot]
                    Log.d("TimetableScreen", "Slot $i has content, course info: $tempCourseInfo")
                    
                    if (tempCourseInfo != null) {
                        nextSlotWithCourse = slot
                        courseInfo = tempCourseInfo
                        Log.d("TimetableScreen", "Found next slot with course at index $i: '${tempCourseInfo.courseTitle}'")
                        break
                    } else {
                        Log.d("TimetableScreen", "Slot $i has content but no course mapping")
                    }
                } else {
                    Log.d("TimetableScreen", "Slot $i is empty or unavailable")
                }
            }
            
            // If no course found after current slot, search from beginning
            if (nextSlotWithCourse == null) {
                Log.d("TimetableScreen", "No course found after current slot, searching from beginning")
                for ((index, slot) in filteredSlots.withIndex()) {
                    Log.d("TimetableScreen", "Checking slot $index from beginning: slot='${slot.slot}', time='${slot.time}', isAvailable=${slot.isAvailable}")
                    
                    if (slot.isAvailable && slot.slot.isNotEmpty()) {
                        val tempCourseInfo = slotToCourseMapping[slot.slot]
                        Log.d("TimetableScreen", "Slot $index has content, course info: $tempCourseInfo")
                        
                        if (tempCourseInfo != null) {
                            nextSlotWithCourse = slot
                            courseInfo = tempCourseInfo
                            Log.d("TimetableScreen", "Found slot with course at index $index: '${tempCourseInfo.courseTitle}'")
                            break
                        } else {
                            Log.d("TimetableScreen", "Slot $index has content but no course mapping")
                        }
                    } else {
                        Log.d("TimetableScreen", "Slot $index is empty or unavailable")
                    }
                }
            }
            
            if (nextSlotWithCourse != null && courseInfo != null) {
                Log.d("TimetableScreen", "Found next slot with course: slot='${nextSlotWithCourse.slot}', course='${courseInfo.courseTitle}', time='${nextSlotWithCourse.time}'")
                
                // Extract start time from time range (e.g., "08:00 - 08:50" -> "08:00")
                val startTime = extractStartTime(nextSlotWithCourse.time)
                Log.d("TimetableScreen", "Sending notification for next available course: '${courseInfo.courseTitle}' at time: '$startTime'")
                
                NotificationDataManager.sendNextSlotNotification(
                    context = context,
                    slotTitle = courseInfo.courseTitle,
                    slotTime = startTime
                )
                
                // Show persistent notification with course name
                NotificationDataManager.showPersistentCourseNotification(
                    context = context,
                    courseName = courseInfo.courseTitle
                )
            } else {
                Log.d("TimetableScreen", "No slots with courses found in filtered slots")
            }
        }
        previousSlotIndex = currentSlotIndex
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentSlotIndex, filteredSlots) {
        if (currentSlotIndex >= 0 && currentSlotIndex < filteredSlots.size) {
            listState.scrollToItem(currentSlotIndex)
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredSlots) { timeSlot ->
            TimeSlotCard(
                timeSlot = timeSlot,
                courseInfo = slotToCourseMapping[timeSlot.slot],
                isCurrent = filteredSlots.indexOf(timeSlot) == currentSlotIndex
            )
        }
    }
}



@Composable
fun TimeSlotCard(
    timeSlot: TimeSlotData,
    courseInfo: CourseInfo? = null,
    isCurrent: Boolean = false
) {
    val hasCourse = courseInfo != null
    val isEmptySlot = timeSlot.isAvailable && !hasCourse
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> MaterialTheme.colorScheme.secondaryContainer
                hasCourse -> MaterialTheme.colorScheme.surface
                isEmptySlot -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time and slot info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = timeSlot.time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isEmptySlot) "Empty slot" else timeSlot.slot,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        hasCourse -> MaterialTheme.colorScheme.onSurface
                        isEmptySlot -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Course information if available
            courseInfo?.let { course ->
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = course.courseTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = course.courseCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    
                    Text(
                        text = course.courseFaculty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}