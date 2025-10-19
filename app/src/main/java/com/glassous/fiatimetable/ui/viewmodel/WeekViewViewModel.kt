package com.glassous.fiatimetable.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.glassous.fiatimetable.data.model.TimeTableData
import com.glassous.fiatimetable.data.repository.TimeTableRepository
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.OnlineCourse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 周视图ViewModel
 * 管理课程表数据和选中的学期状态
 */
class WeekViewViewModel(private val repository: TimeTableRepository) : ViewModel() {
    
    private val _timeTableData = MutableStateFlow(TimeTableData())
    val timeTableData: StateFlow<TimeTableData> = _timeTableData.asStateFlow()
    
    private val _selectedTerm = MutableStateFlow("")
    val selectedTerm: StateFlow<String> = _selectedTerm.asStateFlow()

    // 当前周（1-based）
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    // 当前周的每天日期（mm/dd）
    private val _weekDates = MutableStateFlow<List<String>>(emptyList())
    val weekDates: StateFlow<List<String>> = _weekDates.asStateFlow()

    private val dateFormatterYMD = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateFormatterMD = DateTimeFormatter.ofPattern("MM/dd")

    init {
        loadData()
    }
    
    /**
     * 加载课程表数据
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                val data = repository.getTimeTableData()
                // 如果学期列表为空（例如系统清除数据后），进行安全初始化
                if (data.terms.isEmpty()) {
                    val now = java.time.LocalDate.now()
                    val month = now.monthValue
                    val (name, startDate) = if (month >= 9) {
                        val startYear = now.year
                        val endYear = now.year + 1
                        "${startYear}-${endYear} 秋季" to now.withMonth(9).withDayOfMonth(1).format(dateFormatterYMD)
                    } else {
                        val startYear = now.year - 1
                        val endYear = now.year
                        val feb = java.time.LocalDate.of(endYear, 2, 1)
                        val start = feb.withDayOfMonth(feb.lengthOfMonth())
                        "${startYear}-${endYear} 春季" to start.format(dateFormatterYMD)
                    }
                    val initial = com.glassous.fiatimetable.data.model.Term(name = name, weeks = 16, startDate = startDate)
                    // 写入存储并更新内存状态
                    repository.saveTerms(listOf(initial))
                    repository.saveSelectedTerm(initial.name)
                    _timeTableData.value = data.copy(terms = listOf(initial), selectedTerm = initial.name)
                    _selectedTerm.value = initial.name
                } else {
                    _timeTableData.value = data
                    // 设置默认选中的学期
                    val currentTerm = repository.getSelectedTerm()
                    if (currentTerm.isNotEmpty() && data.terms.any { it.name == currentTerm }) {
                        _selectedTerm.value = currentTerm
                    } else if (data.terms.isNotEmpty()) {
                        _selectedTerm.value = data.terms.first().name
                        repository.saveSelectedTerm(data.terms.first().name)
                    }
                }

                // 选定学期后计算当前周与日期
                recomputeWeekAndDates()
            } catch (e: Exception) {
                // 如果加载失败，使用默认数据
                _timeTableData.value = createDefaultData()
                // 也尝试计算周次
                recomputeWeekAndDates()
            }
        }
    }
    
    /**
     * 选择学期
     */
    fun selectTerm(termName: String) {
        viewModelScope.launch {
            _selectedTerm.value = termName
            repository.saveSelectedTerm(termName)
            recomputeWeekAndDates()
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadData()
    }

    /**
     * 切换到上一周
     */
    fun prevWeek() {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
        val minWeek = 1
        val maxWeek = term?.weeks ?: 1
        val newWeek = (_currentWeek.value - 1).coerceIn(minWeek, maxWeek)
        if (newWeek != _currentWeek.value) {
            _currentWeek.value = newWeek
            recomputeWeekDatesOnly()
        }
    }

    /**
     * 回到本周（根据当前日期与选中学期起始日计算）
     */
    fun backToCurrentWeek() {
        recomputeWeekAndDates()
    }

    /**
     * 判断当前视图是否处于“本周”
     */
    fun isAtCurrentWeek(): Boolean {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value } ?: return false
        return runCatching {
            val start = LocalDate.parse(term.startDate, dateFormatterYMD)
            val today = LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt()
            val computed = if (days >= 0) (days / 7) + 1 else 1
            val bounded = computed.coerceIn(1, term.weeks)
            _currentWeek.value == bounded
        }.getOrDefault(false)
    }

