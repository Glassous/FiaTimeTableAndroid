package com.glassous.fiatimetable.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.glassous.fiatimetable.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonParser

/**
 * 课程表数据仓库
 * 负责管理课程表数据的存储和访问
 * 使用 SharedPreferences 模拟 localStorage 存储
 */
class TimeTableRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("timetable_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        // 存储键，严格遵守 DATA_STRUCTURE.md 中的定义
        private const val KEY_TERMS = "xf_terms"
        private const val KEY_COURSES = "xf_courses"
        private const val KEY_ONLINE_COURSES = "xf_onlineCourses"
        private const val KEY_TIME_SLOTS = "xf_timeSlots"
        private const val KEY_SELECTED_TERM = "xf_term"
        private const val KEY_THEME = "xf_theme"
        private const val KEY_START_PAGE = "xf_start_page"
    }
    
    // 获取学期列表
    fun getTerms(): List<Term> {
        val json = sharedPreferences.getString(KEY_TERMS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Term>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    
    // 保存学期列表
    fun saveTerms(terms: List<Term>) {
        val json = gson.toJson(terms)
        sharedPreferences.edit().putString(KEY_TERMS, json).apply()
    }
    
    // 获取课程数据
    fun getCourses(): Map<String, Map<Int, Map<Int, Any>>> {
        val json = sharedPreferences.getString(KEY_COURSES, null)
        // 反序列化后进行类型归一化：将 List<LinkedTreeMap> 转为 List<Course>
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<Map<String, Map<Int, Map<Int, Any>>>>() {}.type
            val raw: Map<String, Map<Int, Map<Int, Any>>> = gson.fromJson(json, type)
            val normalized = mutableMapOf<String, MutableMap<Int, MutableMap<Int, Any>>>()
            raw.forEach { (term, days) ->
                val daysMap = mutableMapOf<Int, MutableMap<Int, Any>>()
                days.forEach { (dayIndex, slots) ->
                    val slotsMap = mutableMapOf<Int, Any>()
                    slots.forEach { (slotIndex, value) ->
                        when (value) {
                            is List<*> -> {
                                val typedCourses = value.mapNotNull { elem ->
                                    when (elem) {
                                        is Course -> elem
                                        is Map<*, *> -> {
                                            try {
                                                val elemJson = gson.toJson(elem)
                                                gson.fromJson(elemJson, Course::class.java)
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                        else -> null
                                    }
                                }
                                slotsMap[slotIndex] = typedCourses
                            }
                            else -> {
                                // 延续标记或其他结构，保持原样
                                slotsMap[slotIndex] = value
                            }
                        }
                    }
                    daysMap[dayIndex] = slotsMap
                }
                normalized[term] = daysMap
            }
            normalized
        } else {
            emptyMap()
        }
    }
    
    // 保存课程数据
    fun saveCourses(courses: Map<String, Map<Int, Map<Int, Any>>>) {
        val json = gson.toJson(courses)
        sharedPreferences.edit().putString(KEY_COURSES, json).apply()
    }
    
    // 获取网络选修课数据
    fun getOnlineCourses(): Map<String, List<OnlineCourse>> {
        val json = sharedPreferences.getString(KEY_ONLINE_COURSES, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, List<OnlineCourse>>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }
    
    // 保存网络选修课数据
    fun saveOnlineCourses(onlineCourses: Map<String, List<OnlineCourse>>) {
        val json = gson.toJson(onlineCourses)
        sharedPreferences.edit().putString(KEY_ONLINE_COURSES, json).apply()
    }
    
    // 获取时间段设置
    fun getTimeSlots(): List<String> {
        val json = sharedPreferences.getString(KEY_TIME_SLOTS, null)
        return if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } else {
            TimeTableData.defaultTimeSlots
        }
    }
    
    // 保存时间段设置
    fun saveTimeSlots(timeSlots: List<String>) {
        val json = gson.toJson(timeSlots)
        sharedPreferences.edit().putString(KEY_TIME_SLOTS, json).apply()
    }
    
    // 获取当前选中学期
    fun getSelectedTerm(): String {
        return sharedPreferences.getString(KEY_SELECTED_TERM, "") ?: ""
    }
    
    // 保存当前选中学期
    fun saveSelectedTerm(termName: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_TERM, termName).apply()
    }
    
    // 获取主题设置
    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, "system") ?: "system"
    }
    
    // 保存主题设置
    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
    }

    // 启动页面设置：day / week
    fun getStartPage(): String {
        return sharedPreferences.getString(KEY_START_PAGE, "week") ?: "week"
    }

    fun saveStartPage(page: String) {
        sharedPreferences.edit().putString(KEY_START_PAGE, page).apply()
    }

    // 监听主题变更（供 Compose 层注册/注销）
    fun registerOnThemeChangedListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }
    fun unregisterOnThemeChangedListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
    
    // 获取完整的课程表数据
    fun getTimeTableData(): TimeTableData {
        return TimeTableData(
            terms = getTerms(),
            courses = getCourses(),
            onlineCourses = getOnlineCourses(),
            timeSlots = getTimeSlots(),
            selectedTerm = getSelectedTerm(),
            theme = getTheme()
        )
    }
    
    // 保存完整的课程表数据
    fun saveTimeTableData(data: TimeTableData) {
        saveTerms(data.terms)
        saveCourses(data.courses)
        saveOnlineCourses(data.onlineCourses)
        saveTimeSlots(data.timeSlots)
        saveSelectedTerm(data.selectedTerm)
        saveTheme(data.theme)
    }
    
    // 清空所有数据
    fun clearAllData() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}

