package com.glassous.fiatimetable

import android.os.Bundle
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.content.Intent
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
        
        // 设置系统栏为完全透明（状态栏 + 导航栏）
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 移除导航栏分割线（部分品牌会显示一条细线）
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        
        // 对于Android 10及以上版本，禁用导航栏对比度强制（避免底部半透明遮罩）
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

            val intentRoute = intent.getStringExtra("xf_start_route")
            val startRouteDefault = when (startPref) {
                "day" -> Screen.DayView.route
                "course" -> Screen.CourseView.route
                else -> Screen.WeekView.route
            }

            FiaTimeTableTheme(darkTheme = darkTheme) {
                MainScreen(startDestinationRoute = intentRoute ?: startRouteDefault)
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
            // removed duplicate DayView route with intent-based navigation
            composable(Screen.WeekView.route) {
                val ctx = LocalContext.current
                val activity = LocalContext.current as? ComponentActivity
                WeekViewScreen(
                    pureMode = pureMode,
                    onEnterPureMode = { pureMode = true },
                    onExitPureMode = { pureMode = false },
                    onNavigateCycle = { navController.navigate(Screen.CourseView.route) },
                    onStartCourseView = { navController.navigate(Screen.CourseView.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
            composable(Screen.DayView.route) {
                val ctx = LocalContext.current
                val activity = LocalContext.current as? ComponentActivity
                DayViewScreen(
                    onNavigateCycle = { navController.navigate(Screen.WeekView.route) },
                    onStartCourseView = { navController.navigate(Screen.CourseView.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
            composable(Screen.CourseView.route) {
                val ctx = LocalContext.current
                val repo = remember(ctx) { TimeTableRepository(ctx) }
                CourseViewScreen(
                    repository = repo,
                    onNavigateCycle = { navController.navigate(Screen.DayView.route) },
                    onNavigateTo = { screen -> navController.navigate(screen.route) }
                )
            }
        }
    }
}