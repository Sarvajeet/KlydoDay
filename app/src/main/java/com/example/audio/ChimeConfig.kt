package com.example.audio

import com.example.data.model.DisplaySettings
import com.example.ui.viewmodel.KlydoViewModel
import java.util.Calendar
import java.util.Locale

enum class ChimeInterval(
    val key: String,
    val label: String,
    val shortLabel: String,
    val minutes: Int
) {
    OFF("off", "Off", "Off", 0),
    EVERY_15_MIN("15m", "Every 15 min", "15m", 15),
    EVERY_30_MIN("30m", "Every 30 min", "30m", 30),
    EVERY_1_HOUR("1h", "Every 1 hour", "1h", 60),
    EVERY_2_HOURS("2h", "Every 2 hours", "2h", 120),
    EVERY_4_HOURS("4h", "Every 4 hours", "4h", 240);

    companion object {
        fun fromKey(key: String?): ChimeInterval =
            entries.firstOrNull { it.key == key } ?: EVERY_1_HOUR
    }
}

enum class ChimeTone(val key: String, val label: String) {
    WESTMINSTER("westminster", "Westminster"),
    CRYSTAL_BELL("crystal", "Crystal Bell"),
    WARM_MARIMBA("marimba", "Soft Bamboo"),
    TEMPLE_BOWL("bowl", "Temple Bowl"),
    CUCKOO_CHIRP("cuckoo", "Clock Cuckoo");

    companion object {
        fun fromKey(key: String?): ChimeTone =
            entries.firstOrNull { it.key == key } ?: WESTMINSTER
    }
}

data class CustomChimeSound(
    val id: String,
    val title: String,
    val audioFileName: String,
    val durationMs: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class ChimeTriggerKind {
    FULL_HOUR,
    SUB_INTERVAL
}

data class ChimeTriggerDecision(
    val kind: ChimeTriggerKind,
    val dedupKey: String,
    val hour12: Int
)

object ChimeScheduleEvaluator {

    fun effectiveInterval(
        hourlyChimeEnabled: Boolean,
        configuredInterval: ChimeInterval
    ): ChimeInterval {
        if (!hourlyChimeEnabled) return ChimeInterval.OFF
        return if (configuredInterval == ChimeInterval.OFF) ChimeInterval.EVERY_1_HOUR else configuredInterval
    }

    fun evaluate(
        nowMillis: Long,
        interval: ChimeInterval,
        settings: DisplaySettings,
        lastChimeKey: String
    ): ChimeTriggerDecision? {
        if (interval == ChimeInterval.OFF || interval.minutes <= 0) return null

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val minuteOfDay = hour24 * 60 + minute

        if (KlydoViewModel.isMinuteInNightWindow(minuteOfDay, settings)) return null
        if (second > 2) return null

        val matchesBoundary = if (interval.minutes < 60) {
            minute % interval.minutes == 0
        } else {
            val hourStep = (interval.minutes / 60).coerceAtLeast(1)
            minute == 0 && (hour24 % hourStep == 0)
        }
        if (!matchesBoundary) return null

        val dedupKey = String.format(
            Locale.US,
            "%04d-%02d-%02d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            hour24,
            minute
        )
        if (dedupKey == lastChimeKey) return null

        val hour12 = ((hour24 % 12).let { if (it == 0) 12 else it })
        val kind = if (minute == 0) ChimeTriggerKind.FULL_HOUR else ChimeTriggerKind.SUB_INTERVAL
        return ChimeTriggerDecision(kind = kind, dedupKey = dedupKey, hour12 = hour12)
    }

    fun nextBoundaryMillis(nowMillis: Long, interval: ChimeInterval): Long? {
        if (interval == ChimeInterval.OFF || interval.minutes <= 0) return null
        val stepMillis = interval.minutes * 60_000L
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return if (interval.minutes < 60) {
            val curMin = cal.get(Calendar.MINUTE)
            val nextMin = ((curMin / interval.minutes) + 1) * interval.minutes
            if (nextMin >= 60) {
                cal.set(Calendar.MINUTE, 0)
                cal.add(Calendar.HOUR_OF_DAY, 1)
            } else {
                cal.set(Calendar.MINUTE, nextMin)
            }
            cal.timeInMillis
        } else {
            val hourStep = (interval.minutes / 60).coerceAtLeast(1)
            val curHour = cal.get(Calendar.HOUR_OF_DAY)
            val nextHour = ((curHour / hourStep) + 1) * hourStep
            cal.set(Calendar.MINUTE, 0)
            if (nextHour >= 24) {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, nextHour)
            }
            cal.timeInMillis.coerceAtLeast(nowMillis + stepMillis.coerceAtMost(60_000L))
        }
    }
}
