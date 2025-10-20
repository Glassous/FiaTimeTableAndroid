package com.glassous.fiatimetable

import android.os.Bundle
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
        
        // 设置系统窗口为透明，实现沉浸式效果
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置导航栏为完全透明
        window.navigationBarColor = Color.TRANSPARENT
        
        // 对于Android 10及以上版本，禁用导航栏对比度强制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
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
            
            // 根据主题设置系统栏外观，实现真正的沉浸式效果
            LaunchedEffect(darkTheme) {
                val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
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
    
    // 纯净模式：仅周视图使用该模式
    var pureMode by remember { mutableStateOf(false) }

    // 纯净模式切换时隐藏/显示系统栏，实现全屏
    val activity = (LocalContext.current as? ComponentActivity)
    DisposableEffect(pureMode) {
        val win = activity?.window
        val view = win?.decorView
        val controller = if (win != null && view != null) WindowCompat.getInsetsController(win, view) else null
        if (controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (pureMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // TabBar removed; top bar handles navigation
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestinationRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.DayView.route) {
                DayViewScreen(
                    onNavigateCycle = { navController.navigate(Screen.WeekView.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
            composable(Screen.WeekView.route) {
                WeekViewScreen(
                    pureMode = pureMode,
                    onEnterPureMode = { pureMode = true },
                    onExitPureMode = { pureMode = false },
                    onNavigateCycle = { navController.navigate(Screen.Settings.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateCycle = { navController.navigate(Screen.DayView.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
        }
    }
}