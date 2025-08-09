package com.kentaro.guts.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val emoji: String? = null,
    val icon: ImageVector? = null
) {
    object Attendance : BottomNavItem("attendance", "Att", emoji = "✅")
    object Calendar : BottomNavItem("calendar", "Cal", emoji = "📅")
    object Course : BottomNavItem("course", "Course", emoji = "📚")
    object Timetable : BottomNavItem("timetable", "Slots", emoji = "⏳")
    object Marks : BottomNavItem("marks", "Marks", emoji = "💯")
    object Profile : BottomNavItem("profile", "User", icon = Icons.Filled.Person)
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
                icon = {
                    if (item.emoji != null) {
                        Text(item.emoji)
                    } else if (item.icon != null) {
                        Icon(item.icon, contentDescription = item.title)
                    }
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
} 