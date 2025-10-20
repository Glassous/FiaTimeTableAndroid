package com.glassous.fiatimetable

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData
import com.glassous.fiatimetable.data.repository.TimeTableRepository
import com.glassous.fiatimetable.navigation.Screen
import com.glassous.fiatimetable.ui.theme.FiaTimeTableTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity

class CourseViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val context = LocalContext.current
            val repo = remember(context) { TimeTableRepository(context) }
            val darkPref = repo.getTheme()
            val darkTheme = when (darkPref) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            LaunchedEffect(darkTheme) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightNavigationBars = !darkTheme
                controller.isAppearanceLightStatusBars = !darkTheme
            }
            FiaTimeTableTheme(darkTheme = darkTheme) {
                CourseViewScreen(
                    repository = repo,
                    onNavigateCycle = {
                        // 循环到日视图（与周/日/课三页一致：日 -> 周 -> 课 -> 日）
                        repo.saveStartPage("day")
                        val intent = Intent(context, MainActivity::class.java)
                        intent.putExtra("xf_start_route", Screen.DayView.route)
                        context.startActivity(intent)
                        this@CourseViewActivity.overridePendingTransition(0, 0)
                        finish()
                        this@CourseViewActivity.overridePendingTransition(0, 0)
                    },
                    onNavigateTo = { screen ->
                        when (screen) {
                            Screen.DayView -> repo.saveStartPage("day")
                            Screen.WeekView -> repo.saveStartPage("week")
                            Screen.Settings -> { /* 设置入口仍保留在右侧图标 */ }
                            Screen.CourseView -> TODO()
                        }
                        val intent = Intent(context, MainActivity::class.java)
                        intent.putExtra("xf_start_route", screen.route)
                        context.startActivity(intent)
                        this@CourseViewActivity.overridePendingTransition(0, 0)
                        finish()
                        this@CourseViewActivity.overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CourseViewScreen(
    repository: com.glassous.fiatimetable.data.repository.TimeTableRepository,
    onNavigateCycle: () -> Unit,
    onNavigateTo: (com.glassous.fiatimetable.navigation.Screen) -> Unit
) {
    var data by remember { mutableStateOf(repository.getTimeTableData()) }
    val selectedTerm = data.selectedTerm.ifEmpty { repository.getSelectedTerm() }

    // 计算当前学期的周数与当前周
    val dfYMD = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val term = remember { data.terms.find { it.name == selectedTerm } }
    val currentWeek = remember {
        val start = term?.startDate?.let { LocalDate.parse(it, dfYMD) }
        if (start != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()
            val computed = if (days >= 0) (days / 7) + 1 else 1
            computed.coerceIn(1, (term?.weeks ?: 1))
        } else 1
    }
    val timeSlots = remember { data.timeSlots }
    val activity = (LocalContext.current as? androidx.activity.ComponentActivity)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                data = repository.getTimeTableData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = true) {
        activity?.moveTaskToBack(true)
    }

    // 跨天构造课程卡片列表（从今天起直到本学期结束），并标记“当前/下一节课”
    val courseCards = remember(data, selectedTerm, timeSlots) {
        val result = mutableListOf<CourseCardItem>()
        val courses = data.courses[selectedTerm] ?: emptyMap()
        val termStart = term?.startDate?.let { LocalDate.parse(it, dfYMD) }
        val termWeeks = term?.weeks ?: 1
        val termEnd = termStart?.plusDays(termWeeks * 7L - 1) ?: LocalDate.now()
        val nowDate = LocalDate.now()
        val startDate = if (termStart != null && nowDate.isBefore(termStart)) termStart else nowDate

        var d = startDate
        while (!d.isAfter(termEnd)) {
            val week = if (termStart != null) {
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(termStart, d).toInt()
                (daysBetween / 7) + 1
            } else 1
            val dayIndex = d.dayOfWeek.value - 1
            val dayCourses = courses[dayIndex] ?: emptyMap()
            timeSlots.forEachIndexed { slotIndex, slotLabel ->
                val value = dayCourses[slotIndex]
                val course: Course? = when (value) {
                    is List<*> -> value.filterIsInstance<Course>().firstOrNull { it.selectedWeeks.contains(week) }
                    is Map<*, *> -> {
                        if (value["continued"] == true) {
                            val from = (value["fromSlot"] as? Number)?.toInt() ?: slotIndex
                            val origin = dayCourses[from]
                            if (origin is List<*>) origin.filterIsInstance<Course>().firstOrNull { it.selectedWeeks.contains(week) } else null
                        } else null
                    }
                    else -> null
                }
                if (course != null) {
                    result.add(
                        CourseCardItem(
                            date = d,
                            week = week,
                            dayIndex = dayIndex,
                            slotIndex = slotIndex,
                            start = startTimeOf(slotLabel),
                            end = endTimeOf(slotLabel),
                            course = course
                        )
                    )
                }
            }
            d = d.plusDays(1)
        }
        // 排序：先按日期，再按开始时间
        result.sortWith(compareBy<CourseCardItem> { it.date }.thenBy { it.start })
        // 标记“当前在上课”或“下一节课”
        val nowTime = LocalTime.now()
        val idxCurrent = result.indexOfFirst { it.date == nowDate && nowTime >= it.start && nowTime <= it.end }
        if (idxCurrent >= 0) {
            result[idxCurrent] = result[idxCurrent].copy(isCurrent = true)
        } else {
            val idxNext = result.indexOfFirst { it.date.isAfter(nowDate) || (it.date == nowDate && nowTime < it.start) }
            if (idxNext >= 0) {
                result[idxNext] = result[idxNext].copy(isNext = true)
            }
        }
        result
    }

    // 选择初始页：在上课则定位当前课程，否则定位下一节课；若无则定位第1张（若没有课程，则第1张是“没课了啦”卡片）
    val now = LocalTime.now()
    val today = LocalDate.now()
    val initialIndex = remember(courseCards) {
        val currentIdx = courseCards.indexOfFirst { it.date == today && now >= it.start && now <= it.end }
        if (currentIdx >= 0) currentIdx else {
            val nextIdx = courseCards.indexOfFirst { it.date.isAfter(today) || (it.date == today && now < it.start) }
            if (nextIdx >= 0) nextIdx else 0
        }
    }
    val pagerState = rememberPagerState(initialPage = initialIndex) { (courseCards.size + 1).coerceAtLeast(1) }

    // PagerState 扩展：计算页面偏移（引用 https://www.sinasamaki.com/pager-animations/ 的思路）
    fun pagerOffsetForPage(page: Int): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

    Scaffold(topBar = {
        TopAppBar(
            title = {
                val dateText = remember {
                    java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date())
                }
                val dow = remember { java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) }
                val dayName = when (dow) {
                    java.util.Calendar.MONDAY -> "周一"
                    java.util.Calendar.TUESDAY -> "周二"
                    java.util.Calendar.WEDNESDAY -> "周三"
                    java.util.Calendar.THURSDAY -> "周四"
                    java.util.Calendar.FRIDAY -> "周五"
                    java.util.Calendar.SATURDAY -> "周六"
                    else -> "周日"
                }
                Column {
                    Text(text = dateText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (selectedTerm.isNotEmpty()) {
                        Text(text = "第${currentWeek}周  $dayName", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            actions = {
                // 回到当前课/下节课按钮
                IconButton(onClick = {
                    val now = LocalTime.now()
                    val today = LocalDate.now()
                    val currentIdx = courseCards.indexOfFirst { it.date == today && now >= it.start && now <= it.end }
                    val targetIdx = if (currentIdx >= 0) currentIdx else {
                        courseCards.indexOfFirst { it.date.isAfter(today) || (it.date == today && now < it.start) }
                    }
                    if (targetIdx >= 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            pagerState.animateScrollToPage(targetIdx)
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Filled.MyLocation, contentDescription = "回到当前课/下节课")
                }
                
                var menuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(onClick = onNavigateCycle, onLongClick = { menuExpanded = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = "切换页面")
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("周视图") }, onClick = { onNavigateTo(Screen.WeekView); menuExpanded = false })
                        DropdownMenuItem(text = { Text("日视图") }, onClick = { onNavigateTo(Screen.DayView); menuExpanded = false })
                    }
                }
                IconButton(onClick = { onNavigateTo(Screen.Settings) }) {
                    Icon(imageVector = Icons.Filled.Settings, contentDescription = "设置")
                }
            }
        )
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                pageSpacing = 12.dp,
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val density = LocalDensity.current
                val pageOffset = pagerOffsetForPage(page)
                val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                val dealTarget = if (absOffset < 0.01f) 1f else 0f
                val deal by animateFloatAsState(
                    targetValue = dealTarget,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    label = "card_deal"
                )
                val stackShiftPx = with(density) { (24.dp * absOffset).toPx() }
                val dealShiftPx = with(density) { (96.dp * (1f - deal)).toPx() }
                val translationY = stackShiftPx + dealShiftPx
                val scale = 1f - 0.06f * absOffset
                val rotationXDeg = 1.5f * (-pageOffset)

                if (page < courseCards.size) {
                    val item = courseCards[page]
                    CourseBigCard(
                        item = item,
                        modifier = Modifier
                            .graphicsLayer {
                                this.translationY = translationY
                                this.scaleX = scale
                                this.scaleY = scale
                                this.rotationX = rotationXDeg
                                this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                                this.shadowElevation = 0f
                            }
                    )
                } else {
                    EndOfTermCard(
                        modifier = Modifier.graphicsLayer {
                            this.translationY = with(density) { 80.dp.toPx() }
                            this.scaleX = 0.98f
                            this.scaleY = 0.98f
                            this.shadowElevation = 0f
                        }
                    )
                }
            }
        }
    }
}

private data class CourseCardItem(
    val date: LocalDate,
    val week: Int,
    val dayIndex: Int,
    val slotIndex: Int,
    val start: LocalTime,
    val end: LocalTime,
    val course: Course,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false
)

@Composable
private fun CourseBigCard(item: CourseCardItem, modifier: Modifier = Modifier) {
    val bgColor = try {
        ComposeColor(android.graphics.Color.parseColor(item.course.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    // 格式化日期和周信息
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日")
    val dateText = item.date.format(dateFormatter)
    val dayName = when (item.date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
    
    // 倒计时逻辑
    var timeRemaining by remember { mutableStateOf("") }
    var isCurrentClass by remember { mutableStateOf(false) }
    
    LaunchedEffect(item) {
        while (true) {
            val now = LocalTime.now()
            val today = LocalDate.now()
            
            if (item.date == today) {
                when {
                    now >= item.start && now <= item.end -> {
                        // 正在上课
                        isCurrentClass = true
                        val endDateTime = item.date.atTime(item.end)
                        val nowDateTime = today.atTime(now)
                        val duration = java.time.Duration.between(nowDateTime, endDateTime)
                        val minutes = duration.toMinutes()
                        val seconds = duration.seconds % 60
                        timeRemaining = "${minutes}:${seconds.toString().padStart(2, '0')}"
                    }
                    now < item.start -> {
                        // 还未开始
                        isCurrentClass = false
                        val startDateTime = item.date.atTime(item.start)
                        val nowDateTime = today.atTime(now)
                        val duration = java.time.Duration.between(nowDateTime, startDateTime)
                        val minutes = duration.toMinutes()
                        val seconds = duration.seconds % 60
                        timeRemaining = "${minutes}:${seconds.toString().padStart(2, '0')}"
                    }
                    else -> {
                        // 已结束
                        timeRemaining = ""
                    }
                }
            } else if (item.date.isAfter(today)) {
                // 未来的课程
                isCurrentClass = false
                val startDateTime = item.date.atTime(item.start)
                val nowDateTime = today.atTime(now)
                val duration = java.time.Duration.between(nowDateTime, startDateTime)
                val totalMinutes = duration.toMinutes()
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                timeRemaining = if (hours > 0) "${hours}小时${minutes}分钟" else "${minutes}分钟"
            } else {
                timeRemaining = ""
            }
            
            kotlinx.coroutines.delay(1000) // 每秒更新一次
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
            // 状态标签：当前在上课 或 下一节课是
            if (item.isCurrent) {
                Text(text = "当前在上课！！", fontSize = 18.sp, color = ComposeColor.White.copy(alpha = 0.95f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            } else if (item.isNext) {
                Text(text = "下一节课是", fontSize = 18.sp, color = ComposeColor.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            
            // 倒计时显示
            if (timeRemaining.isNotEmpty()) {
                val countdownText = if (isCurrentClass) "还有 $timeRemaining 下课" else "还有 $timeRemaining 上课"
                Text(
                    text = countdownText,
                    fontSize = 20.sp,
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                text = item.course.courseName,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ComposeColor.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.course.room.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = item.course.room,
                    fontSize = 24.sp,
                    color = ComposeColor.White.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.course.teacher.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.course.teacher,
                    fontSize = 20.sp,
                    color = ComposeColor.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(20.dp))
            
            // 日期和周信息
            Text(
                text = "$dateText  第${item.week}周  $dayName",
                fontSize = 16.sp,
                color = ComposeColor.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "${formatTime(item.start)} - ${formatTime(item.end)}",
                fontSize = 18.sp,
                color = ComposeColor.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EndOfTermCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "没课了啦", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}

private fun startTimeOf(slot: String): LocalTime {
    val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
    val start = normalized.substringBefore("-")
    val h = start.substringBefore(":").toIntOrNull() ?: 0
    val m = start.substringAfter(":", "0").toIntOrNull() ?: 0
    return LocalTime.of(h, m)
}
private fun endTimeOf(slot: String): LocalTime {
    val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
    val end = normalized.substringAfter("-", "00:00").substringBefore(" ")
    val h = end.substringBefore(":").toIntOrNull() ?: 0
    val m = end.substringAfter(":", "0").toIntOrNull() ?: 0
    return LocalTime.of(h, m)
}

private fun formatTime(time: LocalTime): String {
    return "%02d:%02d".format(time.hour, time.minute)
}