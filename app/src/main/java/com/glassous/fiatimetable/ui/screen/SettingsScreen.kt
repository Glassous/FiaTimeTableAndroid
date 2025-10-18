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
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        newTermStartDate = date.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = { Text("选择学期开始日期") },
                headline = null
            )
        }
    }

    // 主体内容
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 128.dp)
    ) {
        // 学期设置
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "学期设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (terms.isEmpty()) {
                        Text(
                            text = "已根据当前日期创建初始学期：$selectedTerm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 当前学期选择器
                    if (terms.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = selectedTerm,
                                onValueChange = {},
                                label = { Text("当前学期") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                terms.forEach { term ->
                                    DropdownMenuItem(
                                        text = { Text(term.name) },
                                        onClick = {
                                            viewModel.setSelectedTerm(term.name)
                                            expanded = false
                                            newTermName = term.name
                                            newTermWeeks = term.weeks.toString()
                                            newTermStartDate = term.startDate
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newTermName,
                        onValueChange = { newTermName = it },
                        label = { Text("学期名称，如 2025-2026 秋季") },
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
                        placeholder = { Text("点击下方\u201C选择日期\u201D") }
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
                        Button(onClick = { viewModel.saveTimeSlots() }) { Text("保存时间段") }
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
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var showAddDialog by remember { mutableStateOf(false) }
        FilledTonalButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加时间段")
        }
        if (showAddDialog) {
            AddSlotDialog(
                title = title,
                onDismiss = { showAddDialog = false },
                onConfirm = { sh, sm, eh, em ->
                    val slot = "%02d:%02d-%02d:%02d".format(sh, sm, eh, em)
                    onAdd(slot)
                    showAddDialog = false
                }
            )
        }

        slots.forEachIndexed { index, slot ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = slot, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }

        if (slots.isEmpty()) {
            Text(
                text = "暂无时间段",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun isValidSlotFormat(value: String): Boolean {
    val regex = Regex("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
    if (!regex.matches(value)) return false
    return try {
        val parts = value.split("-")
        val (sh, sm) = parts[0].split(":").map { it.toInt() }
        val (eh, em) = parts[1].split(":").map { it.toInt() }
        val start = sh * 60 + sm
        val end = eh * 60 + em
        start < end
    } catch (_: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSlotDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) {
    val startState = rememberTimePickerState(is24Hour = true)
    val endState = rememberTimePickerState(is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(startState.hour, startState.minute, endState.hour, endState.minute)
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = "添加 $title 时间段", style = MaterialTheme.typography.titleMedium)
                Text(text = "开始时间")
                TimePicker(state = startState)
                Text(text = "结束时间")
                TimePicker(state = endState)
            }
        }
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
    val scrollState = rememberScrollState()

    // 从当前数据计算初始值
    fun parseStartHM(slot: String): Pair<Int, Int>? = try {
        val start = slot.substringBefore("-")
        val h = start.substringBefore(":").toInt()
        val m = start.substringAfter(":").toInt()
        h to m
    } catch (_: Exception) { null }

    fun parseDuration(slot: String): Int? = try {
        val start = slot.substringBefore("-")
        val end = slot.substringAfter("-")
        val sh = start.substringBefore(":").toInt()
        val sm = start.substringAfter(":").toInt()
        val eh = end.substringBefore(":").toInt()
        val em = end.substringAfter(":").toInt()
        (eh * 60 + em) - (sh * 60 + sm)
    } catch (_: Exception) { null }

    fun parseBreak(first: String, second: String): Int? = try {
        val end1 = first.substringAfter("-")
        val start2 = second.substringBefore("-")
        val e1h = end1.substringBefore(":").toInt()
        val e1m = end1.substringAfter(":").toInt()
        val s2h = start2.substringBefore(":").toInt()
        val s2m = start2.substringAfter(":").toInt()
        (s2h * 60 + s2m) - (e1h * 60 + e1m)
    } catch (_: Exception) { null }

    val mHM = parseStartHM(currentMorningSlots.firstOrNull() ?: "08:00-08:45") ?: (8 to 0)
    val aHM = parseStartHM(currentAfternoonSlots.firstOrNull() ?: "14:00-14:45") ?: (14 to 0)
    val eHM = parseStartHM(currentEveningSlots.firstOrNull() ?: "19:00-19:45") ?: (19 to 0)

    val mDurInit = parseDuration(currentMorningSlots.firstOrNull() ?: "08:00-08:45") ?: 45
    val aDurInit = parseDuration(currentAfternoonSlots.firstOrNull() ?: "14:00-14:45") ?: 45
    val eDurInit = parseDuration(currentEveningSlots.firstOrNull() ?: "19:00-19:45") ?: 45

    val mBreakInit = if (currentMorningSlots.size >= 2) {
        parseBreak(currentMorningSlots[0], currentMorningSlots[1]) ?: 10
    } else 10
    val aBreakInit = if (currentAfternoonSlots.size >= 2) {
        parseBreak(currentAfternoonSlots[0], currentAfternoonSlots[1]) ?: 10
    } else 10
    val eBreakInit = if (currentEveningSlots.size >= 2) {
        parseBreak(currentEveningSlots[0], currentEveningSlots[1]) ?: 10
    } else 10

    val mCountInit = (if (currentMorningSlots.isNotEmpty()) currentMorningSlots.size else 4)
    val aCountInit = (if (currentAfternoonSlots.isNotEmpty()) currentAfternoonSlots.size else 4)
    val eCountInit = (if (currentEveningSlots.isNotEmpty()) currentEveningSlots.size else 2)

    val morningStartState = rememberTimePickerState(is24Hour = true, initialHour = mHM.first, initialMinute = mHM.second)
    val afternoonStartState = rememberTimePickerState(is24Hour = true, initialHour = aHM.first, initialMinute = aHM.second)
    val eveningStartState = rememberTimePickerState(is24Hour = true, initialHour = eHM.first, initialMinute = eHM.second)

    var morningDuration by remember { mutableStateOf(mDurInit.toString()) }
    var morningBreak by remember { mutableStateOf(mBreakInit.toString()) }
    var morningCount by remember { mutableStateOf(mCountInit.toString()) }

    var afternoonDuration by remember { mutableStateOf(aDurInit.toString()) }
    var afternoonBreak by remember { mutableStateOf(aBreakInit.toString()) }
    var afternoonCount by remember { mutableStateOf(aCountInit.toString()) }

    var eveningDuration by remember { mutableStateOf(eDurInit.toString()) }
    var eveningBreak by remember { mutableStateOf(eBreakInit.toString()) }
    var eveningCount by remember { mutableStateOf(eCountInit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val mSlots = genSlots(
                    morningStartState.hour,
                    morningStartState.minute,
                    morningDuration.toIntOrNull() ?: 45,
                    morningBreak.toIntOrNull() ?: 10,
                    morningCount.toIntOrNull() ?: 4
                )
                val aSlots = genSlots(
                    afternoonStartState.hour,
                    afternoonStartState.minute,
                    afternoonDuration.toIntOrNull() ?: 45,
                    afternoonBreak.toIntOrNull() ?: 10,
                    afternoonCount.toIntOrNull() ?: 4
                )
                val eSlots = genSlots(
                    eveningStartState.hour,
                    eveningStartState.minute,
                    eveningDuration.toIntOrNull() ?: 45,
                    eveningBreak.toIntOrNull() ?: 10,
                    eveningCount.toIntOrNull() ?: 2
                )
                onApply(mSlots, aSlots, eSlots)
            }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("上课时间快捷设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                // 早上
                Text("早上")
                TimePicker(state = morningStartState)
                OutlinedTextField(
                    value = morningDuration,
                    onValueChange = { morningDuration = it },
                    label = { Text("每节时长(分钟)") }
                )
                OutlinedTextField(
                    value = morningBreak,
                    onValueChange = { morningBreak = it },
                    label = { Text("间隔(分钟)") }
                )
                OutlinedTextField(
                    value = morningCount,
                    onValueChange = { morningCount = it },
                    label = { Text("节数") }
                )
                Divider()

                // 下午
                Text("下午")
                TimePicker(state = afternoonStartState)
                OutlinedTextField(
                    value = afternoonDuration,
                    onValueChange = { afternoonDuration = it },
                    label = { Text("每节时长(分钟)") }
                )
                OutlinedTextField(
                    value = afternoonBreak,
                    onValueChange = { afternoonBreak = it },
                    label = { Text("间隔(分钟)") }
                )
                OutlinedTextField(
                    value = afternoonCount,
                    onValueChange = { afternoonCount = it },
                    label = { Text("节数") }
                )
                Divider()

                // 晚上
                Text("晚上")
                TimePicker(state = eveningStartState)
                OutlinedTextField(
                    value = eveningDuration,
                    onValueChange = { eveningDuration = it },
                    label = { Text("每节时长(分钟)") }
                )
                OutlinedTextField(
                    value = eveningBreak,
                    onValueChange = { eveningBreak = it },
                    label = { Text("间隔(分钟)") }
                )
                OutlinedTextField(
                    value = eveningCount,
                    onValueChange = { eveningCount = it },
                    label = { Text("节数") }
                )
            }
        }
    )
}

private fun genSlots(h: Int, m: Int, d: Int, b: Int, c: Int): List<String> {
    var hour = h
    var minute = m
    val res = mutableListOf<String>()
    repeat(c) {
        var endHour = hour
        var endMinute = minute + d
        endHour += endMinute / 60
        endMinute %= 60
        res += "%02d:%02d-%02d:%02d".format(hour, minute, endHour, endMinute)
        endMinute += b
        endHour += endMinute / 60
        endMinute %= 60
        hour = endHour
        minute = endMinute
    }
    return res
}