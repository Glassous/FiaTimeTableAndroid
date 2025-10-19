package com.glassous.fiatimetable.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.glassous.fiatimetable.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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