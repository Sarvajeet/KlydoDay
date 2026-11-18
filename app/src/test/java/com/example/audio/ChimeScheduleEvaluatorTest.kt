package com.example.audio

import com.example.data.model.DisplaySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ChimeScheduleEvaluatorTest {

    private fun millisAt(hour: Int, minute: Int, second: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 16)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `hourly interval triggers only at top of hour and deduplicates within minute`() {
        val settings = DisplaySettings(hourlyChimeEnabled = true, nightModeEnabled = false)
        val atTop = millisAt(14, 0, 1)
        val trigger = ChimeScheduleEvaluator.evaluate(atTop, ChimeInterval.EVERY_1_HOUR, settings, "")
        assertNotNull(trigger)
        assertEquals(ChimeTriggerKind.FULL_HOUR, trigger?.kind)
        assertEquals(2, trigger?.hour12)

        // Second check in same minute is deduplicated
        val duplicate = ChimeScheduleEvaluator.evaluate(atTop + 1000L, ChimeInterval.EVERY_1_HOUR, settings, trigger!!.dedupKey)
        assertNull(duplicate)

        // Half past hour does not trigger for 1-hour interval
        assertNull(ChimeScheduleEvaluator.evaluate(millisAt(14, 30, 0), ChimeInterval.EVERY_1_HOUR, settings, ""))
    }

    @Test
    fun `fifteen and thirty minute intervals distinguish full hour and sub-interval cues`() {
        val settings = DisplaySettings(hourlyChimeEnabled = true, nightModeEnabled = false)

        val quarter = ChimeScheduleEvaluator.evaluate(millisAt(10, 15, 0), ChimeInterval.EVERY_15_MIN, settings, "")
        assertEquals(ChimeTriggerKind.SUB_INTERVAL, quarter?.kind)

        val half = ChimeScheduleEvaluator.evaluate(millisAt(10, 30, 0), ChimeInterval.EVERY_30_MIN, settings, "")
        assertEquals(ChimeTriggerKind.SUB_INTERVAL, half?.kind)

        val noon = ChimeScheduleEvaluator.evaluate(millisAt(12, 0, 0), ChimeInterval.EVERY_15_MIN, settings, "")
        assertEquals(ChimeTriggerKind.FULL_HOUR, noon?.kind)
        assertEquals(12, noon?.hour12)
    }

    @Test
    fun `two hour interval fires only on even hours and respects night window`() {
        val settings = DisplaySettings(
            hourlyChimeEnabled = true,
            nightModeEnabled = true,
            nightStartMinutes = 1320L, // 22:00
            nightEndMinutes = 420L // 07:00
        )

        assertNotNull(ChimeScheduleEvaluator.evaluate(millisAt(14, 0, 0), ChimeInterval.EVERY_2_HOURS, settings, ""))
        assertNull(ChimeScheduleEvaluator.evaluate(millisAt(15, 0, 0), ChimeInterval.EVERY_2_HOURS, settings, ""))
        // 22:00 is inside night window -> silenced
        assertNull(ChimeScheduleEvaluator.evaluate(millisAt(22, 0, 0), ChimeInterval.EVERY_2_HOURS, settings, ""))
    }

    @Test
    fun `nextBoundaryMillis computes aligned upcoming timestamps`() {
        val at1423 = millisAt(14, 23, 15)
        assertEquals(millisAt(14, 30, 0), ChimeScheduleEvaluator.nextBoundaryMillis(at1423, ChimeInterval.EVERY_15_MIN))
        assertEquals(millisAt(15, 0, 0), ChimeScheduleEvaluator.nextBoundaryMillis(at1423, ChimeInterval.EVERY_1_HOUR))
        assertEquals(millisAt(16, 0, 0), ChimeScheduleEvaluator.nextBoundaryMillis(at1423, ChimeInterval.EVERY_2_HOURS))
    }

    @Test
    fun `evaluate tolerates coroutine jitter inside boundary minute and deduplicates`() {
        val settings = DisplaySettings(hourlyChimeEnabled = true, nightModeEnabled = false)
        val delayedTick = millisAt(15, 0, 12)
        val decision = ChimeScheduleEvaluator.evaluate(delayedTick, ChimeInterval.EVERY_1_HOUR, settings, "")
        assertNotNull(decision)
        assertEquals(ChimeTriggerKind.FULL_HOUR, decision?.kind)
        assertEquals(3, decision?.hour12)
        assertNull(ChimeScheduleEvaluator.evaluate(delayedTick + 2000L, ChimeInterval.EVERY_1_HOUR, settings, decision!!.dedupKey))
    }

    @Test
    fun `evaluateDelayedBoundary preserves boundary kind under Doze delay and skips stale or night alarms`() {
        val settings = DisplaySettings(
            hourlyChimeEnabled = true,
            nightModeEnabled = true,
            nightStartMinutes = 1320L,
            nightEndMinutes = 420L
        )

        val targetQuarter = millisAt(14, 45, 0)
        val delayedQuarterNow = millisAt(14, 57, 10)
        val quarterDecision = ChimeScheduleEvaluator.evaluateDelayedBoundary(
            targetBoundaryMillis = targetQuarter,
            nowMillis = delayedQuarterNow,
            interval = ChimeInterval.EVERY_15_MIN,
            settings = settings,
            lastChimeKey = ""
        )
        assertNotNull(quarterDecision)
        assertEquals(ChimeTriggerKind.SUB_INTERVAL, quarterDecision?.kind)

        val targetHour = millisAt(15, 0, 0)
        val delayedHourNow = millisAt(15, 4, 20)
        val hourDecision = ChimeScheduleEvaluator.evaluateDelayedBoundary(
            targetBoundaryMillis = targetHour,
            nowMillis = delayedHourNow,
            interval = ChimeInterval.EVERY_1_HOUR,
            settings = settings,
            lastChimeKey = ""
        )
        assertNotNull(hourDecision)
        assertEquals(ChimeTriggerKind.FULL_HOUR, hourDecision?.kind)
        assertEquals(3, hourDecision?.hour12)

        // Stale alarm > 35 min late is ignored
        assertNull(
            ChimeScheduleEvaluator.evaluateDelayedBoundary(
                targetBoundaryMillis = millisAt(12, 0, 0),
                nowMillis = millisAt(13, 0, 0),
                interval = ChimeInterval.EVERY_1_HOUR,
                settings = settings,
                lastChimeKey = ""
            )
        )

        // Night mode suppresses delayed alarm
        assertNull(
            ChimeScheduleEvaluator.evaluateDelayedBoundary(
                targetBoundaryMillis = millisAt(22, 0, 0),
                nowMillis = millisAt(22, 2, 0),
                interval = ChimeInterval.EVERY_1_HOUR,
                settings = settings,
                lastChimeKey = ""
            )
        )
    }
}
