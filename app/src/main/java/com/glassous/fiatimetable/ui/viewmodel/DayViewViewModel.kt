package com.glassous.fiatimetable.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData
import com.glassous.fiatimetable.data.repository.TimeTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 日视图ViewModel
 * 管理课程表数据、选中学期、当前周与当前日索引
 */
class DayViewViewModel(private val repository: TimeTableRepository) : ViewModel() {
    private val _timeTableData = MutableStateFlow(TimeTableData())
    val timeTableData: StateFlow<TimeTableData> = _timeTableData.asStateFlow()

    private val _selectedTerm = MutableStateFlow("")
    val selectedTerm: StateFlow<String> = _selectedTerm.asStateFlow()

    // 当前周（1-based）
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    // 当前选中的日索引（0..6，对应周一..周日）
    private val _currentDayIndex = MutableStateFlow(0)
    val currentDayIndex: StateFlow<Int> = _currentDayIndex.asStateFlow()

    // 当前周每天的日期（MM/dd），用于标题展示
    private val _weekDates = MutableStateFlow<List<String>>(emptyList())
    val weekDates: StateFlow<List<String>> = _weekDates.asStateFlow()

    // 是否显示休息分隔线
    private val _showBreaks = MutableStateFlow(true)
    val showBreaks: StateFlow<Boolean> = _showBreaks.asStateFlow()

    // 明日课程预告状态
    private val _showTomorrowPreview = MutableStateFlow(false)
    val showTomorrowPreview: StateFlow<Boolean> = _showTomorrowPreview.asStateFlow()

    private val dateFormatterYMD = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateFormatterMD = DateTimeFormatter.ofPattern("MM/dd")

    init {
        loadData()
    }

    /**
     * 加载数据并设置默认学期、当前周与今天索引
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                val data = repository.getTimeTableData()
                if (data.terms.isEmpty()) {
                    val initial = createDefaultData()
                    _timeTableData.value = initial
                    _selectedTerm.value = initial.selectedTerm
                } else {
                    _timeTableData.value = data
                    val currentTerm = repository.getSelectedTerm()
                    if (currentTerm.isNotEmpty() && data.terms.any { it.name == currentTerm }) {
                        _selectedTerm.value = currentTerm
                    } else {
                        _selectedTerm.value = data.terms.first().name
                        repository.saveSelectedTerm(data.terms.first().name)
                    }
                }
                // 计算当前周与本周日期
                recomputeWeekAndDates()
                // 设置为今天的星期索引
                _currentDayIndex.value = LocalDate.now().dayOfWeek.value.let { it - 1 }.coerceIn(0, 6)
                // 读取是否显示休息分隔线
                _showBreaks.value = repository.getShowBreaks()
            } catch (e: Exception) {
                _timeTableData.value = createDefaultData()
                recomputeWeekAndDates()
                _currentDayIndex.value = LocalDate.now().dayOfWeek.value.let { it - 1 }.coerceIn(0, 6)
                // 读取是否显示休息分隔线
                _showBreaks.value = repository.getShowBreaks()
            }
        }
    }

    /** 选择学期并重新计算周与日期 */
    fun selectTerm(termName: String) {
        viewModelScope.launch {
            _selectedTerm.value = termName
            repository.saveSelectedTerm(termName)
            recomputeWeekAndDates()
        }
    }

