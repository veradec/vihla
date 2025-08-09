package com.kentaro.guts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.kentaro.guts.ui.theme.GoogleSansCodeFont
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.kentaro.guts.data.AllTablesResult
import com.kentaro.guts.data.CachedCredentials
import com.kentaro.guts.data.ParsedAttendanceResult
import com.kentaro.guts.data.TableData
import com.kentaro.guts.repository.AuthRepository
// Notifications removed for now
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CachedCredentialsScreen(
    cachedCredentials: CachedCredentials?,
    onLogout: () -> Unit,
    onShowAttendance: () -> Unit = {},
    onShowCalendar: () -> Unit = {},
    onShowCourse: () -> Unit = {},
    onShowMarks: () -> Unit = {},
    attendanceData: String? = null,
    parsedAttendanceData: ParsedAttendanceResult? = null,
    cachedTableData: String? = null,
    isFetchingAttendance: Boolean = false,
    repository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = "Welcome back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (cachedCredentials != null) {
            // Try to get cached table 2 data first, then fallback to full attendance data
            val table2Data = repository.getCachedTable2Data()
            val tableDataToUse = parsedAttendanceData?.tablesData ?: cachedTableData
            
            if (table2Data != null) {
                // Use cached table 2 data directly
                val gson = Gson()
                val table2 = try {
                    gson.fromJson(table2Data, TableData::class.java)
                } catch (e: Exception) {
                    null
                }
                
                if (table2 != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Extract student information from the specific rows
                            val registrationNumber = table2.rows.getOrNull(0)?.getOrNull(1) ?: ""
                            val name = table2.rows.getOrNull(1)?.getOrNull(1) ?: ""
                            val department = table2.rows.getOrNull(2)?.getOrNull(1) ?: ""
                            val specialization = table2.rows.getOrNull(4)?.getOrNull(1) ?: ""
                            
                            // Display the formatted student information
                            StudentInfoRow("Name", name)
                            StudentInfoRow("Registration Number", registrationNumber)
                            StudentInfoRow("Course", "$department ($specialization)")
                        }
                    }
                } else {
                    // Fallback to full attendance data
                    displayStudentInfoFromFullData(tableDataToUse)
                }
            } else if (tableDataToUse != null) {
                // Fallback to full attendance data
                displayStudentInfoFromFullData(tableDataToUse)
            } else {
                // No data available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "No student information available. Fetch attendance to see your details.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShowAttendance,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Attendance", fontSize = 12.sp)
                }
                
                Button(
                    onClick = onShowCourse,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Courses", fontSize = 12.sp)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShowCalendar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Calendar", fontSize = 12.sp)
                }
                
                Button(
                    onClick = onShowMarks,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Marks", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notifications removed for now
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logout button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            // No cached credentials
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "❌ No Cached Credentials",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No cached credentials found. Please log in again.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun displayStudentInfoFromFullData(tableDataToUse: String?) {
    if (tableDataToUse != null) {
        val gson = Gson()
        val allTablesResult = try {
            gson.fromJson(tableDataToUse, AllTablesResult::class.java)
        } catch (e: Exception) {
            null
        }
        
        if (allTablesResult?.tables != null && allTablesResult.tables.size > 1) {
            // Show table 2 (index 1) from the complete set of tables
            val table2 = allTablesResult.tables[1]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Extract student information from the specific rows
                    val registrationNumber = table2.rows.getOrNull(0)?.getOrNull(1) ?: ""
                    val name = table2.rows.getOrNull(1)?.getOrNull(1) ?: ""
                    val department = table2.rows.getOrNull(2)?.getOrNull(1) ?: ""
                    val specialization = table2.rows.getOrNull(4)?.getOrNull(1) ?: ""
                    
                    // Display the formatted student information
                    StudentInfoRow("Name", name)
                    StudentInfoRow("Registration Number", registrationNumber)
                    StudentInfoRow("Course", "$department ($specialization)")
                }
            }
        } else {
            // No table data available
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
        // No parsed data available
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "No table data available. Fetch attendance to see table 2.",
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudentInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}

// Notifications removed: toggle omitted