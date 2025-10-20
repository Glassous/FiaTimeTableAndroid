package com.glassous.fiatimetable.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData
import com.glassous.fiatimetable.ui.viewmodel.DayViewViewModel
import com.glassous.fiatimetable.ui.viewmodel.DayViewViewModelFactory

// Pager 相关
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import com.glassous.fiatimetable.navigation.Screen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme

private fun segmentOf(slot: String): String {
    val parts = slot.split(":")
    val time = parts.firstOrNull() ?: return ""
    val hour = time.split("-").firstOrNull()?.split(":")?.firstOrNull()?.toIntOrNull() ?: return ""
    return when (hour) {
        in 6..11 -> "morning"
        in 12..17 -> "afternoon"
        else -> "evening"
    }
}

private fun endTimeOf(slot: String): java.time.LocalTime {
    val end = slot.split("-").getOrNull(1) ?: "00:00"
    val h = end.split(":").getOrNull(0)?.toIntOrNull() ?: 0
    val m = end.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    return java.time.LocalTime.of(h, m)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayViewScreen(onNavigateCycle: () -> Unit, onNavigateTo: (Screen) -> Unit) {
    val context = LocalContext.current
    val viewModel: DayViewViewModel = viewModel(factory = DayViewViewModelFactory(context))

    val timeTableData by viewModel.timeTableData.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val currentDayIndex by viewModel.currentDayIndex.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val showBreaks by viewModel.showBreaks.collectAsState()

    // 页面恢复时刷新数据，确保设置页更改生效
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 预告逻辑（标题用）
    val timeSlotsForPreview = timeTableData.timeSlots
    val todayHasCoursePreview = timeSlotsForPreview.indices.any { viewModel.cellForWeek(currentDayIndex, it) != null }
    val lastSlotWithCoursePreview = timeSlotsForPreview.indices.filter { viewModel.cellForWeek(currentDayIndex, it) != null }.lastOrNull()
    val alreadyFinishedTodayPreview = lastSlotWithCoursePreview?.let { java.time.LocalTime.now().isAfter(endTimeOf(timeSlotsForPreview[it])) } ?: false
    val showTomorrowPreviewHeader = viewModel.isAtToday() && (!todayHasCoursePreview || alreadyFinishedTodayPreview)
    val termWeeksHeader = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: currentWeek
    val displayDayIndexHeader = if (showTomorrowPreviewHeader) (currentDayIndex + 1) % 7 else currentDayIndex
    val displayWeekHeader = if (showTomorrowPreviewHeader && currentDayIndex == 6) (currentWeek + 1).coerceAtMost(termWeeksHeader) else currentWeek

    val dayName = TimeTableData.weekDayNames.getOrNull(displayDayIndexHeader) ?: ""
    val dayDate = if (showTomorrowPreviewHeader) {
        val term = timeTableData.terms.find { it.name == selectedTerm }
        val dfYMD = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dfMD = java.time.format.DateTimeFormatter.ofPattern("MM/dd")
        if (term != null) {
            val start = java.time.LocalDate.parse(term.startDate, dfYMD)
            val date = start.plusWeeks((displayWeekHeader - 1).toLong()).plusDays(displayDayIndexHeader.toLong())
            date.format(dfMD)
        } else {
            weekDates.getOrNull(displayDayIndexHeader) ?: ""
        }
    } else {
        weekDates.getOrNull(currentDayIndex) ?: ""
    }

    // Pager 状态（总天数 = 学期周数 * 7）
    val termWeeks = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: currentWeek
    val totalDays = (termWeeks * 7).coerceAtLeast(7)
    val initialPage = ((currentWeek - 1) * 7 + currentDayIndex).coerceIn(0, totalDays - 1)
    val pagerState = rememberPagerState(initialPage = initialPage) { totalDays }

    // 外部状态变化时，同步 Pager 页码
    LaunchedEffect(currentWeek, currentDayIndex, totalDays) {
        val target = ((currentWeek - 1) * 7 + currentDayIndex).coerceIn(0, totalDays - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }
    // Pager 页码变化时，更新 ViewModel 周/日状态
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        val pageWeek = page / 7 + 1
        val pageDay = page % 7
        if (pageWeek != currentWeek || pageDay != currentDayIndex) {
            viewModel.setWeekAndDay(pageWeek, pageDay)
        }
    }

    val activity = (LocalContext.current as? ComponentActivity)
    val isDarkTheme = isSystemInDarkTheme()
    DisposableEffect(isDarkTheme) {
        val win = activity?.window
        val view = win?.decorView
        if (win != null && view != null) {
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }
            val controller = WindowInsetsControllerCompat(win, view)
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
        onDispose { }
    }
    BackHandler(enabled = true) {
        activity?.moveTaskToBack(true)
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$dayName $dayDate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedTerm.isNotEmpty()) {
                            Text(
                                text = selectedTerm,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.backToToday() }, enabled = !viewModel.isAtToday()) {
                        Icon(imageVector = Icons.Filled.DateRange, contentDescription = "回到今天")
                    }
                    IconButton(onClick = { viewModel.prevDay() }) {
                        Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "上一天")
                    }
                    IconButton(onClick = { viewModel.nextDay() }) {
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "下一天")
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
                            DropdownMenuItem(text = { Text("设置") }, onClick = { onNavigateTo(Screen.Settings); menuExpanded = false })
                            DropdownMenuItem(text = { Text("日视图") }, onClick = { onNavigateTo(Screen.DayView); menuExpanded = false })
                        }
                    }
                    IconButton(onClick = { onNavigateTo(Screen.Settings) }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val timeSlots = timeTableData.timeSlots
            val hasEveningSlots = timeSlots.any { segmentOf(it) == "evening" }

            val pageWeek = page / 7 + 1
            val pageDayIndex = page % 7

            // 仅当该页是“今天”时应用明日预告逻辑
            val dfYMD = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val term = timeTableData.terms.find { it.name == selectedTerm }
            val todayIndex = java.time.LocalDate.now().dayOfWeek.value.let { it - 1 }.coerceIn(0, 6)
            val todayWeek = if (term != null) {
                val start = java.time.LocalDate.parse(term.startDate, dfYMD)
                val days = java.time.temporal.ChronoUnit.DAYS.between(start, java.time.LocalDate.now()).toInt()
                val computed = if (days >= 0) (days / 7) + 1 else 1
                computed.coerceIn(1, termWeeks)
            } else pageWeek
            val isPageToday = (pageDayIndex == todayIndex) && (pageWeek == todayWeek)

            val todayHasCourse = timeSlots.indices.any { viewModel.cellForWeekIndex(pageDayIndex, it, pageWeek) != null }
            val lastSlotWithCourse = timeSlots.indices.filter { viewModel.cellForWeekIndex(pageDayIndex, it, pageWeek) != null }.lastOrNull()
            val alreadyFinishedToday = lastSlotWithCourse?.let { java.time.LocalTime.now().isAfter(endTimeOf(timeSlots[it])) } ?: false
            val showTomorrowPreview = isPageToday && (!todayHasCourse || alreadyFinishedToday)
            val displayDayIndex = if (showTomorrowPreview) (pageDayIndex + 1) % 7 else pageDayIndex
            val displayWeek = if (showTomorrowPreview && pageDayIndex == 6) (pageWeek + 1).coerceAtMost(termWeeks) else pageWeek

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // 恢复导航栏 Insets 避让，避免底部内容被遮挡
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) {
                if (timeSlots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "暂无时间段设置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    if (showTomorrowPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .padding(bottom = 8.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "明日课程预告",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    timeSlots.forEachIndexed { slotIndex, slotLabel ->
                        val currentSegment = segmentOf(slotLabel)
                        val previousSegment = if (slotIndex > 0) segmentOf(timeSlots[slotIndex - 1]) else null
                        if (slotIndex > 0 && currentSegment != previousSegment) {
                            val breakLabel = when {
                                previousSegment == "morning" && currentSegment == "afternoon" -> "午休"
                                previousSegment == "afternoon" && currentSegment == "evening" && hasEveningSlots -> "晚休"
                                else -> null
                            }
                            if (breakLabel != null && showBreaks) {
                                BreakHeader(breakLabel)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        val cell = viewModel.cellForWeekIndex(displayDayIndex, slotIndex = slotIndex, week = displayWeek)
                        DayCourseRow(
                            slotLabel = slotLabel,
                            cellData = cell,
                            dayIndex = displayDayIndex,
                            slotIndex = slotIndex,
                            timeTableData = timeTableData,
                            selectedTerm = selectedTerm,
                            currentWeek = displayWeek
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color.Transparent, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DayCourseRow(
    slotLabel: String,
    cellData: Any?,
    dayIndex: Int,
    slotIndex: Int,
    timeTableData: TimeTableData,
    selectedTerm: String,
    currentWeek: Int
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 时间段标签
        Box(
            modifier = Modifier.width(100.dp).height(72.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = slotLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 课程格（恢复为外部无边框样式）
        CourseCellForDay(
            cellData = cellData,
            dayIndex = dayIndex,
            slotIndex = slotIndex,
            timeTableData = timeTableData,
            selectedTerm = selectedTerm,
            currentWeek = currentWeek,
            modifier = Modifier.weight(1f).height(72.dp)
        )
    }
}

@Composable
private fun CourseCellForDay(
    cellData: Any?,
    dayIndex: Int,
    slotIndex: Int,
    timeTableData: TimeTableData,
    selectedTerm: String,
    currentWeek: Int,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
    ) {
        when (cellData) {
            is List<*> -> {
                val course = cellData.filterIsInstance<Course>().firstOrNull()
                if (course != null) CourseContent(course) else EmptyCourseCell()
            }
            is Map<*, *> -> {
                if (cellData["continued"] == true) {
                    val from = (cellData["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = timeTableData.courses[selectedTerm]?.get(dayIndex)?.get(from)
                    val originCourse = when (origin) {
                        is List<*> -> origin.filterIsInstance<Course>().firstOrNull { it.selectedWeeks.contains(currentWeek) }
                        else -> null
                    }
                    if (originCourse != null) {
                        ContinuationCell(colorHex = originCourse.color, courseName = originCourse.courseName)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) { Text("延续", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                } else {
                    EmptyCourseCell()
                }
            }
            else -> EmptyCourseCell()
        }
    }
}

@Composable
private fun CourseContent(course: Course) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(course.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = 0.85f))
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(
                text = course.courseName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            if (course.room.isNotEmpty()) {
                Text(
                    text = course.room,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ContinuationCell(colorHex: String, courseName: String) {
    val bg = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    Box(
        modifier = Modifier.fillMaxSize().background(bg.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = courseName,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun EmptyCourseCell() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 留空，不显示“空”字
    }
}