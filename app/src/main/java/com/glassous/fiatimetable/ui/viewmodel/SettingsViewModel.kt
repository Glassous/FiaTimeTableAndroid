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

    // 云端同步配置状态
    private val _ossEndpoint = MutableStateFlow("")
    val ossEndpoint: StateFlow<String> = _ossEndpoint.asStateFlow()
    private val _ossBucket = MutableStateFlow("")
    val ossBucket: StateFlow<String> = _ossBucket.asStateFlow()
    private val _ossObjectKey = MutableStateFlow("timetable-data.json")
    val ossObjectKey: StateFlow<String> = _ossObjectKey.asStateFlow()
    private val _ossAkId = MutableStateFlow("")
    val ossAkId: StateFlow<String> = _ossAkId.asStateFlow()
    private val _ossAkSecret = MutableStateFlow("")
    val ossAkSecret: StateFlow<String> = _ossAkSecret.asStateFlow()
    private val _ossRegion = MutableStateFlow("")
    val ossRegion: StateFlow<String> = _ossRegion.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 是否显示周末：true/false
    private val _showWeekend = MutableStateFlow(true)
    val showWeekend: StateFlow<Boolean> = _showWeekend.asStateFlow()

    // 是否显示周末：true/false
    private val _showSaturday = MutableStateFlow(true)
    val showSaturday: StateFlow<Boolean> = _showSaturday.asStateFlow()
    private val _showSunday = MutableStateFlow(true)
    val showSunday: StateFlow<Boolean> = _showSunday.asStateFlow()

    // 是否显示休息分隔线：true/false
    private val _showBreaks = MutableStateFlow(true)
    val showBreaks: StateFlow<Boolean> = _showBreaks.asStateFlow()

    // 新增：是否显示“再下节课”小卡片
    private val _showNextCourseCard = MutableStateFlow(true)
    val showNextCourseCard: StateFlow<Boolean> = _showNextCourseCard.asStateFlow()

    // 新增：课视图字段放大弹窗开关
    private val _showFieldDialog = MutableStateFlow(true)
    val showFieldDialog: StateFlow<Boolean> = _showFieldDialog.asStateFlow()

    private val _hideInactiveCourses = MutableStateFlow(false)
    val hideInactiveCourses: StateFlow<Boolean> = _hideInactiveCourses.asStateFlow()

    init {
        viewModelScope.launch {
            loadData()
            loadOssConfig()
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
            // 加载是否显示周六/周日
            _showSaturday.value = repository.getShowSaturday()
            _showSunday.value = repository.getShowSunday()
            // 加载是否显示休息分隔线
            _showBreaks.value = repository.getShowBreaks()
            // 加载是否显示“再下节课”小卡片
            _showNextCourseCard.value = repository.getShowNextCourseCard()
            // 加载是否显示字段放大弹窗
            _showFieldDialog.value = repository.getShowFieldDialog()
            _hideInactiveCourses.value = repository.getHideInactiveCourses()
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

    // 云端同步：加载配置
    fun loadOssConfig() {
        val cfg = repository.getOssSyncConfig()
        if (cfg != null) {
            _ossEndpoint.value = cfg.endpoint
            _ossBucket.value = cfg.bucketName
            _ossObjectKey.value = cfg.objectKey
            _ossAkId.value = cfg.accessKeyId
            _ossAkSecret.value = cfg.accessKeySecret
            _ossRegion.value = cfg.regionId ?: ""
        } else {
            _ossEndpoint.value = ""
            _ossBucket.value = ""
            _ossObjectKey.value = "timetable-data.json"
            _ossAkId.value = ""
            _ossAkSecret.value = ""
            _ossRegion.value = ""
        }
    }

    // 云端同步：保存配置
    fun saveOssConfig(endpoint: String, bucket: String, objectKey: String, akId: String, akSecret: String, regionId: String) {
        viewModelScope.launch {
            val cfg = com.glassous.fiatimetable.data.repository.OssSyncConfig(
                endpoint = endpoint.trim().removeSuffix("/"),
                bucketName = bucket.trim(),
                objectKey = objectKey.trim().ifBlank { "timetable-data.json" },
                accessKeyId = akId.trim(),
                accessKeySecret = akSecret.trim(),
                regionId = regionId.trim().ifBlank { null }
            )
            repository.saveOssSyncConfig(cfg)
            _ossEndpoint.value = cfg.endpoint
            _ossBucket.value = cfg.bucketName
            _ossObjectKey.value = cfg.objectKey
            _ossAkId.value = cfg.accessKeyId
            _ossAkSecret.value = cfg.accessKeySecret
            _ossRegion.value = cfg.regionId ?: ""
        }
    }

    // 云端同步：手动上传
    fun uploadToCloud() {
        viewModelScope.launch {
            try {
                val json = exportBackupJson()
                repository.uploadBackupToOss(json)
                _syncMessage.value = "上传成功"
            } catch (e: com.alibaba.sdk.android.oss.ClientException) {
                _syncMessage.value = "上传失败：网络或本地错误：${e.message ?: "未知错误"}"
            } catch (e: com.alibaba.sdk.android.oss.ServiceException) {
                _syncMessage.value = "上传失败：${e.errorCode ?: "ServiceException"} (requestId=${e.requestId ?: "-"})"
            } catch (e: Exception) {
                _syncMessage.value = "上传失败：${e.message ?: "未知错误"}"
            }
        }
    }

    // 云端同步：手动下载
    fun downloadFromCloud() {
        viewModelScope.launch {
            try {
                val text = repository.downloadBackupFromOss()
                importBackupJson(text)
                _syncMessage.value = "下载并导入成功"
            } catch (e: com.alibaba.sdk.android.oss.ClientException) {
                _syncMessage.value = "下载失败：网络或本地错误：${e.message ?: "未知错误"}"
            } catch (e: com.alibaba.sdk.android.oss.ServiceException) {
                _syncMessage.value = "下载失败：${e.errorCode ?: "ServiceException"} (requestId=${e.requestId ?: "-"})"
            } catch (e: Exception) {
                _syncMessage.value = "下载失败：${e.message ?: "未知错误"}"
            }
        }
    }
    // 是否显示周末设置
    fun setShowWeekend(show: Boolean) {
        viewModelScope.launch {
            _showWeekend.value = show
            repository.saveShowWeekend(show)
        }
    }

    fun setShowSaturday(show: Boolean) {
        viewModelScope.launch {
            _showSaturday.value = show
            repository.saveShowSaturday(show)
        }
    }

    fun setShowSunday(show: Boolean) {
        viewModelScope.launch {
            _showSunday.value = show
            repository.saveShowSunday(show)
        }
    }

    fun setShowBreaks(show: Boolean) {
        viewModelScope.launch {
            _showBreaks.value = show
            repository.saveShowBreaks(show)
        }
    }

    // 新增：设置是否显示“再下节课”小卡片
    fun setShowNextCourseCard(show: Boolean) {
        viewModelScope.launch {
            _showNextCourseCard.value = show
            repository.saveShowNextCourseCard(show)
        }
    }

    // 新增：设置是否显示字段放大弹窗
    fun setShowFieldDialog(show: Boolean) {
        viewModelScope.launch {
            _showFieldDialog.value = show
            repository.saveShowFieldDialog(show)
        }
    }

    fun setHideInactiveCourses(hide: Boolean) {
        viewModelScope.launch {
            _hideInactiveCourses.value = hide
            repository.saveHideInactiveCourses(hide)
        }
    }

    // 新增：删除学期，同时维护选中项与相关课程/网络课数据
    fun deleteTerm(name: String) {
        viewModelScope.launch {
            val updatedTerms = _terms.value.toMutableList().apply { removeAll { it.name == name } }
            repository.saveTerms(updatedTerms)
            _terms.value = updatedTerms

            // 更新选中学期
            val newSelected = when {
                updatedTerms.any { it.name == _selectedTerm.value } -> _selectedTerm.value
                updatedTerms.isNotEmpty() -> updatedTerms.first().name
                else -> ""
            }
            _selectedTerm.value = newSelected
            repository.saveSelectedTerm(newSelected)

            // 清理被删除学期的课程与网络选修课数据
            val courses = repository.getCourses().toMutableMap()
            if (courses.remove(name) != null) {
                repository.saveCourses(courses)
            }
            val online = repository.getOnlineCourses().toMutableMap()
            if (online.remove(name) != null) {
                repository.saveOnlineCourses(online)
            }
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