    /** 刷新数据 */
    fun refreshData() {
        viewModelScope.launch {
            try {
                val prevSelectedTerm = _selectedTerm.value
                val prevWeek = _currentWeek.value
                val prevDay = _currentDayIndex.value

                val data = repository.getTimeTableData()
                // 若数据为空则使用默认数据，但不重置当前位置
                val newData = if (data.terms.isEmpty()) createDefaultData() else data
                _timeTableData.value = newData

                // 选中学期：优先使用存储的设置；否则保留原选中；再否则取首个
                val repoTerm = repository.getSelectedTerm()
                val newSelected = when {
                    repoTerm.isNotEmpty() && newData.terms.any { it.name == repoTerm } -> repoTerm
                    newData.terms.any { it.name == prevSelectedTerm } -> prevSelectedTerm
                    newData.terms.isNotEmpty() -> newData.terms.first().name
                    else -> ""
                }
                _selectedTerm.value = newSelected
                if (newSelected.isNotEmpty()) repository.saveSelectedTerm(newSelected)

                // 保留周/日位置，并在新学期范围内进行约束
                val term = newData.terms.find { it.name == newSelected }
                val boundedWeek = term?.let { prevWeek.coerceIn(1, it.weeks) } ?: prevWeek
                _currentWeek.value = boundedWeek
                _currentDayIndex.value = prevDay.coerceIn(0, 6)

                // 仅根据当前周刷新本周日期，避免回到"本周"
                recomputeWeekDatesOnly()

                // 刷新显示设置
                _showBreaks.value = repository.getShowBreaks()
                
                // 检查是否需要智能跳转到明日
                checkAndAutoJumpToTomorrow()
            } catch (e: Exception) {
                // 设置项仍尽可能刷新，位置保持不变
                _showBreaks.value = repository.getShowBreaks()
            }
        }
    }

    /**
     * 检查当天课程状态，决定是否自动跳转到明日
     */
    private fun checkAndAutoJumpToTomorrow() {
        val today = LocalDate.now()
        val todayDayOfWeek = today.dayOfWeek.value - 1 // 0-6对应周一到周日
        
        // 只有当前显示的是今天时才进行检查
        if (_currentDayIndex.value == todayDayOfWeek) {
            val shouldJumpToTomorrow = shouldAutoJumpToTomorrow()
            if (shouldJumpToTomorrow) {
                jumpToTomorrow()
            }
        }
    }

    /**
     * 判断是否应该自动跳转到明日
     * 条件：1. 当天没有课程 或 2. 当天所有课程已结束
     */
    private fun shouldAutoJumpToTomorrow(): Boolean {
        val currentTime = LocalTime.now()
        val todayDayOfWeek = LocalDate.now().dayOfWeek.value - 1
        val todayCourses = getTodayCourses(todayDayOfWeek)
        
        // 如果今天没有课程，返回true
        if (todayCourses.isEmpty()) {
            return true
        }
        
        // 检查今天所有课程是否都已结束
        val timeSlots = _timeTableData.value.timeSlots
        return todayCourses.all { (slotIndex, _) ->
            val slotTime = timeSlots.getOrNull(slotIndex) ?: return@all false
            val endTime = parseEndTime(slotTime)
            endTime?.isBefore(currentTime) ?: false
        }
    }

    /**
     * 获取今天的课程列表
     */
    private fun getTodayCourses(dayIndex: Int): List<Pair<Int, Course>> {
        val courses = _timeTableData.value.courses[_selectedTerm.value] ?: return emptyList()
        val dayCourses = courses[dayIndex] ?: return emptyList()
        val result = mutableListOf<Pair<Int, Course>>()
        
        dayCourses.forEach { (slotIndex, data) ->
            when (data) {
                is List<*> -> {
                    val filtered = data.filterIsInstance<Course>()
                        .filter { it.selectedWeeks.contains(_currentWeek.value) }
                    filtered.forEach { course ->
                        result.add(slotIndex to course)
                    }
                }
            }
        }
        
        return result
    }

