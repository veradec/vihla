package com.kentaro.guts.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.kentaro.guts.data.CachedCredentials
import com.kentaro.guts.data.AttendanceResult
import com.kentaro.guts.data.ParsedAttendanceResult
import com.kentaro.guts.data.ParsedCalendarResult
import com.kentaro.guts.data.PasswordValidationResult
import com.kentaro.guts.data.UserValidationResult
import com.kentaro.guts.data.AllTablesResult
import com.kentaro.guts.data.TimetableResponse
import com.kentaro.guts.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val identifier: String? = null,
    val digest: String? = null,
    val isAuthenticated: Boolean = false,
    val logContent: String = "",
    val hasCachedCredentials: Boolean = false,
    val cachedCredentials: CachedCredentials? = null,
    val showCachedCredentials: Boolean = false,
    val attendanceData: String? = null,
    val parsedAttendanceData: ParsedAttendanceResult? = null,
    val cachedTableData: String? = null,
    val isFetchingAttendance: Boolean = false,
    val showAttendanceTable: Boolean = false,
    val calendarData: String? = null,
    val parsedCalendarData: ParsedCalendarResult? = null,
    val courseData: String? = null,
    val isFetchingCalendar: Boolean = false,
    val isFetchingCourse: Boolean = false,
    val showCalendar: Boolean = false,
    val showCourse: Boolean = false,
    val showAttendanceScreen: Boolean = false,
    val showMarksScreen: Boolean = false,
    val showCourseTable: Boolean = false,
    val showMarksTable: Boolean = false,
    val showTimetable: Boolean = false,
    val isInitialSetup: Boolean = false,
    val lastAttendanceFetch: Long = 0,
    val lastMarksFetch: Long = 0,
    val currentBottomNavRoute: String = "profile",
    val timetableData: String? = null,
    val parsedTimetable: TimetableResponse? = null
)

sealed class LoginEvent {
    data class UsernameChanged(val username: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    object Login : LoginEvent()
    object ClearError : LoginEvent()
    object ClearSuccess : LoginEvent()
    object ViewLogs : LoginEvent()
    object ClearLogs : LoginEvent()
    object CheckCachedCredentials : LoginEvent()
    object ShowCachedCredentials : LoginEvent()
    object Logout : LoginEvent()
    object FetchAttendance : LoginEvent()
    object ShowAttendanceTable : LoginEvent()
    object HideAttendanceTable : LoginEvent()
    object FetchCalendar : LoginEvent()
    object FetchCourseDetails : LoginEvent()
    object ShowCalendar : LoginEvent()
    object ShowCourse : LoginEvent()
    object HideCalendar : LoginEvent()
    object HideCourse : LoginEvent()
    object ShowAttendanceScreen : LoginEvent()
    object ShowMarksScreen : LoginEvent()
    object HideAttendanceScreen : LoginEvent()
    object HideMarksScreen : LoginEvent()
    object ShowCourseTable : LoginEvent()
    object HideCourseTable : LoginEvent()
    object ShowMarksTable : LoginEvent()
    object HideMarksTable : LoginEvent()
    object HandleCourseButtonClick : LoginEvent()
    object HandleAttendanceButtonClick : LoginEvent()
    object HandleMarksButtonClick : LoginEvent()
    object ShowTimetable : LoginEvent()
    object RefreshTimetable : LoginEvent()
    object RefreshCalendar : LoginEvent()
    data class NavigateToBottomNav(val route: String) : LoginEvent()
}

class LoginViewModel(private val context: Context) : ViewModel() {
    
    private val repository = AuthRepository(context)
    val repositoryInstance: AuthRepository get() = repository
    private val gson = Gson()
    
    // Rate limiting constants (200 seconds = 200,000 milliseconds)
    private val RATE_LIMIT_DURATION = 200_000L
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UsernameChanged -> {
                _state.value = _state.value.copy(username = event.username)
            }
            is LoginEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }

