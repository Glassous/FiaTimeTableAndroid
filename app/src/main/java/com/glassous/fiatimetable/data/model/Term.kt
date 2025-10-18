package com.glassous.fiatimetable.data.model

/**
 * 学期数据结构
 * 严格遵守 DATA_STRUCTURE.md 中的字段定义
 */
data class Term(
    val name: String,           // 学期名称，如 "2023-2024 秋季"
    val weeks: Int,             // 学期总周数
    val startDate: String       // 学期开始日期，格式: "YYYY-MM-DD"
)