package com.glassous.fiatimetable.data.model

/**
 * 完整的课程表数据结构
 * 严格遵守 DATA_STRUCTURE.md 中的数据存储概览
 */
data class TimeTableData(
    val terms: List<Term> = emptyList(),                    // 学期信息 (xf_terms)
    val courses: Map<String, Map<Int, Map<Int, Any>>> = emptyMap(), // 课程数据 (xf_courses)
    val onlineCourses: Map<String, List<OnlineCourse>> = emptyMap(), // 网络选修课数据 (xf_onlineCourses)
    val timeSlots: List<String> = defaultTimeSlots,        // 时间段设置 (xf_timeSlots)
    val selectedTerm: String = "",                          // 当前选中学期 (xf_term)
    val theme: String = "system"                            // 主题设置 (xf_theme)
) {
    companion object {
        // 默认时间段设置
        val defaultTimeSlots = listOf(
            "08:00-08:45",
            "08:55-09:40",
            "10:00-10:45",
            "10:55-11:40",
            "14:00-14:45",
            "14:55-15:40",
            "16:00-16:45",
            "16:55-17:40"
        )
        
        // 预设颜色列表
        val presetColors = listOf(
            "#ef4444", // red-500
            "#f97316", // orange-500
            "#f59e0b", // amber-500
            "#eab308", // yellow-500
            "#84cc16", // lime-500
            "#22c55e", // green-500
            "#10b981", // emerald-500
            "#14b8a6", // teal-500
            "#06b6d4", // cyan-500
            "#0ea5e9", // sky-500
            "#3b82f6", // blue-500
            "#6366f1", // indigo-500
            "#8b5cf6", // violet-500
            "#a855f7", // purple-500
            "#d946ef", // fuchsia-500
            "#ec4899", // pink-500
            "#f43f5e", // rose-500
            "#64748b", // slate-500
            "#6b7280", // gray-500
            "#78716c"  // stone-500
        )
        
        // 星期索引对照表
        val weekDayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}

/**
 * 数据导出格式
 */
data class ExportData(
    val version: String = "1.0",
    val exportDate: String,
    val data: TimeTableData
)