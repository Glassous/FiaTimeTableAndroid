package com.glassous.fiatimetable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 导航屏幕定义
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object DayView : Screen(
        route = "day_view",
        title = "日视图",
        icon = Icons.Default.DateRange
    )
    
    object WeekView : Screen(
        route = "week_view", 
        title = "周视图",
        icon = Icons.Default.Home
    )
    
    object CourseView : Screen(
        route = "course_view",
        title = "课程视图",
        icon = Icons.Default.DateRange
    )
    
    object Settings : Screen(
        route = "settings",
        title = "设置",
        icon = Icons.Default.Settings
    )
}