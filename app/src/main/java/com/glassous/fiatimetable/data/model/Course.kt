package com.glassous.fiatimetable.data.model

/**
 * 课程数据结构
 * 严格遵守 DATA_STRUCTURE.md 中的字段定义
 */
data class Course(
    // 基本信息 - 必填字段
    val courseName: String,                 // 课程名称
    val courseType: String = "课程",        // 课程类型 (课程/实验/实习/讲座等)
    val duration: Int = 1,                  // 占用节数 (1-8)
    val weeksRange: String = "1-16",        // 周次范围，如 "1-16" 或 "1-8,10-16"
    val selectedWeeks: List<Int> = emptyList(), // 选中的周次数组，如 [1,2,3,4]
    val color: String,                      // 课程颜色，十六进制格式 "#rrggbb"
    
    // 教学信息 - 可选字段
    val teacher: String = "",               // 任课教师
    val room: String = "",                  // 上课地点
    val notes: String = "",                 // 备注信息
    
    // 详细信息 - 可选字段
    val courseCode: String = "",            // 课程代码，如 "MATH101"
    val shortName: String = "",             // 课程简称
    val courseNameEn: String = "",          // 英文课程名
    val credits: Int = 0,                   // 学分
    val department: String = "",            // 开课院系
    val teachingGroup: String = "",         // 教学班
    val leader: String = "",                // 负责人
    val courseAttr: String = "必修",        // 课程属性 (必修/选修/限选)
    val assessType: String = "考试",        // 考核方式 (考试/考查)
    val examType: String = "闭卷",          // 考试方式 (闭卷/开卷/论文)
    val classroomType: String = "",         // 教室类型
    val totalHours: Int = 0,                // 总学时
    val referenceBooks: String = "",        // 参考书目
    val capacity: Int = 0,                  // 容量
    val enrolled: Int = 0                   // 已选人数
)

/**
 * 延续标记对象
 * 当课程占用多个时间段时，后续时间段存储延续标记
 */
data class ContinuationMarker(
    val continued: Boolean = true,          // 固定为 true，表示这是延续标记
    val fromSlot: Int                       // 指向课程开始的时间段索引
)