package com.kentaro.guts.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kentaro.guts.MainActivity
import com.kentaro.guts.R
import com.kentaro.guts.data.TimetableResponse
import kotlinx.coroutines.*
import java.util.*

class ClassNotificationService : Service() {
    
    companion object {
        private const val TAG = "ClassNotificationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "class_notifications"
        private const val CHANNEL_NAME = "Class Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for upcoming classes"
        
        private var isServiceRunning = false
        
        fun startService(context: Context) {
            try {
                if (!isServiceRunning) {
                    val intent = Intent(context, ClassNotificationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    isServiceRunning = true
                    Log.d(TAG, "Service start requested")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}")
                isServiceRunning = false
            }
        }
        
        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ClassNotificationService::class.java)
                context.stopService(intent)
                isServiceRunning = false
                Log.d(TAG, "Service stop requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service: ${e.message}")
            }
        }
        
        fun isRunning(): Boolean = isServiceRunning
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val classScheduler = ClassScheduler()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ClassNotificationService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ClassNotificationService started")
        
        try {
            // Start foreground service with a placeholder notification
            val notification = createPlaceholderNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            // Start the notification loop
            serviceScope.launch {
                startNotificationLoop()
            }
            
            Log.d(TAG, "Service started successfully as foreground service")
            return START_STICKY
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
            // Stop the service if we can't start it as foreground
            stopSelf()
            return START_NOT_STICKY
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ClassNotificationService destroyed")
        serviceScope.cancel()
        isServiceRunning = false
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private suspend fun startNotificationLoop() {
        while (serviceScope.isActive) {
            try {
                // Check for next class
                val nextClassInfo = classScheduler.getNextClass(
                    NotificationDataManager.getTimetableResponse(),
                    NotificationDataManager.getCourseData()
                )
                
                if (nextClassInfo != null && nextClassInfo.classInfo != null) {
                    val classInfo = nextClassInfo.classInfo
                    val minutesUntil = nextClassInfo.minutesUntilClass
                    
                    Log.d(TAG, "Next class: ${classInfo.courseTitle} in $minutesUntil minutes")
                    
                    // Send notification if class is within 10 minutes
                    if (minutesUntil <= 10 && minutesUntil > 0) {
                        sendClassNotification(classInfo, minutesUntil, nextClassInfo.isToday)
                    }
                } else {
                    Log.d(TAG, "No upcoming classes found")
                }
                
                // Wait for 10 minutes before checking again
                delay(10 * 60 * 1000) // 10 minutes in milliseconds
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in notification loop: ${e.message}")
                delay(60 * 1000) // Wait 1 minute before retrying
            }
        }
    }
    
    private fun sendClassNotification(
        classInfo: ClassInfo,
        minutesUntil: Long,
        isToday: Boolean
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val title = if (isToday) {
            "Next Class: ${classInfo.courseTitle}"
        } else {
            "Tomorrow's Class: ${classInfo.courseTitle}"
        }
        
        val timeText = if (isToday) {
            "in $minutesUntil minutes"
        } else {
            "at ${classInfo.startTime}"
        }
        
        val message = "${classInfo.courseCode} - ${classInfo.courseFaculty}\n$timeText"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        Log.d(TAG, "Sent notification: $title - $message")
    }
    
    private fun createPlaceholderNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Class Notifications Active")
            .setContentText("Monitoring for upcoming classes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 