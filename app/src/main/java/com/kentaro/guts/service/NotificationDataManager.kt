package com.kentaro.guts.service

import com.kentaro.guts.data.TimetableResponse

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
} 