package com.kentaro.guts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.kentaro.guts.data.AllTablesResult
import com.kentaro.guts.data.TableData

// Data class for course information
data class CourseData(
    val rowIndex: Int,
    val courseCode: String,
    val courseTitle: String,
    val credits: String,
    val facultyName: String,
    val gcrCode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTableScreen(
    courseData: String?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (courseData != null) {
            println("DEBUG: CourseTableScreen received courseData length: ${courseData.length}")
            val gson = Gson()
            val allTablesResult = try {
                gson.fromJson(courseData, AllTablesResult::class.java)
            } catch (e: Exception) {
                println("DEBUG: Error parsing course data: ${e.message}")
                null
            }
            
            println("DEBUG: allTablesResult: $allTablesResult")
            println("DEBUG: tables size: ${allTablesResult?.tables?.size}")
            
            if (allTablesResult?.tables != null) {
                println("DEBUG: Processing ${allTablesResult.tables.size} tables")
                
                // Display all tables for debugging
                allTablesResult.tables.forEachIndexed { tableIndex, tableData ->
                    println("DEBUG: Table $tableIndex has ${tableData.rows.size} rows")
                    tableData.rows.forEachIndexed { rowIndex, row ->
                        println("DEBUG: Table $tableIndex Row $rowIndex: $row")
                    }
                }
                
                // Look for course details in all tables
                val coursesToDisplay = mutableListOf<CourseData>()
                
                allTablesResult.tables.forEachIndexed { tableIndex, tableData ->
                    println("DEBUG: Processing table $tableIndex with ${tableData.rows.size} rows")
                    
                    // Look for rows that might contain course information
                    tableData.rows.forEachIndexed { rowIndex, row ->
                        println("DEBUG: Table $tableIndex Row $rowIndex: $row")
                        
                        // Check if this row contains course information
                        if (row.size >= 5) {
                            // Look for patterns that indicate course data
                            val firstCell = row.getOrNull(0) ?: ""
                            val secondCell = row.getOrNull(1) ?: ""
                            
                            // Check if this looks like course data (has course code pattern like 21CSC302J)
                            if (firstCell.matches(Regex("\\d{2}[A-Z]{2,3}\\d{3}[A-Z]?")) || 
                                secondCell.matches(Regex("\\d{2}[A-Z]{2,3}\\d{3}[A-Z]?")) ||
                                firstCell.matches(Regex("\\d+")) || // Check if first cell is a number (S.No)
                                firstCell.contains("Course") || 
                                secondCell.contains("Course")) {
                                
                                println("DEBUG: Found potential course data in table $tableIndex row $rowIndex")
                                
                                // Skip header row (row 0) and process actual course data
                                if (rowIndex > 0) {
                                    val courseCode = row.getOrNull(1) ?: "" // Course Code is in column 1
                                    val courseTitle = row.getOrNull(2) ?: "" // Course Title is in column 2
                                    val credits = row.getOrNull(3) ?: "" // Credit is in column 3
                                    val facultyName = row.getOrNull(7) ?: "" // Faculty Name is in column 7
                                    val gcrCode = row.getOrNull(9) ?: "" // GCR Code is in column 9
                                    val slot = row.getOrNull(8) ?: "" // Slot is in column 8
                                    val roomNo = row.getOrNull(10) ?: "" // Room No. is in column 10
                                    
                                    if (courseCode.isNotEmpty() && courseTitle.isNotEmpty()) {
                                        coursesToDisplay.add(
                                            CourseData(
                                                rowIndex = rowIndex,
                                                courseCode = courseCode,
                                                courseTitle = courseTitle,
                                                credits = credits,
                                                facultyName = facultyName,
                                                gcrCode = gcrCode
                                            )
                                        )
                                        println("DEBUG: Added course: $courseCode - $courseTitle")
                                    }
                                }
                            }
                        }
                    }
                }
                
                println("DEBUG: Created ${coursesToDisplay.size} course cards")
                
                if (coursesToDisplay.isNotEmpty()) {
                    // Display each course as a separate item
                    coursesToDisplay.forEach { courseData ->
                        item {
                            // Course card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // First line: Course title (course code) - GCR Code
                                    Text(
                                        text = "${courseData.courseTitle} (${courseData.courseCode.lastOrNull() ?: ""}) - ${courseData.gcrCode}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Second line: Faculty Name and Credits (smaller, less opacity)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = courseData.facultyName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        
                                        Text(
                                            text = "${courseData.credits} Credits",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Spacer between cards
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                } else {
                    // No course data found
                    item {
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
                                    text = "No course details found",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "The course data contains timetable information but no detailed course listings were found.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            } else {
                // No data available
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No course data available",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // No data available
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No course data available",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

 