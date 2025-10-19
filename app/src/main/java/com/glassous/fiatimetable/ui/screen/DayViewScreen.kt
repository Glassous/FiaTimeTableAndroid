package com.glassous.fiatimetable.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
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

private fun segmentOf(slot: String): String {
    val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
    val start = normalized.substringBefore("-")
    val hour = start.substringBefore(":").toIntOrNull() ?: 0
    return when {
        hour < 12 -> "morning"
        hour < 18 -> "afternoon"
        else -> "evening"
    }
}

// 新增：解析时间段的结束时间用于“已上完”判断
private fun endTimeOf(slot: String): java.time.LocalTime {
    val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
    val endStr = normalized.substringAfter("-")
    val parts = endStr.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return java.time.LocalTime.of(h, m)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen() {
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

    val dayName = TimeTableData.weekDayNames.getOrNull(currentDayIndex) ?: ""
    val dayDate = weekDates.getOrNull(currentDayIndex) ?: ""

    Scaffold(
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
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            // 时间段 + 课程列表（纵向）
            val timeSlots = timeTableData.timeSlots
            val hasEveningSlots = timeSlots.any { segmentOf(it) == "evening" }
            // 新增：判断是否展示明日课程预告（仅在“今天”视图）
            val todayHasCourse = timeSlots.indices.any { viewModel.cellForWeek(currentDayIndex, it) != null }
            val lastSlotWithCourse = timeSlots.indices.filter { viewModel.cellForWeek(currentDayIndex, it) != null }.lastOrNull()
            val alreadyFinishedToday = lastSlotWithCourse?.let { java.time.LocalTime.now().isAfter(endTimeOf(timeSlots[it])) } ?: false
            val showTomorrowPreview = viewModel.isAtToday() && (!todayHasCourse || alreadyFinishedToday)
            val displayDayIndex = if (showTomorrowPreview) (currentDayIndex + 1) % 7 else currentDayIndex
            val termWeeks = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: currentWeek
            val displayWeek = if (showTomorrowPreview && currentDayIndex == 6) (currentWeek + 1).coerceAtMost(termWeeks) else currentWeek

            if (timeSlots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "暂无时间段设置", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (showTomorrowPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "明日课程预告",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(vertical = 6.dp)
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

@Composable
private fun BreakHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
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
        // 课程格
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
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .then(
                // 仅在非空格子时显示边框
                when (cellData) {
                    is List<*> -> if (cellData.filterIsInstance<Course>().isNotEmpty()) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)) else Modifier
                    is Map<*, *> -> if (cellData["continued"] == true) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)) else Modifier
                    else -> Modifier
                }
            )
    ) {
        when (cellData) {
            is List<*> -> {
                val course = cellData.filterIsInstance<Course>().firstOrNull()
                if (course != null) CourseContent(course)
                else EmptyCourseCell()
            }
            is Map<*, *> -> {
                if (cellData["continued"] == true) {
                    // 查找原始课程以获取颜色与名称（兼容未存储color/courseName的延续标记）
                    val from = (cellData["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = timeTableData.courses[selectedTerm]?.get(dayIndex)?.get(from)
                    val originCourse = when (origin) {
                        is List<*> -> origin.filterIsInstance<Course>().firstOrNull { it.selectedWeeks.contains(currentWeek) }
                        else -> null
                    }
                    if (originCourse != null) {
                        ContinuationCell(colorHex = originCourse.color, courseName = originCourse.courseName)
                    } else {
                        // 无法确定原始课程：显示浅灰占位
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
        modifier = Modifier.fillMaxSize().background(backgroundColor.copy(alpha = 0.85f)).padding(6.dp)
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