// 备份导出时间段结构
data class BackupTimeSlot(val start: String, val end: String, val period: String)

// 导出为备份 JSON（完全兼容示例文件）
fun exportBackupJson(repo: TimeTableRepository): String {
    val gson = Gson()
    val data = repo.getTimeTableData()
    val exportDate = java.time.Instant.now().toString()

    // 转换 timeSlots 为对象数组
    fun classifyPeriod(start: String): String {
        val hour = start.substringBefore(":").toIntOrNull() ?: 0
        return when {
            hour < 12 -> "上午"
            hour < 18 -> "下午"
            else -> "晚上"
        }
    }
    val timeSlotsObj = data.timeSlots.map { slot ->
        val normalized = slot.replace("~", "-")
        val start = normalized.substringBefore("-")
        val end = normalized.substringAfter("-")
        BackupTimeSlot(start = start, end = end, period = classifyPeriod(start))
    }

    // 转换 courses：每个节次导出为一个课程对象（忽略 continued 标记）
    val coursesExport = mutableMapOf<String, MutableMap<Int, MutableMap<Int, Map<String, Any>>>>()
    data.courses.forEach { (term, days) ->
        val dayMap = mutableMapOf<Int, MutableMap<Int, Map<String, Any>>>()
        days.forEach { (dayIndex, slots) ->
            val slotMap = mutableMapOf<Int, Map<String, Any>>()
            slots.forEach { (slotIndex, value) ->
                when (value) {
                    is List<*> -> {
                        val first = value.firstOrNull()
                        if (first is Course) {
                            val courseObj = mapOf(
                                "courseName" to first.courseName,
                                "courseType" to first.courseType,
                                "duration" to first.duration,
                                "weeksRange" to first.weeksRange,
                                "selectedWeeks" to first.selectedWeeks,
                                "color" to first.color,
                                // 教学信息
                                "teacher" to first.teacher,
                                "room" to first.room,
                                "notes" to first.notes,
                                // 详细信息
                                "courseCode" to first.courseCode,
                                "shortName" to first.shortName,
                                "courseNameEn" to first.courseNameEn,
                                "credits" to first.credits,
                                "department" to first.department,
                                "teachingGroup" to first.teachingGroup,
                                "leader" to first.leader,
                                "courseAttr" to first.courseAttr,
                                "assessType" to first.assessType,
                                "examType" to first.examType,
                                "classroomType" to first.classroomType,
                                "totalHours" to first.totalHours,
                                "referenceBooks" to first.referenceBooks,
                                "capacity" to first.capacity,
                                "enrolled" to first.enrolled,
                                // 冗余位置信息，兼容备份模板
                                "day" to (dayIndex + 1),
                                "slotIndex" to slotIndex
                            )
                            slotMap[slotIndex] = courseObj
                        }
                    }
                    // 其他（continued）不导出
                }
            }
            dayMap[dayIndex] = slotMap
        }
        coursesExport[term] = dayMap
    }

    // 构造备份数据结构
    val backupRoot = mapOf(
        "version" to "1.0",
        "exportDate" to exportDate,
        "data" to mapOf(
            "terms" to data.terms,
            "courses" to coursesExport,
            "timeSlots" to timeSlotsObj,
            "selectedTerm" to data.selectedTerm,
            "theme" to data.theme,
            "onlineCourses" to data.onlineCourses
        )
    )
    return gson.toJson(backupRoot)
}

