package com.kentaro.guts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.kentaro.guts.data.AllTablesResult
import com.kentaro.guts.data.AttendanceDetail
import com.kentaro.guts.data.ParsedAttendanceResult
import com.kentaro.guts.data.TableData

// Utility function to format attendance values
private fun formatAttendanceValue(attendance: String): String {
    return try {
        // Remove any existing % sign and trim whitespace
        val cleanValue = attendance.replace("%", "").trim()
        
        // Try to parse as double
        val doubleValue = cleanValue.toDouble()
        
        // Check if it's a whole number
        if (doubleValue == doubleValue.toInt().toDouble()) {
            // It's a whole number, return as integer
            doubleValue.toInt().toString()
        } else {
            // It has decimals, round to one decimal place
            String.format("%.1f", doubleValue)
        }
    } catch (e: Exception) {
        // If parsing fails, return the original value
        attendance
    }
}

// Target helpers for dynamic attendance goal
@Composable
private fun getAttendanceTargetProbability(): Double {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("auth_cache", android.content.Context.MODE_PRIVATE)
    val targetPercent = prefs.getInt("attendance_target_percent", 75)
    return (targetPercent.coerceIn(1, 100)) / 100.0
}

private fun calculateClassesNeededForTarget(conducted: Int, absent: Int, target: Double): Int {
    if (conducted == 0) return 0
    val present = conducted - absent
    val current = present.toDouble() / conducted
    if (current >= target) return 0
    val numerator = target * conducted - present
    val denominator = 1 - target
    val additional = Math.ceil(numerator / denominator).toInt()
    return maxOf(1, additional)
}

private fun calculateMarginForTarget(conducted: Int, absent: Int, target: Double): Int {
    if (conducted == 0) return 0
    val present = conducted - absent
    val current = present.toDouble() / conducted
    if (current < target) return 0
    val margin = Math.floor((present - target * conducted) / target).toInt()
    return maxOf(0, margin)
}

// Utility function to calculate classes needed for 75% attendance
private fun calculateClassesNeededFor75Percent(conductedClasses: String, hoursAbsent: String): Int {
    return try {
        val conducted = conductedClasses.toInt()
        val absent = hoursAbsent.toInt()
        
        if (conducted == 0) return 0
        
        val present = conducted - absent
        val currentAttendance = present.toDouble() / conducted
        
        if (currentAttendance >= 0.75) {
            return 0 // Already at or above 75%
        }
        
        // (present + x) / (conducted + x) ≥ 0.75 → x ≥ (0.75*conducted - present) / 0.25
        val additionalClasses = Math.ceil(
            (0.75 * conducted - present) / 0.25
        ).toInt()
        
        // Ensure we return at least 1 if calculation results in 0 or negative
        maxOf(1, additionalClasses)
    } catch (e: Exception) {
        0 // Return 0 if parsing fails
    }
}

