package com.glassous.fiatimetable.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.glassous.fiatimetable.data.model.OnlineCourse
import java.util.UUID

/**
 * 网络选修课编辑对话框
 * 严格遵守 DATA_STRUCTURE.md 与 OnlineCourse.kt 定义
 */
@Composable
fun OnlineCourseEditDialog(
    course: OnlineCourse? = null,
    termWeeks: Int,
    onDismiss: () -> Unit,
    onSave: (OnlineCourse) -> Unit
) {
    var courseName by remember { mutableStateOf(course?.courseName ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var platform by remember { mutableStateOf(course?.platform ?: "") }
    var url by remember { mutableStateOf(course?.url ?: "") }
    var startWeekText by remember { mutableStateOf((course?.startWeek ?: 1).toString()) }
    var endWeekText by remember { mutableStateOf((course?.endWeek ?: termWeeks).toString()) }
    var creditsText by remember { mutableStateOf(course?.credits?.toString() ?: "") }
    var notes by remember { mutableStateOf(course?.notes ?: "") }

    val isStartWeekValid = startWeekText.toIntOrNull()?.let { it in 1..termWeeks } ?: false
    val isEndWeekValid = endWeekText.toIntOrNull()?.let { it in 1..termWeeks } ?: false
    val startWeekVal = startWeekText.toIntOrNull() ?: 0
    val endWeekVal = endWeekText.toIntOrNull() ?: 0
    val isWeekRangeValid = isStartWeekValid && isEndWeekValid && startWeekVal <= endWeekVal

    val isValid = courseName.isNotBlank() && isWeekRangeValid

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = if (course == null) "新增网络选修课" else "编辑网络选修课", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("课程名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = courseName.isBlank()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("任课教师") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("学习平台/应用") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("课程网址") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startWeekText,
                        onValueChange = { value ->
                            val v = value.filter { it.isDigit() }
                            startWeekText = v
                        },
                        label = { Text("开始周 *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isStartWeekValid,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endWeekText,
                        onValueChange = { value ->
                            val v = value.filter { it.isDigit() }
                            endWeekText = v
                        },
                        label = { Text("结束周 *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isEndWeekValid || (isStartWeekValid && (endWeekVal < startWeekVal)),
                        modifier = Modifier.weight(1f)
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(text = "学期周数: 1-$termWeeks") }
                )

                OutlinedTextField(
                    value = creditsText,
                    onValueChange = { value ->
                        // 允许小数点
                        val filtered = value.filter { it.isDigit() || it == '.' }
                        creditsText = filtered
                    },
                    label = { Text("学分") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            if (isValid) {
                                val creditsVal = creditsText.toDoubleOrNull()
                                val newCourse = OnlineCourse(
                                    id = course?.id ?: UUID.randomUUID().toString(),
                                    courseName = courseName,
                                    teacher = teacher,
                                    platform = platform,
                                    url = url,
                                    startWeek = startWeekVal,
                                    endWeek = endWeekVal,
                                    credits = creditsVal,
                                    notes = notes
                                )
                                onSave(newCourse)
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }
                }
            }
        }
    }
}