    /**
     * 解析时间段的结束时间
     */
    private fun parseEndTime(timeSlot: String): LocalTime? {
        return try {
            val endTimeStr = timeSlot.split("-").getOrNull(1) ?: return null
            val parts = endTimeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
            LocalTime.of(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 跳转到明日并设置预告状态
     */
    private fun jumpToTomorrow() {
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value - 1
        
        // 计算明日所在的周
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value } ?: return
        val tomorrowWeek = calculateWeekForDate(tomorrow, term)
        
        if (tomorrowWeek != null && tomorrowWeek <= term.weeks) {
            _currentWeek.value = tomorrowWeek
            _currentDayIndex.value = tomorrowDayOfWeek
            _showTomorrowPreview.value = true
            recomputeWeekDatesOnly()
        }
    }

    /**
     * 计算指定日期所在的学期周数
     */
    private fun calculateWeekForDate(date: LocalDate, term: com.glassous.fiatimetable.data.model.Term): Int? {
        return try {
            val startDate = LocalDate.parse(term.startDate, dateFormatterYMD)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, date).toInt()
            if (daysBetween >= 0) (daysBetween / 7) + 1 else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否应该显示明日预告横幅
     * 仅在显示次日页面时显示
     */
    fun shouldShowTomorrowPreview(pageWeek: Int, pageDayIndex: Int): Boolean {
        val today = LocalDate.now()
        val todayDayOfWeek = today.dayOfWeek.value - 1
        
        // 计算今天所在的学期周数
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value } ?: return false
        val todayWeek = calculateWeekForDate(today, term) ?: return false
        
        // 计算明天的日期和周数
        val tomorrow = today.plusDays(1)
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value - 1
        val tomorrowWeek = calculateWeekForDate(tomorrow, term) ?: return false
        
        // 检查当前页面是否是明日页面
        return pageWeek == tomorrowWeek && pageDayIndex == tomorrowDayOfWeek
    }

    /**
     * 手动返回今天
     */
    fun backToTodayManual() {
        backToToday()
    }

    /** 切换到前一天（循环） */
    fun prevDay() {
        val prevIndex = _currentDayIndex.value
        val newIndex = (prevIndex + 6) % 7
        _currentDayIndex.value = newIndex
        if (prevIndex == 0 && newIndex == 6) {
            val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
            if (term != null && _currentWeek.value > 1) {
                _currentWeek.value = (_currentWeek.value - 1).coerceAtLeast(1)
                recomputeWeekDatesOnly()
            }
        }
    }

    /** 切换到后一天（循环） */
    fun nextDay() {
        val prevIndex = _currentDayIndex.value
        val newIndex = (prevIndex + 1) % 7
        _currentDayIndex.value = newIndex
        if (prevIndex == 6 && newIndex == 0) {
            val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
            if (term != null && _currentWeek.value < term.weeks) {
                _currentWeek.value = (_currentWeek.value + 1).coerceAtMost(term.weeks)
                recomputeWeekDatesOnly()
            }
        }
    }

    /** 回到今天（根据系统日期） */
    fun backToToday() {
        _currentDayIndex.value = LocalDate.now().dayOfWeek.value.let { it - 1 }.coerceIn(0, 6)
        // 同时刷新当前周（避免跨周时标题日期不准确）
        recomputeWeekAndDates()
    }

    /** 是否处于今天 */
    fun isAtToday(): Boolean {
        val todayIndex = LocalDate.now().dayOfWeek.value.let { it - 1 }.coerceIn(0, 6)
        return _currentDayIndex.value == todayIndex
    }

    /** 根据选中学期和当前日期计算当前周，并刷新周内日期 */
    private fun recomputeWeekAndDates() {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value } ?: return
        runCatching {
            val start = LocalDate.parse(term.startDate, dateFormatterYMD)
            val today = LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
            val computed = if (days >= 0) (days / 7) + 1 else 1
            val bounded = computed.coerceIn(1, term.weeks)
            _currentWeek.value = bounded
            recomputeWeekDatesOnly()
        }.onFailure {
            _currentWeek.value = 1
            recomputeWeekDatesOnly()
        }
    }

    /** 仅基于当前周更新本周每天的日期（MM/dd） */
    private fun recomputeWeekDatesOnly() {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value } ?: run {
            _weekDates.value = emptyList(); return
        }
        runCatching {
            val start = LocalDate.parse(term.startDate, dateFormatterYMD)
            val startOfWeek = start.plusWeeks((_currentWeek.value - 1).toLong())
            val monday = startOfWeek // 假设学期开始日为周一
            val days = (0..6).map { monday.plusDays(it.toLong()).format(dateFormatterMD) }
            _weekDates.value = days
        }.onFailure {
            _weekDates.value = emptyList()
        }
    }

