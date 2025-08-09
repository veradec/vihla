package com.kentaro.guts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
// Notifications removed for now
import com.kentaro.guts.ui.AttendanceTableScreen
import com.kentaro.guts.ui.CachedCredentialsScreen
import com.kentaro.guts.ui.InitialSetupScreen
import com.kentaro.guts.ui.LoginScreen
import com.kentaro.guts.ui.CalendarCourseScreen
import com.kentaro.guts.ui.CalendarScreen
import com.kentaro.guts.ui.CalendarViewScreen
import com.kentaro.guts.ui.CourseScreen
import com.kentaro.guts.ui.AttendanceScreen
import com.kentaro.guts.ui.MarksScreen
import com.kentaro.guts.ui.MarksTableScreen
import com.kentaro.guts.ui.CourseTableScreen
import com.kentaro.guts.ui.BottomNavigationBar
import com.kentaro.guts.ui.theme.GutsTheme
import com.kentaro.guts.viewmodel.LoginEvent
import com.kentaro.guts.viewmodel.LoginViewModel
import com.kentaro.guts.viewmodel.LoginViewModelFactory
import com.kentaro.guts.ui.TimetableScreen

class MainActivity : ComponentActivity() {
    
    // Notifications removed: no permission flow needed
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Notifications removed for now
        
        setContent {
            GutsTheme {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModelFactory(this)
                )
                
                val state by loginViewModel.state.collectAsState()
                
                // Debug bottom navigation state
                LaunchedEffect(state.hasCachedCredentials, state.isInitialSetup, state.currentBottomNavRoute) {
                    println("DEBUG: Bottom Nav - hasCachedCredentials: ${state.hasCachedCredentials}, isInitialSetup: ${state.isInitialSetup}, currentRoute: ${state.currentBottomNavRoute}")
                }
                
                // Notifications removed for now
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (state.hasCachedCredentials && !state.isInitialSetup) {
                            BottomNavigationBar(
                                currentRoute = state.currentBottomNavRoute,
                                onNavigate = { route ->
                                    loginViewModel.onEvent(LoginEvent.NavigateToBottomNav(route))
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                
                // Debug state changes
                LaunchedEffect(state.showCourseTable) {
                    println("DEBUG: showCourseTable changed to: ${state.showCourseTable}")
                }
                
                // Check for cached credentials on startup
                LaunchedEffect(Unit) {
                    loginViewModel.onEvent(LoginEvent.CheckCachedCredentials)
                }
                
                // Auto-show cached credentials if they exist
                LaunchedEffect(state.hasCachedCredentials) {
                    if (state.hasCachedCredentials && !state.showCachedCredentials) {
                        loginViewModel.onEvent(LoginEvent.ShowCachedCredentials)
                    }
                }
                
                when {
                    state.isInitialSetup -> {
                        InitialSetupScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showAttendanceTable -> {
                        println("DEBUG: Showing AttendanceTableScreen")
                        AttendanceTableScreen(
                            parsedAttendanceData = state.parsedAttendanceData,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showAttendanceScreen -> {
                        AttendanceScreen(
                            state = state,
                            onEvent = { event ->
                                loginViewModel.onEvent(event)
                            },
                            onBack = {
                                loginViewModel.onEvent(LoginEvent.HideAttendanceScreen)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showCalendar -> {
                        CalendarViewScreen(
                            parsedCalendarData = state.parsedCalendarData,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showCourseTable -> {
                        println("DEBUG: Showing CourseTableScreen, courseData length: ${state.courseData?.length ?: 0}")
                        CourseTableScreen(
                            courseData = state.courseData,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showCourse -> {
                        println("DEBUG: Showing CourseScreen, showCourseTable: ${state.showCourseTable}")
                        CourseScreen(
                            state = state,
                            onEvent = { event ->
                                loginViewModel.onEvent(event)
                            },
                            onBack = {
                                loginViewModel.onEvent(LoginEvent.HideCourse)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showMarksScreen -> {
                        MarksScreen(
                            state = state,
                            onEvent = { event ->
                                loginViewModel.onEvent(event)
                            },
                            onBack = {
                                loginViewModel.onEvent(LoginEvent.HideMarksScreen)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showMarksTable -> {
                        MarksTableScreen(
                            parsedAttendanceData = state.parsedAttendanceData,
                            courseData = state.courseData,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showCachedCredentials -> {
                        CachedCredentialsScreen(
                            cachedCredentials = state.cachedCredentials,
                            onLogout = {
                                loginViewModel.onEvent(LoginEvent.Logout)
                            },

                            onShowAttendance = {
                                loginViewModel.onEvent(LoginEvent.HandleAttendanceButtonClick)
                            },
                            onShowCalendar = {
                                loginViewModel.onEvent(LoginEvent.ShowCalendar)
                            },
                            onShowCourse = {
                                loginViewModel.onEvent(LoginEvent.HandleCourseButtonClick)
                            },
                            onShowMarks = {
                                loginViewModel.onEvent(LoginEvent.HandleMarksButtonClick)
                            },
                            attendanceData = state.attendanceData,
                            parsedAttendanceData = state.parsedAttendanceData,
                            cachedTableData = state.cachedTableData,
                            isFetchingAttendance = state.isFetchingAttendance,
                            repository = loginViewModel.repositoryInstance,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    state.showTimetable -> {
                        TimetableScreen(
                            parsedTimetable = state.parsedTimetable,
                            courseData = state.courseData,
                            parsedCalendarData = state.parsedCalendarData,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    else -> {
                        LoginScreen(
                            viewModel = loginViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
            }
        }
    }
    
    // Notifications removed for now
}