// 从备份 JSON 导入为内部数据结构（完全兼容示例文件）
fun importBackupJson(json: String): TimeTableData {
    val gson = Gson()
    val root = JsonParser.parseString(json).asJsonObject
    val dataObj = root.getAsJsonObject("data")

    // terms
    val terms: List<Term> = gson.fromJson(
        dataObj.get("terms"), object : com.google.gson.reflect.TypeToken<List<Term>>() {}.type
    )

    // selectedTerm & theme
    val selectedTerm = dataObj.get("selectedTerm")?.asString ?: terms.firstOrNull()?.name ?: ""
    val theme = dataObj.get("theme")?.asString ?: "system"

    // timeSlots: 对象数组 -> 字符串 "HH:mm-HH:mm"
    val timeSlotsMaps: List<Map<String, String>> = gson.fromJson(
        dataObj.get("timeSlots"), object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
    )
    val timeSlots: List<String> = timeSlotsMaps.map { m ->
        val start = m["start"] ?: "00:00"
        val end = m["end"] ?: "00:00"
        "$start-$end"
    }

    // onlineCourses：id 可能为数字，统一转为字符串；credits 兼容小数
    val onlineCoursesRaw: Map<String, List<Map<String, Any>>> = gson.fromJson(
        dataObj.get("onlineCourses"), object : com.google.gson.reflect.TypeToken<Map<String, List<Map<String, Any>>>>() {}.type
    ) ?: emptyMap()
    val onlineCourses: Map<String, List<OnlineCourse>> = onlineCoursesRaw.mapValues { (_, list) ->
        list.map { oc ->
            OnlineCourse(
                id = oc["id"]?.toString() ?: java.util.UUID.randomUUID().toString(),
                courseName = (oc["courseName"] as? String) ?: "",
                teacher = (oc["teacher"] as? String) ?: "",
                platform = (oc["platform"] as? String) ?: "",
                url = (oc["url"] as? String) ?: "",
                startWeek = ((oc["startWeek"] as? Number)?.toInt()) ?: 1,
                endWeek = ((oc["endWeek"] as? Number)?.toInt()) ?: 16,
                credits = (oc["credits"] as? Number)?.toDouble(),
                notes = (oc["notes"] as? String) ?: ""
            )
        }
    }

    // courses：按示例结构构造 -> 内部结构：List<Course> 或 continued 标记
    val coursesRaw: Map<String, Map<String, Map<String, Any>>> = gson.fromJson(
        dataObj.get("courses"), object : com.google.gson.reflect.TypeToken<Map<String, Map<String, Map<String, Any>>>>() {}.type
    ) ?: emptyMap()

    val courses: MutableMap<String, MutableMap<Int, MutableMap<Int, Any>>> = mutableMapOf()
    coursesRaw.forEach { (term, daysStr) ->
        val dayMap: MutableMap<Int, MutableMap<Int, Any>> = mutableMapOf()
        daysStr.forEach { (dayKey, slotsStr) ->
            val dayIndex = dayKey.toIntOrNull() ?: 0
            val slotMap: MutableMap<Int, Any> = mutableMapOf()
            slotsStr.forEach { (slotKey, slotVal) ->
                val slotIndex = slotKey.toIntOrNull() ?: 0
                // slotVal 是课程对象；转成 Course
                val courseJson = gson.toJson(slotVal)
                val course = try { gson.fromJson(courseJson, Course::class.java) } catch (_: Exception) { null }
                if (course != null) {
                    slotMap[slotIndex] = listOf(course)
                    // 根据 duration 添加 continued
                    for (i in 1 until course.duration) {
                        val target = slotIndex + i
                        slotMap[target] = mapOf(
                            "continued" to true,
                            "color" to course.color,
                            "fromSlot" to slotIndex,
                            "courseName" to course.courseName
                        )
                    }
                }
            }
            dayMap[dayIndex] = slotMap
        }
        courses[term] = dayMap
    }

    return TimeTableData(
        terms = terms,
        courses = courses,
        onlineCourses = onlineCourses,
        timeSlots = timeSlots,
        selectedTerm = selectedTerm,
        theme = theme
    )
}