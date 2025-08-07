package com.kentaro.guts.data

import com.google.gson.annotations.SerializedName
import java.util.Calendar

// Response models for user validation
data class UserValidationResponse(
    val lookup: LookupData?,
    val status_code: Int,
    val message: String?
)

data class LookupData(
    val identifier: String,
    val digest: String
)

data class UserValidationResult(
    val data: UserValidationData? = null,
    val error: String? = null,
    val errorReason: String? = null
)

data class UserValidationData(
    val identifier: String,
    val digest: String,
    val status_code: Int,
    val message: String?
)

// Response models for password validation
data class PasswordAuthRequest(
    val passwordauth: PasswordAuth
)

data class PasswordAuth(
    val password: String
)

data class PasswordValidationResponse(
    val status_code: Int,
    val localized_message: String?,
    val cdigest: String?
)

data class PasswordValidationResult(
    val data: PasswordValidationData? = null,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val errorReason: String? = null
)

data class PasswordValidationData(
    val cookies: String? = null,
    val statusCode: Int,
    val message: String? = null,
    val captcha: CaptchaData? = null
)

data class CaptchaData(
    val required: Boolean,
    val digest: String?
)

// Cached credentials data model
data class CachedCredentials(
    val digest: String,
    val cookies: String,
    val identifier: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Attendance data models
data class AttendanceResult(
    val data: String? = null,
    val error: String? = null,
    val errorReason: String? = null
)

// Parsed attendance data models
data class ParsedAttendanceResult(
    val attendance: List<AttendanceDetail>? = null,
    val tablesData: String? = null, // JSON string containing all tables data
    val error: String? = null,
    val status: Int = 200
)

data class AttendanceDetail(
    val courseCode: String,
    val courseTitle: String,
    val courseCategory: String,
    val courseFaculty: String,
    val courseSlot: String,
    val courseConducted: Int,
    val courseAbsent: Int,
    val courseAttendance: String
) {
    // Calculate the number of classes needed to reach 75% attendance
    fun getClassesNeededFor75Percent(): Int {
        val totalClasses = courseConducted + courseAbsent
        val currentAttendance = courseConducted.toDouble() / totalClasses
        
        if (currentAttendance >= 0.75) {
            return 0 // Already at or above 75%
        }
        
        // Formula: (0.75 * (total + x) - conducted) / 0.25 = x
        // Where x is the number of additional classes needed
        // Simplified: x = (0.75 * total - conducted) / 0.25
        val additionalClasses = ((0.75 * totalClasses - courseConducted) / 0.25).toInt()
        
        // Ensure we return at least 1 if calculation results in 0 or negative
        return maxOf(1, additionalClasses)
    }
    
    // Calculate the margin (number of classes you can miss) when above 75%
    fun getMarginFor75Percent(): Int {
        val totalClasses = courseConducted + courseAbsent
        val currentAttendance = courseConducted.toDouble() / totalClasses
        
        if (currentAttendance < 0.75) {
            return 0 // Below 75%, no margin
        }
        
        // Calculate how many more absences are allowed while staying above 75%
        // Formula: (conducted - 0.75 * (total + x)) / 0.25 = x
        // Where x is the number of additional absences allowed
        // Simplified: x = (conducted - 0.75 * total) / 0.25
        val margin = ((courseConducted - 0.75 * totalClasses) / 0.25).toInt()
        
        return maxOf(0, margin)
    }
    
    // Calculate current attendance percentage as double
    fun getCurrentAttendancePercentage(): Double {
        val totalClasses = courseConducted + courseAbsent
        return if (totalClasses > 0) {
            (courseConducted.toDouble() / totalClasses) * 100
        } else {
            0.0
        }
    }
}

// New models for comprehensive table extraction
data class TableData(
    val tableIndex: Int,
    val headers: List<String>,
    val rows: List<List<String>>,
    val tableAttributes: Map<String, String> = emptyMap()
)

data class AllTablesResult(
    val tables: List<TableData>? = null,
    val error: String? = null,
    val status: Int = 200
)

// Dynamic URL generation object
object DynamicUrl {
    suspend fun calendarDynamicUrl(): String {
        val currentDate = java.time.LocalDate.now()
        val currentYear = currentDate.year
        val currentMonth = currentDate.month

        val (academicYearString, semesterType) = if (currentMonth in java.time.Month.JANUARY..java.time.Month.JUNE) {
            "${currentYear - 1}_${currentYear.toString().takeLast(2)}" to "EVEN"
        } else {
            "${currentYear}_${(currentYear + 1).toString().takeLast(2)}" to "ODD"
        }

        val baseUrl = "https://academia.srmist.edu.in/srm_university/academia-academic-services/page/Academic_Planner_"
        return "$baseUrl${academicYearString}_$semesterType"
    }

    suspend fun courseDynamicUrl(): String {
        val currentDate = Calendar.getInstance()
        val currentYear = currentDate.get(Calendar.YEAR)
        
        val academicYearString = "${currentYear - 2}_${(currentYear - 1).toString().takeLast(2)}"
        
        val baseUrl = "https://academia.srmist.edu.in/srm_university/academia-academic-services/page/My_Time_Table_"
        return baseUrl + academicYearString
    }
    
    suspend fun timetableDynamicUrl(): String {
        return "https://academia.srmist.edu.in/srm_university/academia-academic-services/page/Unified_Time_Table_2024_batch_2"
    }
}

// Calendar and Course data models
data class CalendarResult(
    val data: String? = null,
    val parsedData: ParsedCalendarResult? = null,
    val error: String? = null,
    val errorReason: String? = null
)

data class ParsedCalendarResult(
    val months: List<MonthData>? = null,
    val error: String? = null,
    val status: Int = 200
)

data class MonthData(
    val month: String,
    val days: List<DayData>
)

data class DayData(
    val date: String,
    val day: String,
    val event: String,
    val dayOrder: String
)

data class CourseResult(
    val data: String? = null,
    val error: String? = null,
    val errorReason: String? = null
)

// Parsed course tables data model
data class ParsedCourseResult(
    val tablesData: String? = null, // JSON string containing all tables data
    val error: String? = null,
    val status: Int = 200
)

data class TimetableResult(
    val data: String?,
    val error: String?,
    val status: Int
) 

data class TimetableResponse(
    val status: Int,
    val error: String? = null,
    val rawHtml: String? = null,
    val tableCount: Int? = null
)

 