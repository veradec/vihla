package com.kentaro.guts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kentaro.guts.data.ParsedCalendarResult
import com.kentaro.guts.data.MonthData
import com.kentaro.guts.data.DayData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarViewScreen(
    parsedCalendarData: ParsedCalendarResult?,
    modifier: Modifier = Modifier
) {
    val months = parsedCalendarData?.months ?: emptyList()

    // Find initial month index based on current date
    val initialMonthIndex = remember(months) {
        if (months.isNotEmpty()) {
            val currentDate = java.time.LocalDate.now()
            val currentMonthName = currentDate.month.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.ENGLISH
            )
            val currentYear = currentDate.year.toString()

            println("DEBUG: Current date - Month: $currentMonthName, Year: $currentYear")
            println("DEBUG: Available months: ${months.map { it.month }}")

            // Try multiple matching strategies
            var matchIndex = -1

            // Strategy 1: Exact month + year match (e.g., "Aug '25")
            matchIndex = months.indexOfFirst { month ->
                month.month.contains(currentMonthName) && month.month.contains(currentYear)
            }
            println("DEBUG: Strategy 1 (exact match) result: $matchIndex")

            // Strategy 2: Month name only (case insensitive)
            if (matchIndex == -1) {
                matchIndex = months.indexOfFirst { month ->
                    month.month.lowercase().contains(currentMonthName.lowercase())
                }
                println("DEBUG: Strategy 2 (month only) result: $matchIndex")
            }

            // Strategy 3: Partial month name match
            if (matchIndex == -1) {
                val shortMonthName = currentMonthName.take(3) // Take first 3 letters
                matchIndex = months.indexOfFirst { month ->
                    month.month.lowercase().contains(shortMonthName.lowercase())
                }
                println("DEBUG: Strategy 3 (partial match) result: $matchIndex")
            }

            // Strategy 4: Find the most recent month (closest to current date)
            if (matchIndex == -1) {
                // Try to find any month that's not too far in the past
                val currentMonthNum = currentDate.monthValue
                matchIndex = months.indexOfFirst { month ->
                    // Look for any month that might be current or recent
                    month.month.contains(currentMonthName) ||
                    month.month.contains("Dec") || // December is often in academic calendars
                    month.month.contains("Jan") || // January is often in academic calendars
                    month.month.contains("Aug") || // August is often in academic calendars
                    month.month.contains("Sep")    // September is often in academic calendars
                }
                println("DEBUG: Strategy 4 (recent month) result: $matchIndex")
            }

            if (matchIndex != -1) {
                println("DEBUG: Final match found at index: $matchIndex")
                matchIndex
            } else {
                println("DEBUG: No match found, defaulting to index 0")
                0 // Default to first month if no match found
            }
        } else {
            println("DEBUG: No months available, defaulting to index 0")
            0
        }
    }

    var currentMonthIndex by remember { mutableStateOf(initialMonthIndex) }
    
    // Update currentMonthIndex when initialMonthIndex changes
    LaunchedEffect(initialMonthIndex) {
        currentMonthIndex = initialMonthIndex
        println("DEBUG: Updated currentMonthIndex to: $currentMonthIndex")
    }
    
    val currentMonth = if (months.isNotEmpty() && currentMonthIndex < months.size) {
        months[currentMonthIndex]
    } else null
    
    // Debug logging for selected month
    LaunchedEffect(currentMonthIndex, currentMonth) {
        println("DEBUG: Selected month index: $currentMonthIndex")
        println("DEBUG: Selected month: ${currentMonth?.month}")
    }

    // Remember list state for auto-scroll
    val dayListState = rememberLazyListState()

    // Auto-scroll to today's date when the current month changes or parsed data updates
    LaunchedEffect(currentMonth?.month) {
        val month = currentMonth ?: return@LaunchedEffect
        val today = java.time.LocalDate.now().dayOfMonth
        val targetIndex = month.days.indexOfFirst { day ->
            day.date.toIntOrNull() == today
        }
        if (targetIndex >= 0) {
            try {
                dayListState.animateScrollToItem(targetIndex)
                println("DEBUG: Auto-scrolled to today's index: $targetIndex")
                val todaysDayOrder = month.days.getOrNull(targetIndex)?.dayOrder ?: ""
                println("DEBUG: Today's day order at index $targetIndex: ${if (todaysDayOrder.isBlank()) "-" else todaysDayOrder}")
            } catch (e: Exception) {
                println("DEBUG: Failed to auto-scroll: ${e.message}")
            }
        } else {
            println("DEBUG: Today's date not found in current month days")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (parsedCalendarData?.error != null) {
            // Error state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: ${parsedCalendarData.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        } else if (currentMonth != null) {
            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonthIndex > 0) {
                            currentMonthIndex = currentMonthIndex - 1
                        }
                    },
                    enabled = currentMonthIndex > 0
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Month")
                }
                
                Text(
                    text = currentMonth.month,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = {
                        if (currentMonthIndex < months.size - 1) {
                            currentMonthIndex = currentMonthIndex + 1
                        }
                    },
                    enabled = currentMonthIndex < months.size - 1
                ) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next Month")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Month card with scrollable dates
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    state = dayListState
                ) {
                    if (currentMonth.days.isNotEmpty()) {
                        items(currentMonth.days.size) { index ->
                            val dayData = currentMonth.days[index]
                            DayRow(dayData = dayData)
                            if (index < currentMonth.days.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "No events scheduled",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                    text = "No calendar data available",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthCard(
    monthData: MonthData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Month header
            Text(
                text = monthData.month,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days data
            if (monthData.days.isNotEmpty()) {
                
                monthData.days.forEach { dayData ->
                    DayRow(dayData = dayData)
                    if (dayData != monthData.days.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                Text(
                    text = "No events scheduled",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun DayRow(
    dayData: DayData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date and Day
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dayData.date,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dayData.day,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Event
        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (dayData.event.isNotEmpty()) {
                Text(
                    text = dayData.event,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Day Order
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            if (dayData.dayOrder.isNotEmpty()) {
                Text(
                    text = dayData.dayOrder,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
} 