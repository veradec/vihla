package com.kentaro.guts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kentaro.guts.viewmodel.LoginEvent
import com.kentaro.guts.viewmodel.LoginState
import com.kentaro.guts.viewmodel.LoginViewModel

@Composable
fun CalendarCourseScreen(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Calendar & Course Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Calendar Data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEvent(LoginEvent.FetchCalendar) },
                            enabled = !state.isFetchingCalendar,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isFetchingCalendar) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Fetch Calendar")
                            }
                        }
                        
                        Button(
                            onClick = { onEvent(LoginEvent.ShowCalendar) },
                            enabled = state.calendarData != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Calendar")
                        }
                        
                        Button(
                            onClick = { onEvent(LoginEvent.HideCalendar) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hide")
                        }
                    }
                    
                    if (state.showCalendar && state.calendarData != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = state.calendarData!!,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Course Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEvent(LoginEvent.HandleCourseButtonClick) },
                            enabled = !state.isFetchingCourse,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isFetchingCourse) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("View Course")
                            }
                        }
                        
                        Button(
                            onClick = { onEvent(LoginEvent.ShowCourseTable) },
                            enabled = state.courseData != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Show Course Table")
                        }
                        
                        Button(
                            onClick = { onEvent(LoginEvent.HideCourse) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hide")
                        }
                    }
                    
                    if (state.showCourse && state.courseData != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = state.courseData!!,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (state.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Error: ${state.error}",
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    if (state.success != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = state.success,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    if (state.error == null && state.success == null) {
                        Text(
                            text = "No status to display",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 