package com.glassous.fiatimetable.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glassous.fiatimetable.data.model.Term
import com.glassous.fiatimetable.data.repository.TimeTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(private val repository: TimeTableRepository) : ViewModel() {
    private val _terms = MutableStateFlow<List<Term>>(emptyList())
    val terms: StateFlow<List<Term>> = _terms.asStateFlow()

    private val _selectedTerm = MutableStateFlow("")
    val selectedTerm: StateFlow<String> = _selectedTerm.asStateFlow()

    private val _morningSlots = MutableStateFlow<List<String>>(emptyList())
    val morningSlots: StateFlow<List<String>> = _morningSlots.asStateFlow()

    private val _afternoonSlots = MutableStateFlow<List<String>>(emptyList())
    val afternoonSlots: StateFlow<List<String>> = _afternoonSlots.asStateFlow()

    private val _eveningSlots = MutableStateFlow<List<String>>(emptyList())
    val eveningSlots: StateFlow<List<String>> = _eveningSlots.asStateFlow()

    // 主题偏好：system / light / dark
    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    // 启动页面偏好：day / week
    private val _startPage = MutableStateFlow("week")
    val startPage: StateFlow<String> = _startPage.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {
        viewModelScope.launch {
            val existingTerms = repository.getTerms()
            val currentSelected = repository.getSelectedTerm()

            if (existingTerms.isEmpty()) {
                val now = LocalDate.now()
                val (name, startDate) = deriveInitialTerm(now)
                val initial = Term(name = name, weeks = 16, startDate = startDate)
                repository.saveTerms(listOf(initial))
                repository.saveSelectedTerm(initial.name)
                _terms.value = listOf(initial)
                _selectedTerm.value = initial.name
            } else {
                _terms.value = existingTerms
                _selectedTerm.value = if (currentSelected.isNotEmpty()) currentSelected else existingTerms.first().name
                if (currentSelected.isEmpty()) repository.saveSelectedTerm(_selectedTerm.value)
            }

            // classify existing time slots
            val slots = repository.getTimeSlots()
            classifySlots(slots)

            // 加载主题偏好
            _theme.value = repository.getTheme()
            // 加载启动页面偏好
            _startPage.value = repository.getStartPage()
        }
    }

    private fun deriveInitialTerm(now: LocalDate): Pair<String, String> {
        val month = now.monthValue
        return if (month >= 9) {
            val startYear = now.year
            val endYear = now.year + 1
            Pair("${startYear}-${endYear} 秋季", now.withMonth(9).withDayOfMonth(1).format(dateFormatter))
        } else {
            val startYear = now.year - 1
            val endYear = now.year
            // 春季学期一般从2月末或3月初开始，这里默认2月最后一个周一
            val feb = LocalDate.of(endYear, 2, 1)
            val start = feb.withDayOfMonth(feb.lengthOfMonth())
            Pair("${startYear}-${endYear} 春季", start.format(dateFormatter))
        }
    }

    private fun classifySlots(all: List<String>) {
        val morning = mutableListOf<String>()
        val afternoon = mutableListOf<String>()
        val evening = mutableListOf<String>()
        all.forEach { slot ->
            val normalized = slot.replace("~", "-").replace("—", "-").replace("–", "-")
            val start = normalized.substringBefore("-")
             val hour = start.substringBefore(":").toIntOrNull() ?: 0
             when {
                 hour < 12 -> morning.add(slot)
                 hour < 18 -> afternoon.add(slot)
                 else -> evening.add(slot)
             }
        }
        _morningSlots.value = morning
        _afternoonSlots.value = afternoon
        _eveningSlots.value = evening
    }

    fun setSelectedTerm(termName: String) {
        viewModelScope.launch {
            _selectedTerm.value = termName
            repository.saveSelectedTerm(termName)
        }
    }

    fun addMorningSlot(slot: String) {
        _morningSlots.value = _morningSlots.value + slot
    }

    fun addAfternoonSlot(slot: String) {
        _afternoonSlots.value = _afternoonSlots.value + slot
    }

    fun addEveningSlot(slot: String) {
        _eveningSlots.value = _eveningSlots.value + slot
    }

    fun removeMorningSlot(index: Int) {
        _morningSlots.value = _morningSlots.value.toMutableList().apply { removeAt(index) }
    }

    fun removeAfternoonSlot(index: Int) {
        _afternoonSlots.value = _afternoonSlots.value.toMutableList().apply { removeAt(index) }
    }

    fun removeEveningSlot(index: Int) {
        _eveningSlots.value = _eveningSlots.value.toMutableList().apply { removeAt(index) }
    }

    fun saveTimeSlots() {
        viewModelScope.launch {
            val merged = _morningSlots.value + _afternoonSlots.value + _eveningSlots.value
            repository.saveTimeSlots(merged)
        }
    }

    fun saveTerm(name: String, startDate: String, weeks: Int) {
        viewModelScope.launch {
            val updated = _terms.value.toMutableList()
            val idx = updated.indexOfFirst { it.name == name }
            val newTerm = Term(name = name, weeks = weeks, startDate = startDate)
            if (idx >= 0) {
                updated[idx] = newTerm
            } else {
                updated.add(newTerm)
            }
            repository.saveTerms(updated)
            _terms.value = updated
            _selectedTerm.value = name
            repository.saveSelectedTerm(name)
        }
    }

    fun setGroupedSlots(morning: List<String>, afternoon: List<String>, evening: List<String>) {
        _morningSlots.value = morning
        _afternoonSlots.value = afternoon
        _eveningSlots.value = evening
    }

    // 主题设置：system / light / dark
    fun setTheme(theme: String) {
        viewModelScope.launch {
            _theme.value = theme
            repository.saveTheme(theme)
        }
    }

    // 启动页面设置：day / week
    fun setStartPage(page: String) {
        viewModelScope.launch {
            _startPage.value = page
            repository.saveStartPage(page)
        }
    }

    // 备份导出：返回 JSON 字符串
    fun exportBackupJson(): String {
        return com.glassous.fiatimetable.data.repository.exportBackupJson(repository)
    }

    // 备份导入：从 JSON 字符串恢复并保存到仓库，然后刷新本地状态
    fun importBackupJson(json: String) {
        viewModelScope.launch {
            val data = com.glassous.fiatimetable.data.repository.importBackupJson(json)
            repository.saveTimeTableData(data)
            _terms.value = data.terms
            _selectedTerm.value = data.selectedTerm
            classifySlots(data.timeSlots)
            _theme.value = data.theme
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(TimeTableRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}