            is LoginEvent.Login -> {
                login()
            }
            is LoginEvent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }
            is LoginEvent.ClearSuccess -> {
                _state.value = _state.value.copy(success = null)
            }
            is LoginEvent.ViewLogs -> {
                viewLogs()
            }
            is LoginEvent.ClearLogs -> {
                clearLogs()
            }
            is LoginEvent.CheckCachedCredentials -> {
                checkCachedCredentials()
            }
            is LoginEvent.ShowCachedCredentials -> {
                showCachedCredentials()
            }
            is LoginEvent.Logout -> {
                logout()
            }
            is LoginEvent.FetchAttendance -> {
                fetchAttendance()
            }
            is LoginEvent.ShowAttendanceTable -> {
                _state.value = _state.value.copy(showAttendanceTable = true)
            }
            is LoginEvent.HideAttendanceTable -> {
                _state.value = _state.value.copy(showAttendanceTable = false)
            }
            is LoginEvent.FetchCalendar -> {
                fetchCalendar()
            }
            is LoginEvent.FetchCourseDetails -> {
                fetchCourseDetails()
            }
            is LoginEvent.ShowCalendar -> {
                _state.value = _state.value.copy(showCalendar = true)
                // Only fetch calendar data if not already available
                if (_state.value.calendarData == null) {
                    fetchCalendar()
                }
            }
            is LoginEvent.ShowTimetable -> {
                _state.value = _state.value.copy(showTimetable = true)
                // Only fetch timetable data if not already available
                if (_state.value.timetableData == null) {
                    fetchTimetable()
                }
            }
            is LoginEvent.RefreshTimetable -> {
                // Clear cached data and fetch fresh timetable
                repository.clearCachedTimetableData()
                _state.value = _state.value.copy(timetableData = null, parsedTimetable = null)
                fetchTimetable()
            }
            is LoginEvent.RefreshCalendar -> {
                // Clear cached data and fetch fresh calendar
                repository.clearCachedCalendarData()
                _state.value = _state.value.copy(calendarData = null, parsedCalendarData = null)
                fetchCalendar()
            }
            is LoginEvent.ShowCourse -> {
                _state.value = _state.value.copy(showCourse = true)
            }
            is LoginEvent.HideCalendar -> {
                _state.value = _state.value.copy(showCalendar = false)
            }
            is LoginEvent.HideCourse -> {
                _state.value = _state.value.copy(showCourse = false)
            }
            is LoginEvent.ShowAttendanceScreen -> {
                _state.value = _state.value.copy(showAttendanceScreen = true)
            }
            is LoginEvent.ShowMarksScreen -> {
                _state.value = _state.value.copy(showMarksScreen = true)
            }
            is LoginEvent.HideAttendanceScreen -> {
                _state.value = _state.value.copy(showAttendanceScreen = false)
            }
            is LoginEvent.HideMarksScreen -> {
                _state.value = _state.value.copy(showMarksScreen = false)
            }
            is LoginEvent.ShowCourseTable -> {
                println("DEBUG: ShowCourseTable event received")
                _state.value = _state.value.copy(showCourseTable = true)
                println("DEBUG: showCourseTable set to true")
            }
            is LoginEvent.HideCourseTable -> {
                _state.value = _state.value.copy(showCourseTable = false)
            }
            is LoginEvent.ShowMarksTable -> {
                _state.value = _state.value.copy(showMarksTable = true)
            }
            is LoginEvent.HideMarksTable -> {
                _state.value = _state.value.copy(showMarksTable = false)
            }
            is LoginEvent.HandleCourseButtonClick -> {
                handleCourseButtonClick()
            }
            is LoginEvent.HandleAttendanceButtonClick -> {
                handleAttendanceButtonClick()
            }
            is LoginEvent.HandleMarksButtonClick -> {
                handleMarksButtonClick()
            }
            is LoginEvent.NavigateToBottomNav -> {
                handleBottomNavigation(event.route)
            }
        }
    }
    

    
    private fun login() {
        val username = _state.value.username
        val password = _state.value.password
        
        if (username.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter a username")
            return
        }
        
        if (password.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter a password")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // First validate the user
                val userResult = repository.validateUser(username)
                
                if (userResult.error != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid username. Please check your username and try again."
                    )
                    return@launch
                }
                
                // If user validation succeeds, get the identifier and digest
                val userData = userResult.data
                if (userData == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid username. Please check your username and try again."
                    )
                    return@launch
                }
                
                // Now validate the password
                val passwordResult = repository.validatePassword(userData.identifier, userData.digest, password)
                
                if (passwordResult.error != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid password. Please check your password and try again."
                    )
                } else if (passwordResult.isAuthenticated) {
                    // Refresh cached credentials after successful login
                    val cachedCredentials = repository.getCachedCredentials()
                    val cachedTableData = repository.getCachedTableData()
                    val cachedCalendarData = repository.getCachedCalendarData()
                    val cachedCourseData = repository.getCachedCourseData()
                    
                    // Check if this is the first time (no cached table data)
                    val isFirstTime = cachedTableData == null
                    
                    if (isFirstTime) {
                        // First time login - start initial setup
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            success = "Login successful!",
                            isInitialSetup = true,
                            hasCachedCredentials = cachedCredentials != null,
                            cachedCredentials = cachedCredentials,
                            cachedTableData = cachedTableData,
                            calendarData = cachedCalendarData,
                            courseData = cachedCourseData
                        )
                        // Start fetching attendance data
                        fetchAttendanceForInitialSetup()
                    } else {
                        // Not first time - go directly to cached credentials screen
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            success = "Login successful!",
                            showCachedCredentials = true,
                            hasCachedCredentials = cachedCredentials != null,
                            cachedCredentials = cachedCredentials,
                            cachedTableData = cachedTableData,
                            calendarData = cachedCalendarData,
                            courseData = cachedCourseData
                        )
                    }
                } else {
                    val message = passwordResult.data?.message ?: "Invalid password. Please check your password and try again."
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = message
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun viewLogs() {
        val logContent = repository.getLogFileContent()
        _state.value = _state.value.copy(logContent = logContent)
    }
    
    private fun clearLogs() {
        repository.clearLogFile()
        _state.value = _state.value.copy(logContent = "")
    }
    
    private fun checkCachedCredentials() {
        val cachedCredentials = repository.getCachedCredentials()
        val cachedTableData = repository.getCachedTableData()
        val cachedCalendarData = repository.getCachedCalendarData()
        val cachedCourseData = repository.getCachedCourseData()
        _state.value = _state.value.copy(
            hasCachedCredentials = cachedCredentials != null,
            cachedCredentials = cachedCredentials,
            cachedTableData = cachedTableData,
            calendarData = cachedCalendarData,
            courseData = cachedCourseData
        )
    }
    
    private fun showCachedCredentials() {
        // Refresh cached credentials before showing the screen
        val cachedCredentials = repository.getCachedCredentials()
        val cachedTableData = repository.getCachedTableData()
        val cachedCalendarData = repository.getCachedCalendarData()
        val cachedCourseData = repository.getCachedCourseData()
        
        _state.value = _state.value.copy(
            showCachedCredentials = true,
            hasCachedCredentials = cachedCredentials != null,
            cachedCredentials = cachedCredentials,
            cachedTableData = cachedTableData,
            calendarData = cachedCalendarData,
            courseData = cachedCourseData
        )
    }
    
    private fun logout() {
        repository.clearCachedCredentials()
        repository.clearCachedTableData()
        repository.clearCachedAttendanceData()
        repository.clearCachedCalendarData()
        repository.clearCachedCourseData()
        _state.value = _state.value.copy(
            hasCachedCredentials = false,
            cachedCredentials = null,
            cachedTableData = null,
            showCachedCredentials = false,
            showAttendanceTable = false,
            showCalendar = false,
            showCourse = false,
            showAttendanceScreen = false,
            showMarksScreen = false,
            showCourseTable = false,
            showMarksTable = false,
            calendarData = null,
            parsedCalendarData = null,
            courseData = null,
            isAuthenticated = false,
            isInitialSetup = false,
            lastAttendanceFetch = 0,
            lastMarksFetch = 0,
            username = "",
            password = "",
            identifier = null,
            digest = null,
            success = "Logged out successfully"
        )
    }
    
    private fun fetchAttendance() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingAttendance = true, error = null)
            
            try {
                val result = repository.fetchAttendance()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        error = result.error
                    )
                } else {
                    // Parse the JSON response
                    val parsedData = if (result.data != null) {
                        try {
                            // Try to parse as AllTablesResult first
                            val allTablesResult = gson.fromJson(result.data, AllTablesResult::class.java)
                            if (allTablesResult?.tables != null) {
                                println("DEBUG: Parsed all tables data: ${allTablesResult.tables.size} tables")
                                allTablesResult.tables.forEachIndexed { index, table ->
                                    println("DEBUG: Table ${index + 1}: ${table.headers.size} headers, ${table.rows.size} rows")
                                }
                                // Create ParsedAttendanceResult with tablesData
                                ParsedAttendanceResult(
                                    attendance = null,
                                    tablesData = result.data,
                                    status = 200
                                )
                            } else {
                                // Fallback to old attendance parsing
                                val parsed = gson.fromJson(result.data, ParsedAttendanceResult::class.java)
                                println("DEBUG: Parsed attendance data: ${parsed?.attendance?.size ?: 0} courses")
                                parsed?.attendance?.forEach { course ->
                                    println("DEBUG: Course: ${course.courseCode} - ${course.courseAttendance}")
                                }
                                parsed
                            }
                        } catch (e: Exception) {
                            println("DEBUG: JSON parsing error: ${e.message}")
                            null
                        }
                    } else null
                    
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        attendanceData = result.data,
                        parsedAttendanceData = parsedData,
                        success = "Tables data fetched successfully!",
                        showAttendanceTable = true
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingAttendance = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun fetchAttendanceInBackground() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingAttendance = true, error = null)
            
            try {
                val result = repository.fetchAttendance()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        error = result.error
                    )
                } else {
                    // Parse the JSON response
                    val parsedData = if (result.data != null) {
                        try {
                            // Try to parse as AllTablesResult first
                            val allTablesResult = gson.fromJson(result.data, AllTablesResult::class.java)
                            if (allTablesResult?.tables != null) {
                                println("DEBUG: Parsed all tables data: ${allTablesResult.tables.size} tables")
                                allTablesResult.tables.forEachIndexed { index, table ->
                                    println("DEBUG: Table ${index + 1}: ${table.headers.size} headers, ${table.rows.size} rows")
                                }
                                // Create ParsedAttendanceResult with tablesData
                                ParsedAttendanceResult(
                                    attendance = null,
                                    tablesData = result.data,
                                    status = 200
                                )
                            } else {
                                // Fallback to old attendance parsing
                                val parsed = gson.fromJson(result.data, ParsedAttendanceResult::class.java)
                                println("DEBUG: Parsed attendance data: ${parsed?.attendance?.size ?: 0} courses")
                                parsed?.attendance?.forEach { course ->
                                    println("DEBUG: Course: ${course.courseCode} - ${course.courseAttendance}")
                                }
                                parsed
                            }
                        } catch (e: Exception) {
                            println("DEBUG: JSON parsing error: ${e.message}")
                            null
                        }
                    } else null
                    
                    // Log attendance data if available
                    if (parsedData?.attendance != null) {
                        repository.logAttendanceDataLegacy(parsedData.attendance)
                    }
                    
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        attendanceData = result.data,
                        parsedAttendanceData = parsedData,
                        success = "Attendance data updated successfully!"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingAttendance = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun fetchAttendanceForInitialSetup() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingAttendance = true, error = null)
            
            try {
                val result = repository.fetchAttendance()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        error = result.error,
                        isInitialSetup = false,
                        showCachedCredentials = true
                    )
                } else {
                    // Parse the JSON response
                    val parsedData = if (result.data != null) {
                        try {
                            // Try to parse as AllTablesResult first
                            val allTablesResult = gson.fromJson(result.data, AllTablesResult::class.java)
                            if (allTablesResult?.tables != null) {
                                println("DEBUG: Parsed all tables data: ${allTablesResult.tables.size} tables")
                                allTablesResult.tables.forEachIndexed { index, table ->
                                    println("DEBUG: Table ${index + 1}: ${table.headers.size} headers, ${table.rows.size} rows")
                                }
                                // Create ParsedAttendanceResult with tablesData
                                ParsedAttendanceResult(
                                    attendance = null,
                                    tablesData = result.data,
                                    status = 200
                                )
                            } else {
                                // Fallback to old attendance parsing
                                val parsed = gson.fromJson(result.data, ParsedAttendanceResult::class.java)
                                println("DEBUG: Parsed attendance data: ${parsed?.attendance?.size ?: 0} courses")
                                parsed?.attendance?.forEach { course ->
                                    println("DEBUG: Course: ${course.courseCode} - ${course.courseAttendance}")
                                }
                                parsed
                            }
                        } catch (e: Exception) {
                            println("DEBUG: JSON parsing error: ${e.message}")
                            null
                        }
                    } else null
                    
                    // Log attendance data if available
                    if (parsedData?.attendance != null) {
                        repository.logAttendanceDataLegacy(parsedData.attendance)
                    }
                    
                    // Refresh cached credentials after successful fetch (no attendance caching)
                    val cachedCredentials = repository.getCachedCredentials()
                    val cachedCalendarData = repository.getCachedCalendarData()
                    val cachedCourseData = repository.getCachedCourseData()
                    
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        isInitialSetup = false,
                        showCachedCredentials = true,
                        attendanceData = result.data,
                        parsedAttendanceData = parsedData,
                        success = "Initial setup complete!",
                        hasCachedCredentials = cachedCredentials != null,
                        cachedCredentials = cachedCredentials,
                        calendarData = cachedCalendarData,
                        courseData = cachedCourseData
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingAttendance = false,
                    error = "Network error: ${e.message}",
                    isInitialSetup = false,
                    showCachedCredentials = true
                )
            }
        }
    }
    
    private fun fetchCalendar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingCalendar = true, error = null)
            
            try {
                val result = repository.fetchCalendar()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingCalendar = false,
                        error = result.error
                    )
                } else {
                    _state.value = _state.value.copy(
                        isFetchingCalendar = false,
                        calendarData = result.data,
                        parsedCalendarData = result.parsedData,
                        success = "Calendar data fetched successfully!",
                        showCalendar = true
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingCalendar = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }

    private fun fetchTimetable() {
        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)
            
            try {
                val result = repository.fetchTimetable()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        error = result.error
                    )
                } else {
                    // Parse the timetable HTML
                    val parsedTimetable = repository.parseTimetable(result.data ?: "")
                    
                    if (parsedTimetable?.error != null) {
                        _state.value = _state.value.copy(
                            error = parsedTimetable.error
                        )
                    } else {
                        _state.value = _state.value.copy(
                            timetableData = result.data,
                            parsedTimetable = parsedTimetable,
                            success = "Timetable data fetched successfully!",
                            showTimetable = true
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun handleCourseButtonClick() {
        println("DEBUG: Course button clicked - clearing cache and fetching fresh data...")
        
        // Clear cached course data to ensure fresh fetch with correct URL
        repository.clearCachedCourseData()
        
        // Always show the course table immediately
        _state.value = _state.value.copy(showCourseTable = true)
        
        // Always fetch fresh data with the correct URL
        println("DEBUG: Fetching fresh course data with correct URL")
        fetchCourseDetailsInBackground()
    }
    
    private fun handleAttendanceButtonClick() {
        println("DEBUG: Attendance button clicked - checking cache and rate limit...")
        
        // Check if we have cached data that's still valid (within 200 seconds)
        if (repository.isAttendanceDataCachedWithin200s()) {
            val cachedData = repository.getCachedAttendanceData()
            if (cachedData != null) {
                println("DEBUG: Using cached attendance data from repository")
                // Parse cached data and show it
                val parsedData = try {
                    val allTablesResult = gson.fromJson(cachedData, AllTablesResult::class.java)
                    if (allTablesResult?.tables != null) {
                        ParsedAttendanceResult(
                            attendance = null,
                            tablesData = cachedData,
                            status = 200
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
                
                _state.value = _state.value.copy(
                    showAttendanceTable = true,
                    attendanceData = cachedData,
                    parsedAttendanceData = parsedData,
                    success = "Showing cached attendance data"
                )
                return
            }
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFetch = currentTime - _state.value.lastAttendanceFetch
        
        // Check rate limit for fresh fetch
        if (timeSinceLastFetch < RATE_LIMIT_DURATION) {
            val remainingTime = (RATE_LIMIT_DURATION - timeSinceLastFetch) / 1000
            println("DEBUG: Rate limit active - ${remainingTime}s remaining")
            _state.value = _state.value.copy(
                error = "Please wait ${remainingTime} seconds before fetching attendance data again"
            )
            return
        }
        
        // Always show the attendance table immediately
        _state.value = _state.value.copy(showAttendanceTable = true)
        
        // Fetch fresh data and cache it
        println("DEBUG: Fetching fresh attendance data and caching for 200s")
        fetchAttendanceWithRateLimit()
    }
    
    private fun fetchAttendanceWithRateLimit() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingAttendance = true, error = null)
            
            try {
                val result = repository.fetchAttendance()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        error = result.error
                    )
                } else {
                    // Parse the JSON response
                    val parsedData = if (result.data != null) {
                        try {
                            // Try to parse as AllTablesResult first
                            val allTablesResult = gson.fromJson(result.data, AllTablesResult::class.java)
                            if (allTablesResult?.tables != null) {
                                println("DEBUG: Parsed all tables data: ${allTablesResult.tables.size} tables")
                                allTablesResult.tables.forEachIndexed { index, table ->
                                    println("DEBUG: Table ${index + 1}: ${table.headers.size} headers, ${table.rows.size} rows")
                                }
                                // Create ParsedAttendanceResult with tablesData
                                ParsedAttendanceResult(
                                    attendance = null,
                                    tablesData = result.data,
                                    status = 200
                                )
                            } else {
                                // Fallback to old attendance parsing
                                val parsed = gson.fromJson(result.data, ParsedAttendanceResult::class.java)
                                println("DEBUG: Parsed attendance data: ${parsed?.attendance?.size ?: 0} courses")
                                parsed?.attendance?.forEach { course ->
                                    println("DEBUG: Course: ${course.courseCode} - ${course.courseAttendance}")
                                }
                                parsed
                            }
                        } catch (e: Exception) {
                            println("DEBUG: JSON parsing error: ${e.message}")
                            null
                        }
                    } else null
                    
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        attendanceData = result.data,
                        parsedAttendanceData = parsedData,
                        success = "Attendance data fetched successfully!",
                        lastAttendanceFetch = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingAttendance = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun handleMarksButtonClick() {
        println("DEBUG: Marks button clicked - checking rate limit...")
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastFetch = currentTime - _state.value.lastMarksFetch
        
        if (timeSinceLastFetch < RATE_LIMIT_DURATION) {
            val remainingTime = (RATE_LIMIT_DURATION - timeSinceLastFetch) / 1000
            println("DEBUG: Rate limit active - ${remainingTime}s remaining")
            _state.value = _state.value.copy(
                error = "Please wait ${remainingTime} seconds before fetching marks data again"
            )
            return
        }
        
        // Always show the marks table immediately
        _state.value = _state.value.copy(showMarksTable = true)
        
        // Always fetch fresh data (no caching)
        println("DEBUG: Fetching fresh marks data (no cache)")
        fetchMarksWithRateLimit()
    }
    
    private fun fetchMarksWithRateLimit() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingAttendance = true, error = null)
            
            try {
                val result = repository.fetchAttendance()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        error = result.error
                    )
                } else {
                    // Parse the JSON response
                    val parsedData = if (result.data != null) {
                        try {
                            // Try to parse as AllTablesResult first
                            val allTablesResult = gson.fromJson(result.data, AllTablesResult::class.java)
                            if (allTablesResult?.tables != null) {
                                println("DEBUG: Parsed all tables data: ${allTablesResult.tables.size} tables")
                                allTablesResult.tables.forEachIndexed { index, table ->
                                    println("DEBUG: Table ${index + 1}: ${table.headers.size} headers, ${table.rows.size} rows")
                                }
                                // Create ParsedAttendanceResult with tablesData
                                ParsedAttendanceResult(
                                    attendance = null,
                                    tablesData = result.data,
                                    status = 200
                                )
                            } else {
                                // Fallback to old attendance parsing
                                val parsed = gson.fromJson(result.data, ParsedAttendanceResult::class.java)
                                println("DEBUG: Parsed attendance data: ${parsed?.attendance?.size ?: 0} courses")
                                parsed?.attendance?.forEach { course ->
                                    println("DEBUG: Course: ${course.courseCode} - ${course.courseAttendance}")
                                }
                                parsed
                            }
                        } catch (e: Exception) {
                            println("DEBUG: JSON parsing error: ${e.message}")
                            null
                        }
                    } else null
                    
                    _state.value = _state.value.copy(
                        isFetchingAttendance = false,
                        attendanceData = result.data,
                        parsedAttendanceData = parsedData,
                        success = "Marks data fetched successfully!",
                        lastMarksFetch = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingAttendance = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun fetchCourseDetails() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingCourse = true, error = null)
            
            try {
                val result = repository.fetchCourseDetails()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingCourse = false,
                        error = result.error
                    )
                } else {
                    _state.value = _state.value.copy(
                        isFetchingCourse = false,
                        courseData = result.data,
                        success = "Course details fetched successfully!",
                        showCourseTable = true
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingCourse = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun fetchCourseDetailsInBackground() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFetchingCourse = true, error = null)
            
            try {
                val result = repository.fetchCourseDetails()
                
                if (result.error != null) {
                    _state.value = _state.value.copy(
                        isFetchingCourse = false,
                        error = result.error
                    )
                } else {
                    _state.value = _state.value.copy(
                        isFetchingCourse = false,
                        courseData = result.data,
                        success = "Course details updated successfully!"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isFetchingCourse = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    private fun handleBottomNavigation(route: String) {
        // Reset all screen flags
        _state.value = _state.value.copy(
            showAttendanceTable = false,
            showAttendanceScreen = false,
            showCalendar = false,
            showCourse = false,
            showCourseTable = false,
            showTimetable = false,
            showMarksScreen = false,
            showMarksTable = false,
            showCachedCredentials = false,
            currentBottomNavRoute = route
        )
        
        when (route) {
            "attendance" -> {
                println("DEBUG: Bottom nav - attendance route selected")
                _state.value = _state.value.copy(showAttendanceTable = true)
                // Fetch attendance data if not already available
                if (_state.value.parsedAttendanceData == null) {
                    println("DEBUG: No cached attendance data, fetching...")
                    handleAttendanceButtonClick()
                } else {
                    println("DEBUG: Using cached attendance data")
                }
            }
            "calendar" -> {
                println("DEBUG: Bottom nav - calendar route selected")
                _state.value = _state.value.copy(showCalendar = true)
                // Only fetch calendar data if not already available
                if (_state.value.calendarData == null) {
                    println("DEBUG: No cached calendar data, fetching...")
                    fetchCalendar()
                } else {
                    println("DEBUG: Using cached calendar data")
                }
            }
            "course" -> {
                println("DEBUG: Bottom nav - course route selected")
                // Always use handleCourseButtonClick to clear cache and fetch fresh data
                handleCourseButtonClick()
            }
            "timetable" -> {
                println("DEBUG: Bottom nav - timetable route selected")
                _state.value = _state.value.copy(showTimetable = true)
                // Only fetch timetable data if not already available
                if (_state.value.timetableData == null) {
                    println("DEBUG: No cached timetable data, fetching...")
                    fetchTimetable()
                } else {
                    println("DEBUG: Using cached timetable data")
                }
            }
            "marks" -> {
                _state.value = _state.value.copy(showMarksTable = true)
                // Fetch marks data if not already available
                if (_state.value.parsedAttendanceData == null) {
                    handleMarksButtonClick()
                }
            }
            "profile" -> {
                _state.value = _state.value.copy(showCachedCredentials = true)
            }
        }
    }
    

}

// Factory for creating LoginViewModel with context
class LoginViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 