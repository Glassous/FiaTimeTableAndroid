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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import com.glassous.fiatimetable.ui.dialog.OnlineCourseEditDialog
import com.glassous.fiatimetable.data.model.OnlineCourse

/**
 * 周视图屏幕 - 课程表主界面
 * 严格遵守 DATA_STRUCTURE.md 中的数据结构定义
 */
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
// 新增：Pager 相关导入
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeekViewScreen(
    pureMode: Boolean,
    onEnterPureMode: () -> Unit,
    onExitPureMode: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: WeekViewViewModel = viewModel(
        factory = WeekViewViewModelFactory(context)
    )
    
    val timeTableData by viewModel.timeTableData.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val showWeekend by viewModel.showWeekend.collectAsState()
    val showBreaks by viewModel.showBreaks.collectAsState()

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

    // 网络选修课对话框状态
    var showOnlineCourseDialog by remember { mutableStateOf(false) }
    var editingOnlineCourse by remember { mutableStateOf<OnlineCourse?>(null) }
    
    // Bottom Sheet 状态
    var showBottomSheet by remember { mutableStateOf(false) }
    var bottomSheetDayIndex by remember { mutableStateOf(0) }
    var bottomSheetSlotIndex by remember { mutableStateOf(0) }
    var bottomSheetCourses by remember { mutableStateOf(listOf<Course>()) }
    
    // 区分新增/编辑模式
    var isAddNew by remember { mutableStateOf(false) }
    
    // 删除确认相关状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletingCourse by remember { mutableStateOf<Course?>(null) }
    var deleteDayIndex by remember { mutableStateOf(0) }
    var deleteSlotIndex by remember { mutableStateOf(0) }

    // 网络选修课删除确认状态
    var showOnlineDeleteConfirm by remember { mutableStateOf(false) }
    var deletingOnlineCourse by remember { mutableStateOf<OnlineCourse?>(null) }
    
    // 处理格子点击事件
    val handleCellClick = { dayIndex: Int, timeSlotIndex: Int ->
        val dayCourses = timeTableData.courses[selectedTerm]?.get(dayIndex)
        val data = dayCourses?.get(timeSlotIndex)
        val coursesAtSlot = when (data) {
            is List<*> -> data.filterIsInstance<Course>()
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: timeSlotIndex
                    val origin = dayCourses?.get(from)
                    if (origin is List<*>) origin.filterIsInstance<Course>() else emptyList()
                } else emptyList()
            }
            else -> emptyList()
        }
        bottomSheetDayIndex = dayIndex
        bottomSheetSlotIndex = timeSlotIndex
        bottomSheetCourses = coursesAtSlot
        showBottomSheet = true
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏：周次显示与切换（纯净模式隐藏，带动画）
            AnimatedVisibility(
                visible = !pureMode,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
            ) {
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
                            Text(text = dateText, style = MaterialTheme.typography.titleLarge)
                            Text(text = "第${currentWeek}周  $dayName", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    actions = {
                        val atCurrentWeek = viewModel.isAtCurrentWeek()
                        if (!atCurrentWeek) {
                            IconButton(onClick = { viewModel.backToCurrentWeek() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "回到本周")
                            }
                        }
                        IconButton(onClick = { onEnterPureMode() }) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = "纯净模式")
                        }
                        IconButton(onClick = { viewModel.prevWeek() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上一周")
                        }
                        IconButton(onClick = { viewModel.nextWeek() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下一周")
                        }
                    }
                )
            }

            // 课程表网格：使用 HorizontalPager 实现左右滑动切换周
            val termWeeks = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: currentWeek
            val pagerState = rememberPagerState(initialPage = (currentWeek - 1).coerceAtLeast(0)) { termWeeks }

            // 当外部周次变化（按钮/回到本周）时，同步 Pager
            LaunchedEffect(currentWeek, termWeeks) {
                val target = (currentWeek - 1).coerceIn(0, termWeeks - 1)
                if (pagerState.currentPage != target) {
                    pagerState.scrollToPage(target)
                }
            }
            // Pager 滑动时更新 ViewModel 的周次（驱动顶部日期与内容）
            LaunchedEffect(pagerState.currentPage) {
                val targetWeek = pagerState.currentPage + 1
                if (targetWeek != currentWeek) {
                    viewModel.setWeek(targetWeek)
                }
            }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) { page ->
                val weekIndex = page + 1
                TimeTableGrid(
                    timeSlots = timeTableData.timeSlots,
                    courses = timeTableData.courses[selectedTerm] ?: emptyMap(),
                    weekDates = weekDates,
                    currentWeek = weekIndex,
                    onlineCourses = timeTableData.onlineCourses[selectedTerm] ?: emptyList(),
                    onCellClick = handleCellClick,
                    onAddOnlineCourse = {
                        editingOnlineCourse = null
                        showOnlineCourseDialog = true
                    },
                    onEditOnlineCourse = { course ->
                        editingOnlineCourse = course
                        showOnlineCourseDialog = true
                    },
                    onDeleteOnlineCourse = { course ->
                        deletingOnlineCourse = course
                        showOnlineDeleteConfirm = true
                    },
                    showWeekend = showWeekend,
                    showBreaks = showBreaks,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
        // 纯净模式退出悬浮按钮
        if (pureMode) {
            FloatingActionButton(
                onClick = { onExitPureMode() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "退出纯净模式")
            }
        }
    }
    
    // MD3 Bottom Sheet：显示该时间格的课程信息与操作
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val dayName = TimeTableData.weekDayNames.getOrNull(bottomSheetDayIndex) ?: ""
                val slotText = timeTableData.timeSlots.getOrNull(bottomSheetSlotIndex) ?: ""
                Text(
                    text = "$dayName · $slotText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (bottomSheetCourses.isEmpty()) {
                    Text(
                        text = "本周此时间段无课程。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bottomSheetCourses.forEach { course ->
                        CourseBottomSheetItem(course = course)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                // 编辑模式：若为延续格，定位到起始格
                                val raw = timeTableData.courses[selectedTerm]?.get(bottomSheetDayIndex)?.get(bottomSheetSlotIndex)
                                val originSlot = if (raw is Map<*, *> && raw["continued"] == true) {
                                    (raw["fromSlot"] as? Number)?.toInt() ?: bottomSheetSlotIndex
                                } else bottomSheetSlotIndex
                                editingCourse = course
                                editingDayIndex = bottomSheetDayIndex
                                editingTimeSlotIndex = originSlot
                                isAddNew = false
                                showBottomSheet = false
                                showEditDialog = true
                            }) { Text("编辑") }
                            OutlinedButton(onClick = {
                                val raw = timeTableData.courses[selectedTerm]?.get(bottomSheetDayIndex)?.get(bottomSheetSlotIndex)
                                val originSlot = if (raw is Map<*, *> && raw["continued"] == true) {
                                    (raw["fromSlot"] as? Number)?.toInt() ?: bottomSheetSlotIndex
                                } else bottomSheetSlotIndex
                                deletingCourse = course
                                deleteDayIndex = bottomSheetDayIndex
                                deleteSlotIndex = originSlot
                                showDeleteConfirm = true
                            }) { Text("删除") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    // 新增模式：若为延续格，定位到起始格
                    val raw = timeTableData.courses[selectedTerm]?.get(bottomSheetDayIndex)?.get(bottomSheetSlotIndex)
                    val originSlot = if (raw is Map<*, *> && raw["continued"] == true) {
                        (raw["fromSlot"] as? Number)?.toInt() ?: bottomSheetSlotIndex
                    } else bottomSheetSlotIndex
                    editingCourse = null
                    editingDayIndex = bottomSheetDayIndex
                    editingTimeSlotIndex = originSlot
                    isAddNew = true
                    showBottomSheet = false
                    showEditDialog = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("新增课程")
                }
            }
        }
    }
    
    // 课程编辑对话框
    if (showEditDialog) {
        // 计算学期总周数
        val termWeeks = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: 16
        // 计算该起始格的已有课程与占用周次
        val originData = timeTableData.courses[selectedTerm]?.get(editingDayIndex)?.get(editingTimeSlotIndex)
        val originCourses = when (originData) {
            is List<*> -> originData.filterIsInstance<Course>()
            else -> emptyList()
        }
        val occupiedWeeks = if (isAddNew) {
            originCourses.flatMap { it.selectedWeeks }.toSet()
        } else {
            originCourses.filter { it != editingCourse }.flatMap { it.selectedWeeks }.toSet()
        }
        val usedColors = originCourses.map { it.color }.toSet()
        val initialColor = TimeTableData.presetColors.firstOrNull { it !in usedColors } ?: TimeTableData.presetColors.first()

        CourseEditDialog(
            course = editingCourse,
            dayIndex = editingDayIndex,
            timeSlotIndex = editingTimeSlotIndex,
            termWeeks = termWeeks,
            occupiedWeeks = occupiedWeeks,
            initialColor = if (isAddNew) initialColor else null,
            onDismiss = { 
                showEditDialog = false 
                editingCourse = null
            },
            onSave = { course ->
                if (isAddNew) {
                    viewModel.addCourseInSameSlot(course, editingDayIndex, editingTimeSlotIndex)
                } else {
                    viewModel.saveCourse(course, editingDayIndex, editingTimeSlotIndex)
                }
                showEditDialog = false
                editingCourse = null
            }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除课程") },
            text = { Text("确认删除该课程？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deletingCourse?.let {
                        viewModel.deleteSpecificCourse(it, deleteDayIndex, deleteSlotIndex)
                    }
                    showDeleteConfirm = false
                    showBottomSheet = false
                    deletingCourse = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    // 网络选修课编辑对话框
    if (showOnlineCourseDialog) {
        val termWeeks = timeTableData.terms.find { it.name == selectedTerm }?.weeks ?: 16
        OnlineCourseEditDialog(
            course = editingOnlineCourse,
            termWeeks = termWeeks,
            onDismiss = {
                showOnlineCourseDialog = false
                editingOnlineCourse = null
            },
            onSave = { oc ->
                if (editingOnlineCourse == null) {
                    viewModel.addOnlineCourse(oc)
                } else {
                    viewModel.updateOnlineCourse(oc)
                }
                showOnlineCourseDialog = false
                editingOnlineCourse = null
            }
        )
    }
    if (showOnlineDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showOnlineDeleteConfirm = false },
            title = { Text("删除网络选修课") },
            text = { Text("确认删除该网络选修课？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deletingOnlineCourse?.let { oc ->
                        viewModel.deleteOnlineCourse(oc.id)
                    }
                    showOnlineDeleteConfirm = false
                    deletingOnlineCourse = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showOnlineDeleteConfirm = false }) { Text("取消") }
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
    onlineCourses: List<OnlineCourse>,
    onCellClick: (Int, Int) -> Unit,
    onAddOnlineCourse: () -> Unit,
    onEditOnlineCourse: (OnlineCourse) -> Unit,
    onDeleteOnlineCourse: (OnlineCourse) -> Unit,
    showWeekend: Boolean,
    showBreaks: Boolean,
    modifier: Modifier = Modifier
) {
    // 过滤函数：根据当前周返回该格子的有效数据
    fun cellForWeek(dayIndex: Int, slotIndex: Int): Any? {
        val dayCourses = courses[dayIndex] ?: return null
        val data = dayCourses[slotIndex] ?: return null
        return when (data) {
            is List<*> -> {
                val filtered = data.filterIsInstance<Course>().filter { it.selectedWeeks.contains(currentWeek) }
                if (filtered.isNotEmpty()) filtered else null
            }
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = dayCourses[from]
                    if (origin is List<*>) {
                        val filtered = origin.filterIsInstance<Course>().filter { it.selectedWeeks.contains(currentWeek) }
                        if (filtered.isNotEmpty()) data else null
                    } else null
                } else null
            }
            else -> null
        }
    }

    // 当本周该格子没有课，但其他周存在课时，返回该课程（用于灰色占位显示）
    fun cellForOtherWeeksCourse(dayIndex: Int, slotIndex: Int): Course? {
        val dayCourses = courses[dayIndex] ?: return null
        val data = dayCourses[slotIndex] ?: return null
        return when (data) {
            is List<*> -> {
                val candidates = data.filterIsInstance<Course>().filter { !it.selectedWeeks.contains(currentWeek) }
                candidates.firstOrNull()
            }
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = dayCourses[from]
                    if (origin is List<*>) {
                        val candidates = origin.filterIsInstance<Course>().filter { !it.selectedWeeks.contains(currentWeek) }
                        candidates.firstOrNull()
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
            HeaderWithSegment(weekDates = weekDates, showWeekend = showWeekend)
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
                        if (showBreaks) {
                            BreakHeader(breakLabel)
                        }
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
                    val days = if (showWeekend) TimeTableData.weekDayNames else TimeTableData.weekDayNames.take(5)
                    days.forEachIndexed { dayIndex, _ ->
                        val courseData = cellForWeek(dayIndex, timeSlotIndex)
                        val rawData = courses[dayIndex]?.get(timeSlotIndex)
                        val otherCourse = cellForOtherWeeksCourse(dayIndex, timeSlotIndex)
                        val cellHasContent = when (courseData) {
                            is List<*> -> courseData.filterIsInstance<Course>().isNotEmpty()
                            is Map<*, *> -> (courseData["continued"] == true)
                            else -> otherCourse != null
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                .then(
                                    if (cellHasContent) Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(4.dp)
                                    ) else Modifier
                                )
                                .clickable { onCellClick(dayIndex, timeSlotIndex) },
                            contentAlignment = Alignment.Center
                        ) {
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
                                        val from = (courseData["fromSlot"] as? Number)?.toInt() ?: timeSlotIndex
                                        val origin = courses[dayIndex]?.get(from)
                                        val courseForWeek = if (origin is List<*>) origin.filterIsInstance<Course>().firstOrNull { it.selectedWeeks.contains(currentWeek) } else null
                                        if (courseForWeek != null) {
                                            val courseColor = courseForWeek.color
                                            val courseName = courseForWeek.courseName
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
                                }
                                else -> {
                                    // 当前周此格无课：如果其他周存在课，显示灰色占位，否则显示添加+号
                                    if (otherCourse != null) {
                                        if (rawData is Map<*, *> && rawData["continued"] == true) {
                                            GrayContinuationCell()
                                        } else {
                                            GrayCourseContent(course = otherCourse, isMainCell = true)
                                        }
                                    } else {
                                        EmptyCellPlus(onClick = { onCellClick(dayIndex, timeSlotIndex) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 在课程表下方增加网络选修课模块
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            OnlineCoursesSection(
                onlineCourses = onlineCourses,
                currentWeek = currentWeek,
                onAdd = onAddOnlineCourse,
                onEdit = onEditOnlineCourse,
                onDelete = onDeleteOnlineCourse
            )
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

// 移除分段中文（上午/下午/晚上）显示，仅保留“时间”+星期+日期
@Composable
private fun HeaderWithSegment(weekDates: List<String>, showWeekend: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧时间列占位（移除“时间”文字）
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(56.dp)
                    .background(
                        Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.width(2.dp))
            // 星期列标题 - 根据设置是否显示周末
            val days = if (showWeekend) TimeTableData.weekDayNames else TimeTableData.weekDayNames.take(5)
            days.forEachIndexed { dayIndex, dayName ->
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
                if (dayIndex < days.size - 1) {
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
private fun GrayCourseContent(course: Course, isMainCell: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 17.sp
            )
            if (course.room.isNotEmpty()) {
                Text(
                    text = course.room,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 15.sp
                )
            }
            if (course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
            if (isMainCell && course.duration > 1) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${'$'}{course.duration}节",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun GrayContinuationCell() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun CourseBottomSheetItem(course: Course) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val colorBox = try {
                Color(android.graphics.Color.parseColor(course.color))
            } catch (_: Exception) {
                MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(colorBox, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        val info = listOfNotNull(
            course.room.takeIf { it.isNotEmpty() },
            course.teacher.takeIf { it.isNotEmpty() },
            course.weeksRange.takeIf { it.isNotEmpty() }
        ).joinToString(" · ")
        if (info.isNotEmpty()) {
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnlineCoursesSection(
    onlineCourses: List<OnlineCourse>,
    currentWeek: Int,
    onAdd: () -> Unit,
    onEdit: (OnlineCourse) -> Unit,
    onDelete: (OnlineCourse) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "网络选修课",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "新增网络选修课")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (onlineCourses.isEmpty()) {
            Text(
                text = "暂无网络选修课",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                onlineCourses.forEach { oc ->
                    val isActiveThisWeek = currentWeek in oc.startWeek..oc.endWeek
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 课程名及本周标记
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = oc.courseName,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActiveThisWeek) {
                                    AssistChip(onClick = {}, label = { Text("本周") })
                                }
                            }
                            // 教师 / 平台
                            val teacherText = oc.teacher.takeIf { it.isNotBlank() } ?: ""
                            val platformText = oc.platform.takeIf { it.isNotBlank() } ?: ""
                            if (teacherText.isNotBlank() || platformText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = listOfNotNull(
                                        teacherText.takeIf { it.isNotBlank() }?.let { "教师：$it" },
                                        platformText.takeIf { it.isNotBlank() }?.let { "平台：$it" }
                                    ).joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // 网址
                            if (oc.url.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "网址：${oc.url}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            // 周次范围
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "周次：${oc.startWeek}-${oc.endWeek}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onEdit(oc) }) { Text("编辑") }
                                OutlinedButton(onClick = { onDelete(oc) }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }
    }
}