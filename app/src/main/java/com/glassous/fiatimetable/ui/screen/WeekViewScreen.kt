package com.glassous.fiatimetable.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData
import com.glassous.fiatimetable.ui.viewmodel.WeekViewViewModel
import com.glassous.fiatimetable.ui.viewmodel.WeekViewViewModelFactory
import com.glassous.fiatimetable.ui.dialog.CourseEditDialog

/**
 * 周视图屏幕 - 课程表主界面
 * 严格遵守 DATA_STRUCTURE.md 中的数据结构定义
 */
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekViewScreen() {
    val context = LocalContext.current
    val viewModel: WeekViewViewModel = viewModel(
        factory = WeekViewViewModelFactory(context)
    )
    
    val timeTableData by viewModel.timeTableData.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()

    // 页面恢复时刷新，确保设置页更改生效
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingDayIndex by remember { mutableStateOf(0) }
    var editingTimeSlotIndex by remember { mutableStateOf(0) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    
    // 处理格子点击事件
    val handleCellClick = { dayIndex: Int, timeSlotIndex: Int ->
        // 获取当前格子的课程（不按周过滤，便于编辑原始数据）
        val currentCourses = timeTableData.courses[selectedTerm]?.get(dayIndex)?.get(timeSlotIndex)
        
        editingDayIndex = dayIndex
        editingTimeSlotIndex = timeSlotIndex
        editingCourse = when (currentCourses) {
            is List<*> -> {
                // 如果有课程，编辑第一个课程
                currentCourses.filterIsInstance<Course>().firstOrNull()
            }
            else -> null // 新建课程
        }
        showEditDialog = true
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏：周次显示与切换
        TopAppBar(
            title = {
                Text(text = "第${currentWeek}周")
            },
            actions = {
                IconButton(onClick = { viewModel.prevWeek() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上一周")
                }
                IconButton(onClick = { viewModel.nextWeek() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下一周")
                }
            }
        )

        // 课程表网格 - 直接显示，无需学期选择器
        TimeTableGrid(
            timeSlots = timeTableData.timeSlots,
            courses = timeTableData.courses[selectedTerm] ?: emptyMap(), 
            weekDates = weekDates,
            currentWeek = currentWeek,
            onCellClick = handleCellClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
    
    // 课程编辑对话框
    if (showEditDialog) {
        CourseEditDialog(
            course = editingCourse,
            dayIndex = editingDayIndex,
            timeSlotIndex = editingTimeSlotIndex,
            onDismiss = { 
                showEditDialog = false 
                editingCourse = null
            },
            onSave = { course ->
                // 保存课程到ViewModel
                viewModel.saveCourse(course, editingDayIndex, editingTimeSlotIndex)
                showEditDialog = false
                editingCourse = null
            }
        )
    }
}

@Composable
private fun TimeTableGrid(
    timeSlots: List<String>,
    courses: Map<Int, Map<Int, Any>>, 
    weekDates: List<String>,
    currentWeek: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 过滤函数：根据当前周返回该格子的有效数据
    fun cellForWeek(dayIndex: Int, slotIndex: Int): Any? {
        val dayCourses = courses[dayIndex] ?: return null
        val data = dayCourses[slotIndex] ?: return null
        return when (data) {
            is List<*> -> {
                val course = data.filterIsInstance<Course>().firstOrNull()
                if (course != null && course.selectedWeeks.contains(currentWeek)) data else null
            }
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = dayCourses[from]
                    if (origin is List<*>) {
                        val course = origin.filterIsInstance<Course>().firstOrNull()
                        if (course != null && course.selectedWeeks.contains(currentWeek)) data else null
                    } else null
                } else null
            }
            else -> null
        }
    }

    // 计算是否需要显示“晚休”（仅当本周晚上存在课程时）
    val eveningStartIndex = timeSlots.indexOfFirst { segmentOf(it) == "evening" }
    val hasEveningSlots = eveningStartIndex != -1

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 128.dp)
    ) {
        // 表头 - 星期与左侧时间列（含日期）
        item {
            HeaderWithSegment(weekDates = weekDates)
        }
        // 取消中间空隙，避免割裂感
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 课程表内容，加入午休/晚休分隔
        itemsIndexed(timeSlots) { timeSlotIndex, timeSlot ->
            val currentSegment = segmentOf(timeSlot)
            val previousSegment = if (timeSlotIndex > 0) segmentOf(timeSlots[timeSlotIndex - 1]) else null
            Column {
                if (timeSlotIndex > 0 && currentSegment != previousSegment) {
                    val breakLabel = when {
                        previousSegment == "morning" && currentSegment == "afternoon" -> "午休"
                        previousSegment == "afternoon" && currentSegment == "evening" && hasEveningSlots -> "晚休"
                        else -> null
                    }
                    if (breakLabel != null) {
                        BreakHeader(breakLabel)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 时间段
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(120.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val normalizedSlot = timeSlot.replace("~", "-").replace("—", "-").replace("–", "-")
                        val startText = normalizedSlot.substringBefore("-")
                        val endText = normalizedSlot.substringAfter("-")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = startText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = endText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    // 每日课程格子
                    TimeTableData.weekDayNames.forEachIndexed { dayIndex, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .clickable { onCellClick(dayIndex, timeSlotIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            val courseData = cellForWeek(dayIndex, timeSlotIndex)
                            when (courseData) {
                                is List<*> -> {
                                    val courseList = courseData.filterIsInstance<Course>()
                                    if (courseList.isNotEmpty()) {
                                        val course = courseList.first()
                                        CourseContent(course = course, isMainCell = true)
                                    }
                                }
                                is Map<*, *> -> {
                                    if (courseData["continued"] == true) {
                                        val courseColor = courseData["color"] as? String ?: "#2196F3"
                                        val courseName = courseData["courseName"] as? String ?: ""
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    try {
                                                        Color(android.graphics.Color.parseColor(courseColor)).copy(alpha = 0.8f)
                                                    } catch (e: Exception) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (courseName.isNotEmpty()) {
                                                Text(
                                                    text = courseName,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 17.sp,
                                                    modifier = Modifier.padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // 空课程格：首次点击显示+号，二次点击触发添加；几秒后自动隐藏
                                    EmptyCellPlus(onClick = { onCellClick(dayIndex, timeSlotIndex) })
                                }
                            }
                        }
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
            .height(32.dp)
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

// 移除分段中文（上午/下午/晚上）显示，仅保留“时间”+星期+日期
@Composable
private fun HeaderWithSegment(weekDates: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧时间列标题
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "时间",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 星期列标题 - 直接显示所有7天，并在下方显示日期mm/dd
            TimeTableData.weekDayNames.forEachIndexed { dayIndex, dayName ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        val dateText = weekDates.getOrNull(dayIndex) ?: ""
                        if (dateText.isNotEmpty()) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                if (dayIndex < TimeTableData.weekDayNames.size - 1) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyCellPlus(onClick: () -> Unit) {
    var showAddIcon by remember { mutableStateOf(false) }

    LaunchedEffect(showAddIcon) {
        if (showAddIcon) {
            kotlinx.coroutines.delay(3000)
            showAddIcon = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                if (showAddIcon) {
                    onClick()
                } else {
                    showAddIcon = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showAddIcon) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加课程",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CourseCell(
    courses: Any?,
    dayIndex: Int,
    timeSlotIndex: Int,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddIcon by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                when (courses) {
                    null -> {
                        // 空格子，如果已显示+号则进入编辑，否则显示+号
                        if (showAddIcon) {
                            onCellClick(dayIndex, timeSlotIndex)
                        } else {
                            showAddIcon = true
                        }
                    }
                    is List<*> -> {
                        // 有课程，直接进入编辑
                        onCellClick(dayIndex, timeSlotIndex)
                    }
                    is Map<*, *> -> {
                        // 延续标记，查找原始课程进行编辑
                        // 向上查找原始课程的位置
                        var originalSlot = timeSlotIndex - 1
                        while (originalSlot >= 0) {
                            // 这里需要从父组件传递完整的课程数据来查找原始课程
                            // 暂时不处理点击，或者可以传递一个回调来处理
                            break
                        }
                    }
                }
            }
    ) {
        when (courses) {
            is List<*> -> {
                // 处理课程列表
                val courseList = courses.filterIsInstance<Course>()
                if (courseList.isNotEmpty()) {
                    val course = courseList.first() // 暂时只显示第一个课程
                    CourseContent(course = course, isMainCell = true)
                }
            }
            is Map<*, *> -> {
                // 处理延续标记
                if (courses["continued"] == true) {
                    val courseColor = courses["color"] as? String ?: "#2196F3"
                    val courseName = courses["courseName"] as? String ?: ""
                    // 延续标记不显示边框，创造连续卡片的视觉效果
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(courseColor)).copy(alpha = 0.8f)
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 显示课程名称而不是"..."
                        if (courseName.isNotEmpty()) {
                            Text(
                                text = courseName,
                                color = Color.White,
                                fontSize = 15.sp, // 继续增大延续标记的字体大小从13sp到15sp
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 3, // 增加最大行数从2到3，允许更多换行
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp, // 添加行高设置
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
            else -> {
                if (showAddIcon) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加课程",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseContent(course: Course, isMainCell: Boolean = true) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(course.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = 0.8f))
            // 移除圆角和边框，创造连续卡片效果
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3, // 增加最大行数从2到3，允许更多换行
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp, // 继续增大字体从14sp到16sp
                lineHeight = 18.sp // 添加行高设置
            )
            
            if (course.room.isNotEmpty()) {
                Text(
                    text = course.room,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2, // 增加最大行数从1到2，允许换行
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp, // 继续增大字体从12sp到14sp
                    lineHeight = 16.sp // 添加行高设置
                )
            }
            
            if (course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2, // 增加最大行数从1到2，允许换行
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp, // 继续增大字体从11sp到13sp
                    lineHeight = 15.sp // 添加行高设置
                )
            }
            
            // 如果是多节课程，在主格子显示节数信息
            if (isMainCell && course.duration > 1) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${course.duration}节",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp, // 继续增大字体从10sp到12sp
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun EmptyCellContent(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加课程",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ContinuationCell(colorHex: String) {
    val bg = try {
        Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.8f)
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "…",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ObsoleteHeaderWithSegment(initialSegment: String) {
    val segmentLabel = when (initialSegment) {
        "morning" -> "上午"
        "afternoon" -> "下午"
        else -> "晚上"
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 时间列标题（集成分段标签）
            Box(
                modifier = Modifier
                    .width(45.dp)
                    .height(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "时间",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = segmentLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 星期列标题 - 直接显示所有7天
            TimeTableData.weekDayNames.forEachIndexed { dayIndex, dayName ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (dayIndex < TimeTableData.weekDayNames.size - 1) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}