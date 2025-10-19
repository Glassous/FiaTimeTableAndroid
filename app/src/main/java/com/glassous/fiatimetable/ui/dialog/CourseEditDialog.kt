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
import androidx.compose.ui.window.DialogProperties
import com.glassous.fiatimetable.data.model.Course
import com.glassous.fiatimetable.data.model.TimeTableData
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * 课程编辑对话框
 * 严格遵循 DATA_STRUCTURE.md 中的字段定义
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditDialog(
    course: Course? = null,
    dayIndex: Int,
    timeSlotIndex: Int,
    termWeeks: Int,
    occupiedWeeks: Set<Int>,
    initialColor: String? = null,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    // 必填字段状态
    var courseName by remember { mutableStateOf(course?.courseName ?: "") }
    var courseType by remember { mutableStateOf(course?.courseType ?: "课程") }
    var duration by remember { mutableStateOf(course?.duration ?: 1) }
    var durationText by remember { mutableStateOf((course?.duration ?: 1).toString()) }
    var selectedColor by remember { mutableStateOf(course?.color ?: (initialColor ?: TimeTableData.presetColors.first())) }

    // 周次选择状态（卡片式）
    var selectedWeeks by remember { mutableStateOf(course?.selectedWeeks ?: emptyList()) }

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
                  selectedWeeks.isNotEmpty() && 
                  selectedColor.isNotBlank()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = if (course == null) "添加课程" else "编辑课程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
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
                
                // 周次选择（卡片式）
                Text(
                    text = "周次选择 *",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // 单周/双周/全选/清空 快捷设置
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    fun canSelect(w: Int): Boolean = !(occupiedWeeks.contains(w) && (course?.selectedWeeks?.contains(w) != true))
                    AssistChip(onClick = {
                        selectedWeeks = (1..termWeeks).filter { it % 2 == 1 && canSelect(it) }
                    }, label = { Text("单周") })
                    AssistChip(onClick = {
                        selectedWeeks = (1..termWeeks).filter { it % 2 == 0 && canSelect(it) }
                    }, label = { Text("双周") })
                    AssistChip(onClick = {
                        selectedWeeks = (1..termWeeks).filter { canSelect(it) }
                    }, label = { Text("全选") })
                    AssistChip(onClick = { selectedWeeks = emptyList() }, label = { Text("清空") })
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    (1..termWeeks).forEach { w ->
                        val disabled = occupiedWeeks.contains(w) && (course?.selectedWeeks?.contains(w) != true)
                        val selected = selectedWeeks.contains(w)
                        val bg = when {
                            disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            disabled -> MaterialTheme.colorScheme.outline
                            selected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bg),
                            modifier = Modifier
                                .width(56.dp)
                                .height(36.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable(enabled = !disabled) {
                                    selectedWeeks = if (selected) selectedWeeks - w else selectedWeeks + w
                                    selectedWeeks = selectedWeeks.distinct().sorted()
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = w.toString(), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                if (selectedWeeks.isEmpty()) {
                    Text(
                        text = "请选择至少一个周次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = formatWeeksRange(selectedWeeks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // 课程颜色
                Text(
                    text = "课程颜色 *",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                var showColorPicker by remember { mutableStateOf(false) }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
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
                OutlinedButton(onClick = { showColorPicker = true }, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text("自定义颜色")
                }
                if (showColorPicker) {
                    ColorPickerDialog(
                        initialHex = selectedColor,
                        onDismiss = { showColorPicker = false },
                        onConfirm = { hex -> selectedColor = hex; showColorPicker = false }
                    )
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
                
                // 结束可滚动区域
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
                                val newCourse = Course(
                                    courseName = courseName,
                                    courseType = courseType,
                                    duration = duration,
                                    weeksRange = formatWeeksRange(selectedWeeks),
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

// 将周次列表压缩为区间字符串，如 [1,2,3,5,7,8] -> "1-3,5,7-8"
private fun formatWeeksRange(weeks: List<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.distinct().sorted()
    val sb = StringBuilder()
    var start = sorted.first()
    var prev = start
    for (i in 1 until sorted.size) {
        val cur = sorted[i]
        if (cur == prev + 1) {
            prev = cur
        } else {
            if (start == prev) sb.append(start) else sb.append("$start-$prev")
            sb.append(',')
            start = cur
            prev = cur
        }
    }
    if (start == prev) sb.append(start) else sb.append("$start-$prev")
    return sb.toString()
}

@Composable
private fun ColorPickerDialog(initialHex: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var hexText by remember { mutableStateOf(initialHex) }
    var r by remember { mutableStateOf(try { Color(android.graphics.Color.parseColor(initialHex)).red } catch (_: Exception) { 1f }) }
    var g by remember { mutableStateOf(try { Color(android.graphics.Color.parseColor(initialHex)).green } catch (_: Exception) { 1f }) }
    var b by remember { mutableStateOf(try { Color(android.graphics.Color.parseColor(initialHex)).blue } catch (_: Exception) { 1f }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(r, g, b))
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Hex 输入
                OutlinedTextField(
                    value = hexText,
                    onValueChange = {
                        hexText = it
                        runCatching {
                            val c = Color(android.graphics.Color.parseColor(it))
                            r = c.red; g = c.green; b = c.blue
                        }
                    },
                    label = { Text("HEX，如 #4CAF50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                // RGB 滑条
                Text("R")
                Slider(value = r, onValueChange = { r = it; hexText = toHex(r, g, b) }, valueRange = 0f..1f)
                Text("G")
                Slider(value = g, onValueChange = { g = it; hexText = toHex(r, g, b) }, valueRange = 0f..1f)
                Text("B")
                Slider(value = b, onValueChange = { b = it; hexText = toHex(r, g, b) }, valueRange = 0f..1f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(toHex(r, g, b)) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun toHex(r: Float, g: Float, b: Float): String {
    val rr = (r * 255).toInt().coerceIn(0, 255)
    val gg = (g * 255).toInt().coerceIn(0, 255)
    val bb = (b * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", rr, gg, bb)
}