    /**
     * 切换到下一周
     */
    fun nextWeek() {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
        val minWeek = 1
        val maxWeek = term?.weeks ?: 1
        val newWeek = (_currentWeek.value + 1).coerceIn(minWeek, maxWeek)
        if (newWeek != _currentWeek.value) {
            _currentWeek.value = newWeek
            recomputeWeekDatesOnly()
        }
    }

    /**
     * 设置到指定周（1-based）
     */
    fun setWeek(week: Int) {
        val term = _timeTableData.value.terms.find { it.name == _selectedTerm.value }
        val minWeek = 1
        val maxWeek = term?.weeks ?: 1
        val newWeek = week.coerceIn(minWeek, maxWeek)
        if (newWeek != _currentWeek.value) {
            _currentWeek.value = newWeek
            recomputeWeekDatesOnly()
        }
    }

    /**
     * 根据选中学期和当前日期计算当前周，并刷新周内日期
     */
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
            // 解析失败则保持周1并计算日期
            _currentWeek.value = 1
            recomputeWeekDatesOnly()
        }
    }

    /**
     * 仅基于当前周更新本周每天的日期（mm/dd）
     */
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
     * 保存或更新课程
     */
    fun saveCourse(course: Course, dayIndex: Int, timeSlotIndex: Int) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value
                
                // 创建新的课程数据结构
                val updatedCourses = currentData.courses.toMutableMap()
                val termCourses = updatedCourses[currentTerm]?.toMutableMap() ?: mutableMapOf()
                val dayCourses = termCourses[dayIndex]?.toMutableMap() ?: mutableMapOf()
                
                // 先清理旧占用（包括多节的延续标记）
                val existing = dayCourses[timeSlotIndex]
                var slotsToClear: List<Int> = emptyList()
                when (existing) {
                    is List<*> -> {
                        val prevCourse = existing.filterIsInstance<Course>().firstOrNull()
                        val prevDuration = prevCourse?.duration ?: run {
                            val firstMap = existing.firstOrNull() as? Map<*, *>
                            (firstMap?.get("duration") as? Number)?.toInt() ?: 1
                        }
                        slotsToClear = (0 until prevDuration).map { timeSlotIndex + it }
                    }
                    is Map<*, *> -> {
                        if (existing["continued"] == true) {
                            val from = (existing["fromSlot"] as? Number)?.toInt() ?: timeSlotIndex
                            val origin = dayCourses[from]
                            val originDuration = when (origin) {
                                is List<*> -> {
                                    val c = origin.filterIsInstance<Course>().firstOrNull()
                                    c?.duration ?: run {
                                        val fm = origin.firstOrNull() as? Map<*, *>
                                        (fm?.get("duration") as? Number)?.toInt() ?: 1
                                    }
                                }
                                else -> 1
                            }
                            slotsToClear = (0 until originDuration).map { from + it }
                        } else {
                            slotsToClear = listOf(timeSlotIndex)
                        }
                    }
                }
                slotsToClear.forEach { dayCourses.remove(it) }
                
                // 如果课程占用多节，需要处理跨格显示
                if (course.duration > 1) {
                    for (i in 0 until course.duration) {
                        val targetSlot = timeSlotIndex + i
                        if (targetSlot < currentData.timeSlots.size) {
                            if (i == 0) {
                                // 第一节课存储完整课程信息
                                dayCourses[targetSlot] = listOf(course)
                            } else {
                                // 后续节课标记为延续，包含课程颜色和原始位置信息
                                dayCourses[targetSlot] = mapOf(
                                    "continued" to true,
                                    "color" to course.color,
                                    "fromSlot" to timeSlotIndex,
                                    "courseName" to course.courseName
                                )
                            }
                        }
                    }
                } else {
                    // 单节课程
                    dayCourses[timeSlotIndex] = listOf(course)
                }
                
                // 更新数据结构
                termCourses[dayIndex] = dayCourses
                updatedCourses[currentTerm] = termCourses
                
                val updatedData = currentData.copy(courses = updatedCourses)
                _timeTableData.value = updatedData
                
                // 保存到存储库
                repository.saveTimeTableData(updatedData)
                
            } catch (e: Exception) {
                // 处理保存错误
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 删除课程（删除整个格子的内容）
     */
    fun deleteCourse(dayIndex: Int, timeSlotIndex: Int) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value
                
                val updatedCourses = currentData.courses.toMutableMap()
                val termCourses = updatedCourses[currentTerm]?.toMutableMap() ?: mutableMapOf()
                val dayCourses = termCourses[dayIndex]?.toMutableMap() ?: mutableMapOf()
                
                // 获取要删除的课程信息
                val courseToDelete = dayCourses[timeSlotIndex]
                if (courseToDelete is List<*>) {
                    val course = courseToDelete.filterIsInstance<Course>().firstOrNull()
                    if (course != null && course.duration > 1) {
                        // 删除多节课程的所有相关格子
                        for (i in 0 until course.duration) {
                            val targetSlot = timeSlotIndex + i
                            dayCourses.remove(targetSlot)
                        }
                    } else {
                        // 删除单节课程
                        dayCourses.remove(timeSlotIndex)
                    }
                } else {
                    // 直接删除
                    dayCourses.remove(timeSlotIndex)
                }
                
                // 更新数据结构
                termCourses[dayIndex] = dayCourses
                updatedCourses[currentTerm] = termCourses
                
                val updatedData = currentData.copy(courses = updatedCourses)
                _timeTableData.value = updatedData
                
                // 保存到存储库
                repository.saveTimeTableData(updatedData)
                
            } catch (e: Exception) {
                // 处理删除错误
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 新增课程：同一时间段不同周追加，不覆盖原有
     */
    fun addCourseInSameSlot(course: Course, dayIndex: Int, timeSlotIndex: Int) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value

                val updatedCourses = currentData.courses.toMutableMap()
                val termCourses = updatedCourses[currentTerm]?.toMutableMap() ?: mutableMapOf()
                val dayCourses = termCourses[dayIndex]?.toMutableMap() ?: mutableMapOf()

                // 在起始格追加课程
                val existing = dayCourses[timeSlotIndex]
                val newList = when (existing) {
                    is List<*> -> {
                        val typed = existing.filterIsInstance<Course>().toMutableList()
                        typed.add(course)
                        typed.toList()
                    }
                    else -> listOf(course)
                }
                dayCourses[timeSlotIndex] = newList

                // 处理延续格：仅设置标准延续标记（严格遵守 DATA_STRUCTURE.md）
                if (course.duration > 1) {
                    for (i in 1 until course.duration) {
                        val targetSlot = timeSlotIndex + i
                        if (targetSlot < currentData.timeSlots.size) {
                            dayCourses[targetSlot] = mapOf(
                                "continued" to true,
                                "fromSlot" to timeSlotIndex
                            )
                        }
                    }
                }

                termCourses[dayIndex] = dayCourses
                updatedCourses[currentTerm] = termCourses

                val updatedData = currentData.copy(courses = updatedCourses)
                _timeTableData.value = updatedData
                repository.saveTimeTableData(updatedData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 删除指定课程：仅移除给定课程，并根据剩余课程重新调整延续标记
     */
    fun deleteSpecificCourse(target: Course, dayIndex: Int, timeSlotIndex: Int) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value

                val updatedCourses = currentData.courses.toMutableMap()
                val termCourses = updatedCourses[currentTerm]?.toMutableMap() ?: mutableMapOf()
                val dayCourses = termCourses[dayIndex]?.toMutableMap() ?: mutableMapOf()

                val origin = dayCourses[timeSlotIndex]
                val remaining: List<Course> = when (origin) {
                    is List<*> -> origin.filterIsInstance<Course>().filterNot { it == target }
                    is Map<*, *> -> {
                        // 若点击的是延续格，定位到起始格
                        val from = (origin["fromSlot"] as? Number)?.toInt() ?: timeSlotIndex
                        val o = dayCourses[from]
                        val list = if (o is List<*>) o.filterIsInstance<Course>() else emptyList()
                        val filtered = list.filterNot { it == target }
                        // 清理旧延续标记（以原始最大duration为准）
                        val maxPrevDuration = list.maxOfOrNull { it.duration } ?: 1
                        for (i in 1 until maxPrevDuration) {
                            val s = from + i
                            dayCourses.remove(s)
                        }
                        // 写回起始格剩余课程（为空则移除键）
                        if (filtered.isEmpty()) {
                            dayCourses.remove(from)
                        } else {
                            dayCourses[from] = filtered
                        }
                        // 根据剩余课程的最大duration重建延续标记
                        val maxNewDuration = filtered.maxOfOrNull { it.duration } ?: 1
                        if (maxNewDuration > 1) {
                            for (i in 1 until maxNewDuration) {
                                val s = from + i
                                if (s < currentData.timeSlots.size) {
                                    dayCourses[s] = mapOf(
                                        "continued" to true,
                                        "fromSlot" to from
                                    )
                                }
                            }
                        }
                        termCourses[dayIndex] = dayCourses
                        updatedCourses[currentTerm] = termCourses
                        val updatedData = currentData.copy(courses = updatedCourses)
                        _timeTableData.value = updatedData
                        repository.saveTimeTableData(updatedData)
                        return@launch
                    }
                    else -> emptyList()
                }

                // 非延续格：直接处理起始格
                // 先清理旧延续标记（根据删除前的最大duration）
                val maxPrevDuration = (origin as? List<*>)?.filterIsInstance<Course>()?.maxOfOrNull { it.duration } ?: 1
                for (i in 1 until maxPrevDuration) {
                    val s = timeSlotIndex + i
                    dayCourses.remove(s)
                }
                // 写回起始格
                if (remaining.isEmpty()) {
                    dayCourses.remove(timeSlotIndex)
                } else {
                    dayCourses[timeSlotIndex] = remaining
                }
                // 重建延续标记（根据剩余课程最大duration）
                val maxNewDuration = remaining.maxOfOrNull { it.duration } ?: 1
                if (maxNewDuration > 1) {
                    for (i in 1 until maxNewDuration) {
                        val s = timeSlotIndex + i
                        if (s < currentData.timeSlots.size) {
                            dayCourses[s] = mapOf(
                                "continued" to true,
                                "fromSlot" to timeSlotIndex
                            )
                        }
                    }
                }

                termCourses[dayIndex] = dayCourses
                updatedCourses[currentTerm] = termCourses
                val updatedData = currentData.copy(courses = updatedCourses)
                _timeTableData.value = updatedData
                repository.saveTimeTableData(updatedData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 创建默认数据用于演示
     */
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
                    // 周一
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
                    // 周二
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
                    ),
                    // 周三
                    2 to mapOf(
                        4 to listOf(
                            com.glassous.fiatimetable.data.model.Course(
                                courseName = "民法学",
                                courseType = "必修",
                                duration = 3,
                                weeksRange = "1-16",
                                selectedWeeks = (1..16).toList(),
                                color = "#4CAF50",
                                teacher = "王教授",
                                room = "C303",
                                notes = ""
                            )
                        ),
                        5 to mapOf("continued" to true, "fromSlot" to 4),
                        6 to mapOf("continued" to true, "fromSlot" to 4)
                    )
                )
            ),
            onlineCourses = mapOf(),
            timeSlots = TimeTableData.defaultTimeSlots,
            selectedTerm = "2024-2025学年第一学期",
            theme = "system"
        )
    }

    // 在线选修课：新增
    fun addOnlineCourse(course: OnlineCourse) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value
                val updatedOnline = currentData.onlineCourses.toMutableMap()
                val termList = updatedOnline[currentTerm]?.toMutableList() ?: mutableListOf()
                // 保证ID唯一：若冲突则替换，否则追加
                val idx = termList.indexOfFirst { it.id == course.id }
                if (idx >= 0) termList[idx] = course else termList.add(course)
                updatedOnline[currentTerm] = termList
                val updatedData = currentData.copy(onlineCourses = updatedOnline)
                _timeTableData.value = updatedData
                repository.saveTimeTableData(updatedData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 在线选修课：更新
    fun updateOnlineCourse(course: OnlineCourse) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value
                val updatedOnline = currentData.onlineCourses.toMutableMap()
                val termList = updatedOnline[currentTerm]?.toMutableList() ?: mutableListOf()
                val idx = termList.indexOfFirst { it.id == course.id }
                if (idx >= 0) {
                    termList[idx] = course
                } else {
                    termList.add(course)
                }
                updatedOnline[currentTerm] = termList
                val updatedData = currentData.copy(onlineCourses = updatedOnline)
                _timeTableData.value = updatedData
                repository.saveTimeTableData(updatedData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 在线选修课：删除
    fun deleteOnlineCourse(id: String) {
        viewModelScope.launch {
            try {
                val currentData = _timeTableData.value
                val currentTerm = _selectedTerm.value
                val updatedOnline = currentData.onlineCourses.toMutableMap()
                val termList = updatedOnline[currentTerm]?.toMutableList() ?: mutableListOf()
                val filtered = termList.filter { it.id != id }
                updatedOnline[currentTerm] = filtered
                val updatedData = currentData.copy(onlineCourses = updatedOnline)
                _timeTableData.value = updatedData
                repository.saveTimeTableData(updatedData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * WeekViewViewModel工厂类
 */
class WeekViewViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeekViewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeekViewViewModel(TimeTableRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}