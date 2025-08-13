package com.kentaro.guts.service

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.kentaro.guts.service.NotificationDataManager
import com.kentaro.guts.service.ClassNotificationService

class TestNotificationActivity : Activity() {
    
    companion object {
        private const val TAG = "TestNotificationActivity"
        
        fun startActivity(context: Context) {
            val intent = android.content.Intent(context, TestNotificationActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val titleText = TextView(this).apply {
            text = "Test Notifications"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }
        
        val testButton = Button(this).apply {
            text = "Send Test Notification"
            setOnClickListener {
                sendTestNotification()
            }
        }
        
        val testNextSlotButton = Button(this).apply {
            text = "Test Next Slot Notification"
            setOnClickListener {
                NotificationDataManager.sendNextSlotNotification(
                    this@TestNotificationActivity,
                    "Test Course",
                    "10:00 AM"
                )
                Toast.makeText(this@TestNotificationActivity, "Next slot notification sent", Toast.LENGTH_SHORT).show()
            }
        }
        
        val testPersistentButton = Button(this).apply {
            text = "Test Persistent Notification"
            setOnClickListener {
                NotificationDataManager.showPersistentCourseNotification(
                    this@TestNotificationActivity,
                    "Test Persistent Course"
                )
                Toast.makeText(this@TestNotificationActivity, "Persistent notification sent", Toast.LENGTH_SHORT).show()
            }
        }
        
        val startServiceButton = Button(this).apply {
            text = "Start Notification Service"
            setOnClickListener {
                ClassNotificationService.startService(this@TestNotificationActivity)
                Toast.makeText(this@TestNotificationActivity, "Service started", Toast.LENGTH_SHORT).show()
            }
        }
        
        val stopServiceButton = Button(this).apply {
            text = "Stop Notification Service"
            setOnClickListener {
                ClassNotificationService.stopService(this@TestNotificationActivity)
                Toast.makeText(this@TestNotificationActivity, "Service stopped", Toast.LENGTH_SHORT).show()
            }
        }
        
        layout.addView(titleText)
        layout.addView(testButton)
        layout.addView(testNextSlotButton)
        layout.addView(testPersistentButton)
        layout.addView(startServiceButton)
        layout.addView(stopServiceButton)
        
        setContentView(layout)
    }
    
    private fun sendTestNotification() {
        try {
            val channelId = "test_channel"
            val notificationId = 9999
            
            // Create notification channel if needed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val name = "Test Notifications"
                val descriptionText = "Test notification channel"
                val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
                val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: android.app.NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
            
            val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification from the app")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            
            with(androidx.core.app.NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
            
            Toast.makeText(this, "Test notification sent successfully", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Test notification sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending test notification: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}