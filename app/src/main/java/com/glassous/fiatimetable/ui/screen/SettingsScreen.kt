package com.glassous.fiatimetable.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glassous.fiatimetable.ui.viewmodel.SettingsViewModel
import com.glassous.fiatimetable.ui.viewmodel.SettingsViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

// 新增导入
import androidx.compose.ui.draw.scale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    val terms by viewModel.terms.collectAsState()
    val selectedTerm by viewModel.selectedTerm.collectAsState()
    val morningSlots by viewModel.morningSlots.collectAsState()
    val afternoonSlots by viewModel.afternoonSlots.collectAsState()
    val eveningSlots by viewModel.eveningSlots.collectAsState()

    var newTermName by remember { mutableStateOf(selectedTerm) }
    var newTermWeeks by remember { mutableStateOf("16") }
    var newTermStartDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showQuickSetup by remember { mutableStateOf(false) }

    LaunchedEffect(terms, selectedTerm) {
        val current = terms.find { it.name == selectedTerm } ?: terms.firstOrNull()
        if (current != null) {
            newTermName = current.name
            newTermWeeks = current.weeks.toString()
            newTermStartDate = current.startDate
        } else if (selectedTerm.isNotBlank()) {
            newTermName = selectedTerm
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        newTermStartDate = localDate.toString()
                        showDatePicker = false
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) {
        val padding = it
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "学期设置", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "选择学期：", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = selectedTerm,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("当前学期") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().weight(1f)
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    terms.forEach { term ->
                                        DropdownMenuItem(text = { Text(term.name) }, onClick = {
                                            viewModel.setSelectedTerm(term.name)
                                            expanded = false
                                        })
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTermName,
                            onValueChange = { newTermName = it },
                            label = { Text("学期名称") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTermWeeks,
                            onValueChange = { newTermWeeks = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("学期总周数") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTermStartDate,
                            onValueChange = { /* 通过日期选择器设置，避免手动输入 */ },
                            label = { Text("学期开始日期") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            placeholder = { Text("点击下方“选择日期”") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showDatePicker = true }) {
                                Text("选择日期")
                            }
                            Button(onClick = {
                                val weeks = newTermWeeks.toIntOrNull() ?: 16
                                val date = if (newTermStartDate.isNotBlank()) newTermStartDate else ""
                                if (newTermName.isNotBlank() && date.isNotBlank()) {
                                    viewModel.saveTerm(newTermName, date, weeks)
                                }
                            }) {
                                Text("保存学期")
                            }
                        }
                    }
                }
            }

            // 上课时间设置（上午/下午/晚上）
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "上课时间设置", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        TimeSlotsSection(
                            title = "上午",
                            slots = morningSlots,
                            onAdd = { viewModel.addMorningSlot(it) },
                            onRemove = { viewModel.removeMorningSlot(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeSlotsSection(
                            title = "下午",
                            slots = afternoonSlots,
                            onAdd = { viewModel.addAfternoonSlot(it) },
                            onRemove = { viewModel.removeAfternoonSlot(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeSlotsSection(
                            title = "晚上",
                            slots = eveningSlots,
                            onAdd = { viewModel.addEveningSlot(it) },
                            onRemove = { viewModel.removeEveningSlot(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showQuickSetup = true }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("快捷设置")
                            }
                        }
                    }
                }
            }

            // 数据备份与恢复（导入/导出）
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "数据备份与恢复", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        var backupMessage by remember { mutableStateOf<String?>(null) }

                        val exportLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.CreateDocument("application/json")
                        ) { uri ->
                            if (uri != null) {
                                val json = viewModel.exportBackupJson()
                                try {
                                    context.contentResolver.openOutputStream(uri)?.use { out ->
                                        out.write(json.toByteArray())
                                    }
                                    backupMessage = "导出成功"
                                } catch (e: Exception) {
                                    backupMessage = "导出失败：${e.message ?: "未知错误"}"
                                }
                            }
                        }

                        val importLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                try {
                                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                                    if (!text.isNullOrBlank()) {
                                        viewModel.importBackupJson(text)
                                        backupMessage = "导入成功"
                                    } else {
                                        backupMessage = "导入失败：文件为空"
                                    }
                                } catch (e: Exception) {
                                    backupMessage = "导入失败：${e.message ?: "未知错误"}"
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val filename = "timetable-backup-${LocalDate.now()}.json"
                                exportLauncher.launch(filename)
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("导出课程")
                            }
                            OutlinedButton(onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("导入课程")
                            }
                        }

                        if (backupMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(backupMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showQuickSetup) {
        QuickSetupDialog(
            onDismiss = { showQuickSetup = false },
            currentMorningSlots = morningSlots,
            currentAfternoonSlots = afternoonSlots,
            currentEveningSlots = eveningSlots,
            onApply = { mSlots, aSlots, eSlots ->
                viewModel.setGroupedSlots(mSlots, aSlots, eSlots)
                viewModel.saveTimeSlots() // 立即保存
                showQuickSetup = false
            }
        )
    }
}

@Composable
private fun TimeSlotsSection(
    title: String,
    slots: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        slots.forEachIndexed { index, slot ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = slot, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        var showAdd by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showAdd = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("添加时间段")
        }
        if (showAdd) {
            AddSlotDialog(title = "添加 $title 时间段", onDismiss = { showAdd = false }, onConfirm = { sh, sm, eh, em ->
                val value = "%02d:%02d-%02d:%02d".format(sh, sm, eh, em)
                if (isValidSlotFormat(value)) {
                    onAdd(value)
                    showAdd = false
                }
            })
        }
    }
}

private fun isValidSlotFormat(value: String): Boolean {
    val regex = Regex("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
    return regex.matches(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSlotDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) {
    var startHour by remember { mutableStateOf("08") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("08") }
    var endMinute by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("输入格式：HH:mm-HH:mm，如 08:00-08:45")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startHour, onValueChange = { startHour = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("开始小时") })
                    OutlinedTextField(value = startMinute, onValueChange = { startMinute = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("开始分钟") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = endHour, onValueChange = { endHour = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("结束小时") })
                    OutlinedTextField(value = endMinute, onValueChange = { endMinute = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("结束分钟") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sh = startHour.toIntOrNull() ?: 8
                val sm = startMinute.toIntOrNull() ?: 0
                val eh = endHour.toIntOrNull() ?: 8
                val em = endMinute.toIntOrNull() ?: 45
                onConfirm(sh, sm, eh, em)
            }) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSetupDialog(
    onDismiss: () -> Unit,
    currentMorningSlots: List<String>,
    currentAfternoonSlots: List<String>,
    currentEveningSlots: List<String>,
    onApply: (morning: List<String>, afternoon: List<String>, evening: List<String>) -> Unit
) {
    var h by remember { mutableStateOf("08") }
    var m by remember { mutableStateOf("00") }
    var d by remember { mutableStateOf("45") }
    var b by remember { mutableStateOf("10") }
    var c by remember { mutableStateOf("4") }

    var morningChecked by remember { mutableStateOf(true) }
    var afternoonChecked by remember { mutableStateOf(true) }
    var eveningChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快捷生成时间段") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("将按规则自动生成并替换对应时段的时间表")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = morningChecked, onCheckedChange = { morningChecked = it })
                    Text("上午")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = afternoonChecked, onCheckedChange = { afternoonChecked = it })
                    Text("下午")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = eveningChecked, onCheckedChange = { eveningChecked = it })
                    Text("晚上")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = h, onValueChange = { h = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("开始小时") })
                    OutlinedTextField(value = m, onValueChange = { m = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("开始分钟") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = d, onValueChange = { d = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("节时长(分钟)") })
                    OutlinedTextField(value = b, onValueChange = { b = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("课间(分钟)") })
                }
                OutlinedTextField(value = c, onValueChange = { c = it.filter { ch -> ch.isDigit() }.take(1) }, label = { Text("每段节数") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val slots = genSlots(h.toIntOrNull() ?: 8, m.toIntOrNull() ?: 0, d.toIntOrNull() ?: 45, b.toIntOrNull() ?: 10, c.toIntOrNull() ?: 4)
                val mSlots = if (morningChecked) slots else currentMorningSlots
                val aSlots = if (afternoonChecked) slots else currentAfternoonSlots
                val eSlots = if (eveningChecked) slots else currentEveningSlots
                onApply(mSlots, aSlots, eSlots)
                onDismiss()
            }) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun genSlots(h: Int, m: Int, d: Int, b: Int, c: Int): List<String> {
    val slots = mutableListOf<String>()
    var sh = h
    var sm = m
    repeat(c) {
        var eh = sh
        var em = sm + d
        if (em >= 60) {
            eh += em / 60
            em %= 60
        }
        slots.add("%02d:%02d-%02d:%02d".format(sh, sm, eh, em))
        sm = em + b
        sh = eh
        if (sm >= 60) {
            sh += sm / 60
            sm %= 60
        }
    }
    return slots
}