// Utility function to calculate margin (classes you can miss) when above 75%
private fun calculateMarginFor75Percent(conductedClasses: String, hoursAbsent: String): Int {
    return try {
        val conducted = conductedClasses.toInt()
        val absent = hoursAbsent.toInt()
        
        if (conducted == 0) return 0
        
        val present = conducted - absent
        val currentAttendance = present.toDouble() / conducted
        
        if (currentAttendance < 0.75) {
            return 0 // Below 75%, no margin
        }
        
        // Max x such that present / (conducted + x) ≥ 0.75 → x ≤ (present - 0.75*conducted) / 0.75
        val margin = Math.floor(
            (present - 0.75 * conducted) / 0.75
        ).toInt()
        
        maxOf(0, margin)
    } catch (e: Exception) {
        0 // Return 0 if parsing fails
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTableScreen(
    parsedAttendanceData: ParsedAttendanceResult?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (parsedAttendanceData?.error != null) {
            // Error state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: ${parsedAttendanceData.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        } else if (parsedAttendanceData?.tablesData != null) {
            // Success state - show tables 3 and 4
            val gson = Gson()
            val allTablesResult = try {
                gson.fromJson(parsedAttendanceData.tablesData, AllTablesResult::class.java)
            } catch (e: Exception) {
                println("DEBUG: Error parsing tables data: ${e.message}")
                null
            }
            
            println("DEBUG: allTablesResult: $allTablesResult")
            println("DEBUG: tables size: ${allTablesResult?.tables?.size}")
            
            if (allTablesResult?.tables != null) {
                // Display only table 3 (index 2) - attendance data
                val targetTableIndex = 2 // 0-based indexing, so table 3
                
                println("DEBUG: targetTableIndex: $targetTableIndex, tables size: ${allTablesResult.tables.size}")
                
                if (targetTableIndex < allTablesResult.tables.size) {
                    val tableData = allTablesResult.tables[targetTableIndex]
                    println("DEBUG: Found table data with ${tableData.rows.size} rows")
                    AttendanceTableCard(tableData = tableData)
                } else {
                    println("DEBUG: targetTableIndex $targetTableIndex is out of bounds for tables size ${allTablesResult.tables.size}")
                }
            } else {
                // No data available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No table data available",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (parsedAttendanceData?.attendance != null) {
            // Fallback to old attendance data if available
            val attendanceList = parsedAttendanceData.attendance
            println("DEBUG: AttendanceTableScreen received ${attendanceList.size} courses")
            
            Text(
                text = "Found ${attendanceList.size} courses",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Attendance table
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Table header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Course",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Faculty",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Slot",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Attendance",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Attendance rows
                    attendanceList.forEach { attendance ->
                        AttendanceRow(attendance = attendance)
                        Spacer(modifier = Modifier.height(50.dp))
                    }
                }
            }
        } else {
            // No data available
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No attendance data available",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttendanceTableCard(tableData: TableData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display table data
            if (tableData.rows.isNotEmpty()) {
                // Custom formatting for Attendance table (table 3)
                // Skip header row and format each data row as a card
                tableData.rows.drop(1).forEachIndexed { rowIndex, rowData ->
                    if (rowData.size >= 9) {
                        AttendanceRowCard(
                            courseCode = rowData[0],
                            courseTitle = rowData[1],
                            conductedClasses = rowData[6],
                            hoursAbsent = rowData[7],
                            attendance = rowData[8]
                        )
                        
                        if (rowIndex < tableData.rows.size - 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRowCard(
    courseCode: String,
    courseTitle: String,
    conductedClasses: String,
    hoursAbsent: String,
    attendance: String
) {
    val target = getAttendanceTargetProbability()
    val classesNeededFor75 = try {
        calculateClassesNeededForTarget(conductedClasses.toInt(), hoursAbsent.toInt(), target)
    } catch (e: Exception) { 0 }
    val marginFor75 = try {
        calculateMarginForTarget(conductedClasses.toInt(), hoursAbsent.toInt(), target)
    } catch (e: Exception) { 0 }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Main row with course info and attendance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Course info
                Column(
                    modifier = Modifier.weight(2f)
                ) {
                    // Extract the last letter of the first word from course code
                    val courseCodeLetter = courseCode.split(" ").firstOrNull()?.lastOrNull()
                    val formattedCourseTitle = if (courseCodeLetter != null) {
                        "$courseTitle ($courseCodeLetter)"
                    } else {
                        courseTitle
                    }
                    
                    Text(
                        text = formattedCourseTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Hours information in a row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total hours conducted (blue)
                    Text(
                        text = conductedClasses,
                        fontSize = 10.sp,
                        color = Color(0xFF2196F3), // Blue
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Hours absent (red, no brackets)
                    Text(
                        text = hoursAbsent,
                        color = Color(0xFFF44336), // Red
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Attendance percentage
                Text(
                    text = formatAttendanceValue(attendance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = try {
                        val percentage = attendance.replace("%", "").toDoubleOrNull() ?: 0.0
                        when {
                            percentage >= 90 -> Color(0xFFFFD700) // Gold (Excellent)
                            percentage >= 80 -> Color(0xFF8BC34A) // Light Green (Good)
                            percentage >= 75 -> Color(0xFF2196F3) // Blue (Minimum required)
                            percentage >= 60 -> Color(0xFFFF9800) // Orange (Warning)
                            else -> Color(0xFFF44336) // Red (Critical)
                        }
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(0.5f)
                )
            }
            
            // Classes needed for 75% row
            if (classesNeededFor75 > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "need $classesNeededFor75",
                        fontSize = 9.sp,
                        color = Color(0xFFFF9800), // Orange
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "margin $marginFor75",
                        fontSize = 9.sp,
                        color = Color(0xFF8BC34A), // Green
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(attendance: AttendanceDetail) {
    val target = getAttendanceTargetProbability()
    val classesNeededFor75 = calculateClassesNeededForTarget(attendance.courseConducted, attendance.courseAbsent, target)
    val marginFor75 = calculateMarginForTarget(attendance.courseConducted, attendance.courseAbsent, target)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Course info
            Column(
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = attendance.courseCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = attendance.courseTitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = attendance.courseCategory,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Faculty
            Text(
                text = attendance.courseFaculty,
                modifier = Modifier.weight(1.5f),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Slot
            Text(
                text = attendance.courseSlot,
                modifier = Modifier.weight(0.8f),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Attendance percentage
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = formatAttendanceValue(attendance.courseAttendance),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = try {
                        val percentage = attendance.courseAttendance.replace("%", "").toDoubleOrNull() ?: 0.0
                        when {
                            percentage >= 90 -> Color(0xFFFFD700) // Gold (Excellent)
                            percentage >= 80 -> Color(0xFF8BC34A) // Light Green (Good)
                            percentage >= 75 -> Color(0xFF2196F3) // Blue (Minimum required)
                            percentage >= 60 -> Color(0xFFFF9800) // Orange (Warning)
                            else -> Color(0xFFF44336) // Red (Critical)
                        }
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ).padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "${attendance.courseConducted}/${attendance.courseConducted + attendance.courseAbsent}",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Classes needed for 75% row
        if (classesNeededFor75 > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "need $classesNeededFor75",
                    fontSize = 9.sp,
                    color = Color(0xFFFF9800), // Orange
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "margin $marginFor75",
                    fontSize = 9.sp,
                    color = Color(0xFF8BC34A), // Green
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
} 