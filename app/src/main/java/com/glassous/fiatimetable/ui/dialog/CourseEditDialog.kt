package com.glassous.fiatimetable.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData

/**
 * 课程编辑对话框
 * 严格遵循 DATA_STRUCTURE.md 中的字段定义
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditDialog(
    course: Course? = null,
    dayIndex: Int,
    timeSlotIndex: Int,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    // 必填字段状态
    var courseName by remember { mutableStateOf(course?.courseName ?: "") }
    var courseType by remember { mutableStateOf(course?.courseType ?: "课程") }
    var duration by remember { mutableStateOf(course?.duration ?: 1) }
    var durationText by remember { mutableStateOf((course?.duration ?: 1).toString()) }
    var weeksRange by remember { mutableStateOf(course?.weeksRange ?: "1-16") }
    var selectedColor by remember { mutableStateOf(course?.color ?: TimeTableData.presetColors.first()) }
    
    // 可选字段状态
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var room by remember { mutableStateOf(course?.room ?: "") }
    var notes by remember { mutableStateOf(course?.notes ?: "") }
    
    // 详细信息字段状态（默认折叠）
    var showAdvanced by remember { mutableStateOf(false) }
    var courseCode by remember { mutableStateOf(course?.courseCode ?: "") }
    var shortName by remember { mutableStateOf(course?.shortName ?: "") }
    var courseNameEn by remember { mutableStateOf(course?.courseNameEn ?: "") }
    var credits by remember { mutableStateOf(course?.credits ?: 0) }
    var department by remember { mutableStateOf(course?.department ?: "") }
    var teachingGroup by remember { mutableStateOf(course?.teachingGroup ?: "") }
    var leader by remember { mutableStateOf(course?.leader ?: "") }
    var courseAttr by remember { mutableStateOf(course?.courseAttr ?: "必修") }
    var assessType by remember { mutableStateOf(course?.assessType ?: "考试") }
    var examType by remember { mutableStateOf(course?.examType ?: "闭卷") }
    var classroomType by remember { mutableStateOf(course?.classroomType ?: "") }
    var totalHours by remember { mutableStateOf(course?.totalHours ?: 0) }
    var referenceBooks by remember { mutableStateOf(course?.referenceBooks ?: "") }
    var capacity by remember { mutableStateOf(course?.capacity ?: 0) }
    var enrolled by remember { mutableStateOf(course?.enrolled ?: 0) }
    
    // 课程类型选项
    val courseTypes = listOf("课程", "实验", "实习", "讲座", "选修", "必修")
    val courseAttrs = listOf("必修", "选修", "限选")
    val assessTypes = listOf("考试", "考查")
    val examTypes = listOf("闭卷", "开卷", "论文")
    
    // 表单验证
    val isValid = courseName.isNotBlank() && 
                  courseType.isNotBlank() && 
                  (durationText.toIntOrNull()?.let { it in 1..8 } ?: false) && 
                  weeksRange.isNotBlank() && 
                  selectedColor.isNotBlank()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题
                Text(
                    text = if (course == null) "添加课程" else "编辑课程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 必填字段
                Text(
                    text = "基本信息 *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 课程名称
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称 *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    isError = courseName.isBlank()
                )
                
                // 课程类型
                var courseTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = courseTypeExpanded,
                    onExpandedChange = { courseTypeExpanded = !courseTypeExpanded },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = courseType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("课程类型 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = courseTypeExpanded,
                        onDismissRequest = { courseTypeExpanded = false }
                    ) {
                        courseTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    courseType = type
                                    courseTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // 占用节数
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { newValue ->
                        durationText = newValue
                        newValue.toIntOrNull()?.let { value ->
                            if (value in 1..8) {
                                duration = value
                            }
                        }
                    },
                    label = { Text("占用节数 (1-8) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    isError = durationText.toIntOrNull()?.let { it !in 1..8 } ?: true
                )
                
                // 周次范围
                OutlinedTextField(
                    value = weeksRange,
                    onValueChange = { weeksRange = it },
                    label = { Text("周次范围 (如: 1-16 或 1-8,10-16) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    isError = weeksRange.isBlank()
                )
                
                // 课程颜色
                Text(
                    text = "课程颜色 *",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(TimeTableData.presetColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                // 可选字段
                Text(
                    text = "教学信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("任课教师") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("上课地点") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    maxLines = 3
                )
                
                // 详细信息（可折叠）
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { showAdvanced = !showAdvanced },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "详细信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showAdvanced) "收起" else "展开"
                        )
                    }
                }
                
                if (showAdvanced) {
                    // 详细信息字段
                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("课程代码") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("课程简称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = courseNameEn,
                        onValueChange = { courseNameEn = it },
                        label = { Text("英文课程名") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = credits.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value >= 0) credits = value
                            }
                        },
                        label = { Text("学分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("开课院系") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = teachingGroup,
                        onValueChange = { teachingGroup = it },
                        label = { Text("教学班") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = leader,
                        onValueChange = { leader = it },
                        label = { Text("负责人") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    // 课程属性下拉框
                    var courseAttrExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = courseAttrExpanded,
                        onExpandedChange = { courseAttrExpanded = !courseAttrExpanded },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = courseAttr,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("课程属性") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseAttrExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = courseAttrExpanded,
                            onDismissRequest = { courseAttrExpanded = false }
                        ) {
                            courseAttrs.forEach { attr ->
                                DropdownMenuItem(
                                    text = { Text(attr) },
                                    onClick = {
                                        courseAttr = attr
                                        courseAttrExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 考核方式下拉框
                    var assessTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = assessTypeExpanded,
                        onExpandedChange = { assessTypeExpanded = !assessTypeExpanded },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = assessType,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("考核方式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assessTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = assessTypeExpanded,
                            onDismissRequest = { assessTypeExpanded = false }
                        ) {
                            assessTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        assessType = type
                                        assessTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 考试方式下拉框
                    var examTypeExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = examTypeExpanded,
                        onExpandedChange = { examTypeExpanded = !examTypeExpanded },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = examType,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("考试方式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examTypeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = examTypeExpanded,
                            onDismissRequest = { examTypeExpanded = false }
                        ) {
                            examTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        examType = type
                                        examTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = classroomType,
                        onValueChange = { classroomType = it },
                        label = { Text("教室类型") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = totalHours.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value >= 0) totalHours = value
                            }
                        },
                        label = { Text("总学时") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = referenceBooks,
                        onValueChange = { referenceBooks = it },
                        label = { Text("参考书目") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        maxLines = 3
                    )
                    
                    OutlinedTextField(
                        value = capacity.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value >= 0) capacity = value
                            }
                        },
                        label = { Text("容量") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = enrolled.toString(),
                        onValueChange = { 
                            it.toIntOrNull()?.let { value ->
                                if (value >= 0) enrolled = value
                            }
                        },
                        label = { Text("已选人数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            if (isValid) {
                                // 解析周次范围为selectedWeeks
                                val selectedWeeks = parseWeeksRange(weeksRange)
                                
                                val newCourse = Course(
                                    courseName = courseName,
                                    courseType = courseType,
                                    duration = duration,
                                    weeksRange = weeksRange,
                                    selectedWeeks = selectedWeeks,
                                    color = selectedColor,
                                    teacher = teacher,
                                    room = room,
                                    notes = notes,
                                    courseCode = courseCode,
                                    shortName = shortName,
                                    courseNameEn = courseNameEn,
                                    credits = credits,
                                    department = department,
                                    teachingGroup = teachingGroup,
                                    leader = leader,
                                    courseAttr = courseAttr,
                                    assessType = assessType,
                                    examType = examType,
                                    classroomType = classroomType,
                                    totalHours = totalHours,
                                    referenceBooks = referenceBooks,
                                    capacity = capacity,
                                    enrolled = enrolled
                                )
                                onSave(newCourse)
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 解析周次范围字符串为周次列表
 * 例如: "1-16" -> [1,2,3,...,16]
 * 例如: "1-8,10-16" -> [1,2,3,4,5,6,7,8,10,11,12,13,14,15,16]
 */
private fun parseWeeksRange(weeksRange: String): List<Int> {
    val weeks = mutableListOf<Int>()
    try {
        val ranges = weeksRange.split(",")
        for (range in ranges) {
            val trimmedRange = range.trim()
            if (trimmedRange.contains("-")) {
                val parts = trimmedRange.split("-")
                if (parts.size == 2) {
                    val start = parts[0].trim().toInt()
                    val end = parts[1].trim().toInt()
                    for (week in start..end) {
                        weeks.add(week)
                    }
                }
            } else {
                weeks.add(trimmedRange.toInt())
            }
        }
    } catch (e: Exception) {
        // 解析失败时返回默认范围
        return (1..16).toList()
    }
    return weeks.distinct().sorted()
}