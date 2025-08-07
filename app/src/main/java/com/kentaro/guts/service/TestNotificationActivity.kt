package com.kentaro.guts.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kentaro.guts.ui.theme.GutsTheme

class TestNotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GutsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Notification Test",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                // Test the notification service
                                try {
                                    if (NotificationDataManager.hasData() && !ClassNotificationService.isRunning()) {
                                        ClassNotificationService.startService(this@TestNotificationActivity)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("TestNotificationActivity", "Failed to start service: ${e.message}")
                                }
                            }
                        ) {
                            Text("Start Notification Service")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    ClassNotificationService.stopService(this@TestNotificationActivity)
                                } catch (e: Exception) {
                                    android.util.Log.e("TestNotificationActivity", "Failed to stop service: ${e.message}")
                                }
                            }
                        ) {
                            Text("Stop Notification Service")
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "Has timetable data: ${NotificationDataManager.hasData()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
} 