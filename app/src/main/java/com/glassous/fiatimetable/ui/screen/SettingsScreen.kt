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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
    // 新增：主题偏好
    val themePref by viewModel.theme.collectAsState()
    // 新增：启动页面偏好
    val startPagePref by viewModel.startPage.collectAsState()

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
            // 新增：界面主题设置
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "界面主题", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val options = listOf("system", "light", "dark")
                        val labels = listOf("跟随系统", "浅色", "深色")
                        var selectedIndex by remember(themePref) {
                            mutableStateOf(options.indexOf(themePref).coerceIn(0, options.size - 1))
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, opt ->
                                SegmentedButton(
                                    selected = index == selectedIndex,
                                    onClick = {
                                        selectedIndex = index
                                        viewModel.setTheme(opt)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    val icon = when (opt) {
                                        "system" -> Icons.Filled.PhoneAndroid
                                        "light" -> Icons.Filled.WbSunny
                                        else -> Icons.Filled.DarkMode
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(labels[index], maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "默认跟随系统",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 新增：启动页面设置
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "启动页面", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val startOptions = listOf("day", "week")
                        val startLabels = listOf("日视图", "周视图")
                        var startSelectedIndex by remember(startPagePref) {
                            mutableStateOf(startOptions.indexOf(startPagePref).coerceIn(0, startOptions.size - 1))
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            startOptions.forEachIndexed { index, opt ->
                                SegmentedButton(
                                    selected = index == startSelectedIndex,
                                    onClick = {
                                        startSelectedIndex = index
                                        viewModel.setStartPage(opt)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = startOptions.size),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    val icon = when (opt) {
                                        "day" -> Icons.Filled.DateRange
                                        else -> Icons.Filled.Home
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(startLabels[index], maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "选择应用启动时进入的默认页面",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
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
                            onValueChange = { newTermStartDate = it },
                            label = { Text("学期开始日期 (YYYY-MM-DD)") },
                            trailingIcon = {
                                TextButton(onClick = { showDatePicker = true }) { Text("选择日期") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.saveTerm(newTermName, newTermStartDate, newTermWeeks.toIntOrNull() ?: 16) }) { Text("保存学期") }
                            OutlinedButton(onClick = { showQuickSetup = true }) { Text("快速生成时间段") }
                        }
                        if (showQuickSetup) {
                            QuickSetupDialog(
                                currentMorningSlots = morningSlots,
                                currentAfternoonSlots = afternoonSlots,
                                currentEveningSlots = eveningSlots,
                                onDismiss = { showQuickSetup = false },
                                onApply = { m, a, e -> viewModel.setGroupedSlots(m, a, e) }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "时间段设置", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeSlotsSection(title = "上午", slots = morningSlots, onAdd = { viewModel.addMorningSlot(it) }, onRemove = { viewModel.removeMorningSlot(it) })
                        Spacer(modifier = Modifier.height(12.dp))
                        TimeSlotsSection(title = "下午", slots = afternoonSlots, onAdd = { viewModel.addAfternoonSlot(it) }, onRemove = { viewModel.removeAfternoonSlot(it) })
                        Spacer(modifier = Modifier.height(12.dp))
                        TimeSlotsSection(title = "晚上", slots = eveningSlots, onAdd = { viewModel.addEveningSlot(it) }, onRemove = { viewModel.removeEveningSlot(it) })
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "数据备份与还原", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        BackupSection(viewModel)
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSlotDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Int) -> Unit
) {
    var startHour by remember { mutableStateOf("") }
    var startMinute by remember { mutableStateOf("") }
    var endHour by remember { mutableStateOf("") }
    var endMinute by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    currentMorningSlots: List<String>,
    currentAfternoonSlots: List<String>,
    currentEveningSlots: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>, List<String>, List<String>) -> Unit
) {
    // 初始时间使用当前已有的时段数据（若无则采用合理默认）
    fun firstStartHM(slots: List<String>, fallbackHour: Int, fallbackMinute: Int): Pair<Int, Int> {
        val s = slots.firstOrNull() ?: return fallbackHour to fallbackMinute
        val normalized = s.replace("~", "-").replace("—", "-").replace("–", "-")
        val start = normalized.substringBefore("-")
        val h = start.substringBefore(":").toIntOrNull() ?: fallbackHour
        val m = start.substringAfter(":").toIntOrNull() ?: fallbackMinute
        return h to m
    }

    // 分页（上午/下午/晚上）
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("上午", "下午", "晚上")

    // 默认全部覆盖
    var morningChecked by remember { mutableStateOf(true) }
    var afternoonChecked by remember { mutableStateOf(true) }
    var eveningChecked by remember { mutableStateOf(true) }

    // 各时段独立设置（基于已有时段初始化）
    val (mInitH, mInitM) = remember { firstStartHM(currentMorningSlots, 8, 0) }
    val morningTime = rememberTimePickerState(initialHour = mInitH, initialMinute = mInitM, is24Hour = true)
    var morningDuration by remember { mutableStateOf("45") }
    var morningBreak by remember { mutableStateOf("10") }
    var morningCount by remember { mutableStateOf("4") }

    val (aInitH, aInitM) = remember { firstStartHM(currentAfternoonSlots, 14, 0) }
    val afternoonTime = rememberTimePickerState(initialHour = aInitH, initialMinute = aInitM, is24Hour = true)
    var afternoonDuration by remember { mutableStateOf("45") }
    var afternoonBreak by remember { mutableStateOf("10") }
    var afternoonCount by remember { mutableStateOf("4") }

    val (eInitH, eInitM) = remember { firstStartHM(currentEveningSlots, 19, 0) }
    val eveningTime = rememberTimePickerState(initialHour = eInitH, initialMinute = eInitM, is24Hour = true)
    var eveningDuration by remember { mutableStateOf("45") }
    var eveningBreak by remember { mutableStateOf("10") }
    var eveningCount by remember { mutableStateOf("4") }

    var infoMessage by remember { mutableStateOf<String?>(null) }

    // 本地工具函数：解析节次开始/结束分钟数
    fun parseStartMinutes(slot: String): Int {
        val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
        val start = normalized.substringBefore("-")
        val h = start.substringBefore(":").toIntOrNull() ?: 0
        val m = start.substringAfter(":").toIntOrNull() ?: 0
        return h * 60 + m
    }
    fun parseEndMinutes(slot: String): Int {
        val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
        val end = normalized.substringAfter("-")
        val h = end.substringBefore(":").toIntOrNull() ?: 0
        val m = end.substringAfter(":").toIntOrNull() ?: 0
        return h * 60 + m
    }

    @Composable
    fun PeriodConfigPage(
        periodLabel: String,
        coverChecked: Boolean,
        onCoverChange: (Boolean) -> Unit,
        timeState: androidx.compose.material3.TimePickerState,
        durationText: String,
        onDurationChange: (String) -> Unit,
        breakText: String,
        onBreakChange: (String) -> Unit,
        countText: String,
        onCountChange: (String) -> Unit,
        warnText: String?
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = coverChecked, onCheckedChange = onCoverChange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$periodLabel：覆盖此时段")
            }
            TimePicker(state = timeState)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = onDurationChange,
                    label = { Text("节时长(分钟)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = breakText,
                    onValueChange = onBreakChange,
                    label = { Text("课间(分钟)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = countText,
                onValueChange = onCountChange,
                label = { Text("节数") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (!warnText.isNullOrBlank()) {
                Text(warnText!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("快速生成时间段") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
                        )
                    }
                }
                when (selectedTab) {
                    0 -> PeriodConfigPage(
                        periodLabel = "上午",
                        coverChecked = morningChecked,
                        onCoverChange = { morningChecked = it },
                        timeState = morningTime,
                        durationText = morningDuration,
                        onDurationChange = { morningDuration = it.filter { c -> c.isDigit() }.take(2) },
                        breakText = morningBreak,
                        onBreakChange = { morningBreak = it.filter { c -> c.isDigit() }.take(2) },
                        countText = morningCount,
                        onCountChange = { morningCount = it.filter { c -> c.isDigit() }.take(1) },
                        warnText = if (morningTime.hour >= 12) "上午开始时间建议在 12:00 前" else null
                    )
                    1 -> PeriodConfigPage(
                        periodLabel = "下午",
                        coverChecked = afternoonChecked,
                        onCoverChange = { afternoonChecked = it },
                        timeState = afternoonTime,
                        durationText = afternoonDuration,
                        onDurationChange = { afternoonDuration = it.filter { c -> c.isDigit() }.take(2) },
                        breakText = afternoonBreak,
                        onBreakChange = { afternoonBreak = it.filter { c -> c.isDigit() }.take(2) },
                        countText = afternoonCount,
                        onCountChange = { afternoonCount = it.filter { c -> c.isDigit() }.take(1) },
                        warnText = if (afternoonTime.hour !in 12..17) "下午开始时间建议在 12:00-18:00 之间" else null
                    )
                    2 -> PeriodConfigPage(
                        periodLabel = "晚上",
                        coverChecked = eveningChecked,
                        onCoverChange = { eveningChecked = it },
                        timeState = eveningTime,
                        durationText = eveningDuration,
                        onDurationChange = { eveningDuration = it.filter { c -> c.isDigit() }.take(2) },
                        breakText = eveningBreak,
                        onBreakChange = { eveningBreak = it.filter { c -> c.isDigit() }.take(2) },
                        countText = eveningCount,
                        onCountChange = { eveningCount = it.filter { c -> c.isDigit() }.take(1) },
                        warnText = if (eveningTime.hour < 18) "晚上开始时间建议在 18:00 之后" else null
                    )
                }
                if (infoMessage != null) {
                    Text(infoMessage!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mSlotsGen = if (morningChecked) genSlots(
                    morningTime.hour, morningTime.minute,
                    morningDuration.toIntOrNull() ?: 45,
                    morningBreak.toIntOrNull() ?: 10,
                    morningCount.toIntOrNull() ?: 4
                ) else currentMorningSlots

                val aSlotsGen = if (afternoonChecked) genSlots(
                    afternoonTime.hour, afternoonTime.minute,
                    afternoonDuration.toIntOrNull() ?: 45,
                    afternoonBreak.toIntOrNull() ?: 10,
                    afternoonCount.toIntOrNull() ?: 4
                ) else currentAfternoonSlots

                var eSlotsGen = if (eveningChecked) genSlots(
                    eveningTime.hour, eveningTime.minute,
                    eveningDuration.toIntOrNull() ?: 45,
                    eveningBreak.toIntOrNull() ?: 10,
                    eveningCount.toIntOrNull() ?: 4
                ) else currentEveningSlots

                // 与下午时段的冲突规避
                val earliestAfternoonStart = aSlotsGen.firstOrNull()?.let { parseStartMinutes(it) } ?: (12 * 60)
                val latestAfternoonEnd = aSlotsGen.lastOrNull()?.let { parseEndMinutes(it) } ?: (18 * 60)

                val mResolved = if (morningChecked) {
                    mSlotsGen.filter { parseEndMinutes(it) <= earliestAfternoonStart }
                } else mSlotsGen

                if (eveningChecked && eSlotsGen.isNotEmpty() && parseStartMinutes(eSlotsGen.first()) < latestAfternoonEnd) {
                    // 晚上开始时间顺延至下午结束后
                    val newStartHour = latestAfternoonEnd / 60
                    val newStartMinute = latestAfternoonEnd % 60
                    eSlotsGen = genSlots(
                        newStartHour, newStartMinute,
                        eveningDuration.toIntOrNull() ?: 45,
                        eveningBreak.toIntOrNull() ?: 10,
                        eveningCount.toIntOrNull() ?: 4
                    )
                    infoMessage = "已自动调整晚上开始时间以避开下午时段"
                }

                onApply(mResolved, aSlotsGen, eSlotsGen)
                onDismiss()
            }) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun isValidSlotFormat(value: String): Boolean {
    val regex = Regex("^\\d{2}:\\d{2}-\\d{2}:\\d{2}$")
    return regex.matches(value)
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

@Composable
private fun BackupSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val filename = "timetable-backup-${LocalDate.now()}.json"
                exportLauncher.launch(filename)
            }) {
                Text("导出课程")
            }
            OutlinedButton(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/*"))
            }) {
                Text("导入课程")
            }
        }
        if (backupMessage != null) {
            Text(backupMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}