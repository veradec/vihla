package com.kentaro.guts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
import com.kentaro.guts.data.ParsedAttendanceResult
import com.kentaro.guts.data.TableData

// Function to extract course code to name mappings from course data
private fun extractCourseCodeToNameMapping(courseData: String?): Map<String, String> {
    if (courseData == null) return emptyMap()
    
    return try {
        val gson = Gson()
        val allTablesResult = gson.fromJson(courseData, AllTablesResult::class.java)
        val courseCodeToName = mutableMapOf<String, String>()
        
        allTablesResult?.tables?.forEach { table ->
            // Look for rows that contain course codes and names
            table.rows.forEach { row ->
                if (row.size >= 2) {
                    // Assuming course code is in first column and name in second column
                    val courseCode = row.getOrNull(0)?.trim() ?: ""
                    val courseName = row.getOrNull(1)?.trim() ?: ""
                    
                    if (courseCode.isNotEmpty() && courseName.isNotEmpty() && 
                        courseCode.matches(Regex("[A-Z]{2,3}\\d{4}"))) { // Basic course code pattern
                        courseCodeToName[courseCode] = courseName
                    }
                }
            }
        }
        
        courseCodeToName
    } catch (e: Exception) {
        emptyMap()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksTableScreen(
    parsedAttendanceData: ParsedAttendanceResult?,
    courseData: String? = null,
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
            // Success state - show table 4 (marks/grades)
            val gson = Gson()
            val allTablesResult = try {
                gson.fromJson(parsedAttendanceData.tablesData, AllTablesResult::class.java)
            } catch (e: Exception) {
                null
            }
            
            if (allTablesResult?.tables != null) {
                // Display only table 4 (index 3) - marks/grades data
                val targetTableIndex = 3 // 0-based indexing, so table 4
                
                if (targetTableIndex < allTablesResult.tables.size) {
                    val tableData = allTablesResult.tables[targetTableIndex]
                    
                    // Extract course code to name mapping from courseData
                    val courseCodeToName = extractCourseCodeToNameMapping(courseData)
                    
                    MarksTableCard(
                        tableData = tableData,
                        tableIndex = targetTableIndex,
                        courseCodeToName = courseCodeToName
                    )
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
        } else {
            // No data available
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No marks data available",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MarksTableCard(
    tableData: TableData,
    tableIndex: Int,
    courseCodeToName: Map<String, String> = emptyMap()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            
            // Display table rows (excluding row 13)
            tableData.rows.forEachIndexed { rowIndex, rowData ->
                // Skip row 13 (index 12)
                if (rowIndex == 12) return@forEachIndexed
                
                if (rowIndex == 0) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowData.forEachIndexed { colIndex, cellData ->
                            Text(
                                text = cellData,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    // Data rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowData.forEachIndexed { colIndex, cellData ->
                            // Replace course code with course name if available
                            val displayText = if (colIndex == 0 && courseCodeToName.containsKey(cellData)) {
                                courseCodeToName[cellData] ?: cellData
                            } else {
                                cellData
                            }
                            
                            Text(
                                text = displayText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    if (rowIndex < tableData.rows.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
} 