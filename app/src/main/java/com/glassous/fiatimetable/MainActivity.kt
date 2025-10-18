package com.glassous.fiatimetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glassous.fiatimetable.navigation.Screen
import com.glassous.fiatimetable.ui.components.BottomNavigationBar
import com.glassous.fiatimetable.ui.screen.DayViewScreen
import com.glassous.fiatimetable.ui.screen.WeekViewScreen
import com.glassous.fiatimetable.ui.screen.SettingsScreen
import com.glassous.fiatimetable.ui.theme.FiaTimeTableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FiaTimeTableTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.DayView,
        Screen.WeekView,
        Screen.Settings
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                screens = screens
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.WeekView.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.DayView.route) {
                DayViewScreen()
            }
            composable(Screen.WeekView.route) {
                WeekViewScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}