package com.kentaro.guts.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Attendance : BottomNavItem("attendance", "Attendance", Icons.Filled.List)
    object Calendar : BottomNavItem("calendar", "Calendar", Icons.Filled.List)
    object Course : BottomNavItem("course", "Course", Icons.Filled.List)
    object Timetable : BottomNavItem("timetable", "Timetable", Icons.Filled.List)
    object Marks : BottomNavItem("marks", "Marks", Icons.Filled.List)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Attendance,
        BottomNavItem.Calendar,
        BottomNavItem.Course,
        BottomNavItem.Timetable,
        BottomNavItem.Marks,
        BottomNavItem.Profile
    )
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
} 