package com.glassous.fiatimetable

import android.os.Bundle
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glassous.fiatimetable.navigation.Screen
import com.glassous.fiatimetable.ui.components.BottomNavigationBar
import com.glassous.fiatimetable.ui.screen.DayViewScreen
import com.glassous.fiatimetable.ui.screen.WeekViewScreen
import com.glassous.fiatimetable.ui.screen.SettingsScreen
import com.glassous.fiatimetable.ui.theme.FiaTimeTableTheme
import com.glassous.fiatimetable.data.repository.TimeTableRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repo = remember(context) { TimeTableRepository(context) }
            var themePref by remember { mutableStateOf(repo.getTheme()) }
            val startPref by remember { mutableStateOf(repo.getStartPage()) }

            // 监听 SharedPreferences 的主题变化，实现即时切换
            DisposableEffect(repo) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "xf_theme") {
                        themePref = prefs.getString("xf_theme", "system") ?: "system"
                    }
                }
                repo.registerOnThemeChangedListener(listener)
                onDispose { repo.unregisterOnThemeChangedListener(listener) }
            }

            val darkTheme = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val startRoute = if (startPref == "day") Screen.DayView.route else Screen.WeekView.route

            FiaTimeTableTheme(darkTheme = darkTheme) {
                MainScreen(startDestinationRoute = startRoute)
            }
        }
    }
}

@Composable
fun MainScreen(startDestinationRoute: String) {
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
            startDestination = startDestinationRoute,
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