    /**
     * 返回当前周该格子的课程或延续标记（与 WeekView 的过滤逻辑一致）
     */
    fun cellForWeek(dayIndex: Int, slotIndex: Int): Any? {
        val courses = _timeTableData.value.courses[_selectedTerm.value] ?: return null
        val dayCourses = courses[dayIndex] ?: return null
        val data = dayCourses[slotIndex] ?: return null
        return when (data) {
            is List<*> -> {
                val filtered = data.filterIsInstance<Course>().filter { it.selectedWeeks.contains(_currentWeek.value) }
                if (filtered.isNotEmpty()) filtered else null
            }
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = dayCourses[from]
                    if (origin is List<*>) {
                        val filtered = origin.filterIsInstance<Course>().filter { it.selectedWeeks.contains(_currentWeek.value) }
                        if (filtered.isNotEmpty()) data else null
                    } else null
                } else null
            }
            else -> null
        }
    }

    // 按指定周数返回格子内容（供按周索引渲染使用）
    fun cellForWeekIndex(dayIndex: Int, slotIndex: Int, week: Int): Any? {
        val courses = _timeTableData.value.courses[_selectedTerm.value] ?: return null
        val dayCourses = courses[dayIndex] ?: return null
        val data = dayCourses[slotIndex] ?: return null
        return when (data) {
            is List<*> -> {
                val filtered = data.filterIsInstance<Course>().filter { it.selectedWeeks.contains(week) }
                if (filtered.isNotEmpty()) filtered else null
            }
            is Map<*, *> -> {
                if (data["continued"] == true) {
                    val from = (data["fromSlot"] as? Number)?.toInt() ?: slotIndex
                    val origin = dayCourses[from]
                    if (origin is List<*>) {
                        val filtered = origin.filterIsInstance<Course>().filter { it.selectedWeeks.contains(week) }
                        if (filtered.isNotEmpty()) data else null
                    } else null
                } else null
            }
            else -> null
        }
    }

    // 新增：同步周与日索引，用于 Pager 改变页面时更新状态
    fun setWeekAndDay(week: Int, dayIndex: Int) {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
        val boundedWeek = if (term != null) week.coerceIn(1, term.weeks) else week.coerceAtLeast(1)
        val boundedDay = dayIndex.coerceIn(0, 6)
        val weekChanged = boundedWeek != _currentWeek.value
        _currentWeek.value = boundedWeek
        _currentDayIndex.value = boundedDay
        if (weekChanged) {
            recomputeWeekDatesOnly()
        }
    }

    /** 创建默认数据用于演示 */
    private fun createDefaultData(): TimeTableData {
        return TimeTableData(
            terms = listOf(
                com.glassous.fiatimetable.data.model.Term(
                    name = "2024-2025学年第一学期",
                    weeks = 18,
                    startDate = "2024-09-01"
                ),
                com.glassous.fiatimetable.data.model.Term(
                    name = "2024-2025学年第二学期",
                    weeks = 18,
                    startDate = "2025-02-24"
                )
            ),
            courses = mapOf(
                "2024-2025学年第一学期" to mapOf(
                    0 to mapOf(
                        0 to listOf(
                            com.glassous.fiatimetable.data.model.Course(
                                courseName = "法理学",
                                courseType = "必修",
                                duration = 2,
                                weeksRange = "1-16",
                                selectedWeeks = (1..16).toList(),
                                color = "#FF5722",
                                teacher = "张教授",
                                room = "A101",
                                notes = ""
                            )
                        ),
                        1 to mapOf("continued" to true, "fromSlot" to 0)
                    ),
                    1 to mapOf(
                        2 to listOf(
                            com.glassous.fiatimetable.data.model.Course(
                                courseName = "宪法学",
                                courseType = "必修",
                                duration = 2,
                                weeksRange = "1-16",
                                selectedWeeks = (1..16).toList(),
                                color = "#2196F3",
                                teacher = "李教授",
                                room = "B202",
                                notes = ""
                            )
                        ),
                        3 to mapOf("continued" to true, "fromSlot" to 2)
                    )
                )
            ),
            onlineCourses = mapOf(),
            timeSlots = TimeTableData.defaultTimeSlots,
            selectedTerm = "2024-2025学年第一学期",
            theme = "system"
        )
    }
}

/** DayViewViewModel 工厂类 */
class DayViewViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayViewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayViewViewModel(TimeTableRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}