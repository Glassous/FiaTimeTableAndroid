package com.glassous.fiatimetable.data.model

/**
 * 网络选修课数据结构
 * 严格遵守 DATA_STRUCTURE.md 中的字段定义
 * 这类课程没有固定的上课时间和地点
 */
data class OnlineCourse(
    // 基本信息
    val id: String,                         // 唯一标识符，自动生成
    val courseName: String,                 // 课程名称
    val teacher: String = "",               // 任课教师
    val platform: String = "",             // 学习平台/应用名称
    val url: String = "",                   // 课程网址链接
    
    // 时间信息
    val startWeek: Int = 1,                 // 开始周次
    val endWeek: Int = 16,                  // 结束周次
    
    // 其他信息
    val credits: Int? = null,               // 学分
    val notes: String = ""                  // 备注信息
)