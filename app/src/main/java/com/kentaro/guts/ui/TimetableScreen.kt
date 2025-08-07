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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@Composable
fun TimetableScreen(
    parsedTimetable: TimetableResponse?,
    courseData: String? = null,
    modifier: Modifier = Modifier
) {
    var currentDayOrderIndex by remember { mutableStateOf(0) }
    
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
        
        // Parse course data and create slot mapping
        val slotToCourseMapping = parseCourseDataAndCreateMapping(courseData)
        
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
    
    Log.d("TIMETABLE_DEBUG", "=== SAMPLE TABLE DATA ===")
    tables.take(3).forEachIndexed { tableIndex, table ->
        val sampleCells = table.rows.take(2).flatMap { it.cells }.take(5)
        Log.d("TIMETABLE_DEBUG", "Table ${table.index}: ${sampleCells.joinToString(" | ")}")
    }
    Log.d("TIMETABLE_DEBUG", "")
    
    Log.d("TIMETABLE_DEBUG", "=== ALL TABLE CELLS (for copy-paste) ===")
    tables.forEachIndexed { tableIndex, table ->
        Log.d("TIMETABLE_DEBUG", "--- Table ${table.index} ---")
        table.rows.forEachIndexed { rowIndex, row ->
            Log.d("TIMETABLE_DEBUG", "Row $rowIndex: ${row.cells.joinToString(" | ")}")
        }
        Log.d("TIMETABLE_DEBUG", "")
    }
    Log.d("TIMETABLE_DEBUG", "=== END DEBUG INFO ===")
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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dayOrder.timeSlots) { timeSlot ->
            TimeSlotCard(
                timeSlot = timeSlot,
                courseInfo = slotToCourseMapping[timeSlot.slot]
            )
        }
    }
}

@Composable
fun TimeSlotCard(
    timeSlot: TimeSlotData,
    courseInfo: CourseInfo? = null
) {
    val hasCourse = courseInfo != null
    val isEmptySlot = timeSlot.isAvailable && !hasCourse
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
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