package com.kentaro.guts.service

import com.kentaro.guts.data.TimetableResponse
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationDataManager {
    private var timetableResponse: TimetableResponse? = null
    private var courseData: String? = null
    
    fun setTimetableData(timetable: TimetableResponse?, course: String?) {
        timetableResponse = timetable
        courseData = course
    }
    
    fun getTimetableResponse(): TimetableResponse? = timetableResponse
    
    fun getCourseData(): String? = courseData
    
    fun hasData(): Boolean {
        return timetableResponse != null && courseData != null
    }

    fun sendNextSlotNotification(context: Context, slotTitle: String, slotTime: String) {
        val channelId = "next_slot_channel"
        val notificationId = 1001
        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Next Slot Notifications"
            val descriptionText = "Notifications for next timetable slot"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Next Slot: $slotTitle")
            .setContentText("Time: $slotTime")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
} 