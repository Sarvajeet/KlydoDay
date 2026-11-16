package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ChimePlayer
import com.example.data.KlydoDataSource
import com.example.data.firestore.KlydoRepository
import com.example.data.model.DisplaySettings
import com.example.data.model.KlydoArtCatalog
import com.example.data.model.PendulumStyle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class KlydoViewModel(
    private val repository: KlydoDataSource,
    private val currentUserId: String
) : ViewModel() {

    val scheduleBlocksState: StateFlow<UiState<List<ScheduleBlock>>> =
        repository.observeScheduleBlocks(currentUserId)
            .map<List<ScheduleBlock>, UiState<List<ScheduleBlock>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing schedule blocks", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load schedule"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val displaySettingsState: StateFlow<DisplaySettings> =
        repository.observeDisplaySettings(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing display settings", firestoreError ?: error)
                emit(DisplaySettings(id = KlydoRepository.SETTINGS_DOC_ID, userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DisplaySettings(id = KlydoRepository.SETTINGS_DOC_ID, userId = currentUserId)
            )

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _scrubOffsetMinutes = MutableStateFlow(0f)
    val scrubOffsetMinutes: StateFlow<Float> = _scrubOffsetMinutes.asStateFlow()

    private val _selectedDayCode = MutableStateFlow(currentDayCode(System.currentTimeMillis()))
    val selectedDayCode: StateFlow<String> = _selectedDayCode.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _availableFaceIds = MutableStateFlow(KlydoArtCatalog.all.map { it.id })

    private var lastChimedHourKey: String = ""
    private var lastChimedBlockKey: String = ""
    private var lastAutoCycleMinuteMark: Long = -1L

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                evaluateChimesAndCycle(now)
                delay(1000L)
            }
        }
    }

    private fun evaluateChimesAndCycle(now: Long) {
        val settings = displaySettingsState.value
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val minuteOfDay = hour * 60 + minute
        val dateKey = formatDateKey(cal)
        val dayCode = dayCodeFromCalendar(cal)

        val isNight = isMinuteInNightWindow(minuteOfDay, settings)
        if (!isNight && second <= 1) {
            val hourKey = "$dateKey-$hour"
            if (minute == 0 && settings.hourlyChimeEnabled && hourKey != lastChimedHourKey) {
                lastChimedHourKey = hourKey
                ChimePlayer.playHourlyChime()
            }

            if (settings.blockChimeEnabled) {
                val blocks = (scheduleBlocksState.value as? UiState.Success)?.data.orEmpty()
                val startingBlock = blocks.firstOrNull { block ->
                    block.chimeEnabled &&
                        block.isActiveForDay(dayCode) &&
                        block.startMinutes.toInt() == minuteOfDay
                }
                if (startingBlock != null) {
                    val blockKey = "$dateKey-${startingBlock.id}-$minuteOfDay"
                    if (blockKey != lastChimedBlockKey) {
                        lastChimedBlockKey = blockKey
                        ChimePlayer.playBlockStartChime()
                    }
                }
            }
        }

        if (settings.autoCycleArt) {
            val totalMinutes = now / 60_000L
            val interval = settings.cycleIntervalMinutes.coerceAtLeast(15L)
            val bucket = totalMinutes / interval
            if (lastAutoCycleMinuteMark == -1L) {
                lastAutoCycleMinuteMark = bucket
            } else if (bucket != lastAutoCycleMinuteMark) {
                lastAutoCycleMinuteMark = bucket
                val catalogIds = _availableFaceIds.value.ifEmpty { KlydoArtCatalog.all.map { it.id } }
                val currentIndex = catalogIds.indexOfFirst { it == settings.activeKlydoId }
                val nextId = catalogIds[(currentIndex + 1) % catalogIds.size]
                selectKlydo(nextId)
            }
        }
    }

    fun updateAvailableFaceIds(ids: List<String>) {
        if (ids.isNotEmpty()) {
            _availableFaceIds.value = ids
        }
    }

    fun adjustScrubMinutes(deltaMinutes: Float) {
        val next = (_scrubOffsetMinutes.value + deltaMinutes).coerceIn(-720f, 720f)
        _scrubOffsetMinutes.value = next
    }

    fun setScrubMinutes(offsetMinutes: Float) {
        _scrubOffsetMinutes.value = offsetMinutes.coerceIn(-720f, 720f)
    }

    fun snapToLiveTime() {
        _scrubOffsetMinutes.value = 0f
    }

    fun selectDayCode(dayCode: String) {
        _selectedDayCode.value = dayCode
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun saveBlock(block: ScheduleBlock) {
        viewModelScope.launch {
            val result = if (block.id.isBlank()) {
                repository.createScheduleBlock(block)
            } else {
                repository.updateScheduleBlock(block)
            }
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Could not save schedule block"
            }
        }
    }

    fun toggleBlockCompletedToday(block: ScheduleBlock) {
        viewModelScope.launch {
            val todayKey = currentDateKey()
            val nextKey = if (block.isCompletedOn(todayKey)) "" else todayKey
            val result = repository.toggleBlockCompleted(block.id, nextKey)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Could not update block status"
            }
        }
    }

    fun deleteBlock(blockId: String) {
        viewModelScope.launch {
            val result = repository.deleteScheduleBlock(blockId)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Could not delete block"
            }
        }
    }

    fun addStarterRoutine() {
        viewModelScope.launch {
            val starterBlocks = listOf(
                ScheduleBlock(
                    title = "Morning Ritual & Espresso",
                    subtitle = "Sunlit table • Hydrate & plan",
                    startMinutes = 450L, // 07:30
                    durationMinutes = 45L,
                    category = ScheduleCategory.WELLNESS.key,
                    colorHex = ScheduleCategory.WELLNESS.defaultColorHex,
                    chimeEnabled = true
                ),
                ScheduleBlock(
                    title = "Deep Studio Focus",
                    subtitle = "Notifications paused • Priority project",
                    startMinutes = 540L, // 09:00
                    durationMinutes = 150L,
                    category = ScheduleCategory.FOCUS.key,
                    colorHex = ScheduleCategory.FOCUS.defaultColorHex,
                    chimeEnabled = true
                ),
                ScheduleBlock(
                    title = "Midday Lunch & Walk",
                    subtitle = "Step outside for daylight",
                    startMinutes = 750L, // 12:30
                    durationMinutes = 60L,
                    category = ScheduleCategory.MEAL.key,
                    colorHex = ScheduleCategory.MEAL.defaultColorHex,
                    chimeEnabled = true
                ),
                ScheduleBlock(
                    title = "Creative & Admin Sprint",
                    subtitle = "Atelier desk • Wraps & reviews",
                    startMinutes = 870L, // 14:30
                    durationMinutes = 120L,
                    category = ScheduleCategory.CREATIVE.key,
                    colorHex = ScheduleCategory.CREATIVE.defaultColorHex,
                    chimeEnabled = true
                ),
                ScheduleBlock(
                    title = "Family Dinner & Wind Down",
                    subtitle = "Kitchen & living room",
                    startMinutes = 1140L, // 19:00
                    durationMinutes = 90L,
                    category = ScheduleCategory.FAMILY.key,
                    colorHex = ScheduleCategory.FAMILY.defaultColorHex,
                    chimeEnabled = true
                )
            )
            var hadError = false
            for (block in starterBlocks) {
                val res = repository.createScheduleBlock(block)
                if (res.isFailure) hadError = true
            }
            if (hadError) {
                _statusBanner.value = "Some starter blocks could not be saved."
            }
        }
    }

    fun selectKlydo(klydoId: String) {
        val current = displaySettingsState.value
        saveDisplaySettings(
            current.copy(
                activeKlydoId = klydoId,
                pendulumStyle = PendulumStyle.OBSIDIAN.key
            )
        )
    }

    fun toggleFavoriteKlydo(klydoId: String) {
        val current = displaySettingsState.value
        val updated = if (current.favoriteKlydoIds.contains(klydoId)) {
            current.favoriteKlydoIds.filterNot { it == klydoId }
        } else {
            (current.favoriteKlydoIds + klydoId).take(20)
        }
        saveDisplaySettings(current.copy(favoriteKlydoIds = updated))
    }

    fun selectPendulumStyle(style: PendulumStyle) {
        val current = displaySettingsState.value
        saveDisplaySettings(current.copy(pendulumStyle = style.key))
    }

    fun setShowFrame(showFrame: Boolean) {
        val current = displaySettingsState.value
        saveDisplaySettings(current.copy(showFrame = showFrame))
    }

    fun saveDisplaySettings(settings: DisplaySettings) {
        viewModelScope.launch {
            val result = repository.saveDisplaySettings(settings)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save display settings"
            }
        }
    }

    fun testChime() {
        ChimePlayer.playHourlyChime()
    }

    companion object {
        private const val TAG = "KlydoVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        val WEEKDAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

        fun currentDateKey(nowMillis: Long = System.currentTimeMillis()): String {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return formatDateKey(cal)
        }

        private fun formatDateKey(cal: Calendar): String =
            String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )

        fun currentDayCode(nowMillis: Long = System.currentTimeMillis()): String {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            return dayCodeFromCalendar(cal)
        }

        fun dayCodeFromCalendar(cal: Calendar): String = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "MON"
        }

        fun isMinuteInNightWindow(minuteOfDay: Int, settings: DisplaySettings): Boolean {
            if (!settings.nightModeEnabled) return false
            val start = settings.nightStartMinutes.toInt()
            val end = settings.nightEndMinutes.toInt()
            return if (start <= end) {
                minuteOfDay in start until end
            } else {
                minuteOfDay >= start || minuteOfDay < end
            }
        }

        fun formatMinutesOfDay(minuteOfDay: Int, use24h: Boolean): String {
            val norm = ((minuteOfDay % 1440) + 1440) % 1440
            val hour = norm / 60
            val min = norm % 60
            return if (use24h) {
                String.format(Locale.US, "%02d:%02d", hour, min)
            } else {
                val period = if (hour < 12) "AM" else "PM"
                val h12 = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format(Locale.US, "%d:%02d %s", h12, min, period)
            }
        }
    }
}
