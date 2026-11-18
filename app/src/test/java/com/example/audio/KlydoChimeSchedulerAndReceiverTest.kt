package com.example.audio

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ClockSurfaceStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KlydoChimeSchedulerAndReceiverTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Test
    fun `syncSchedule schedules next boundary alarm and cancels when disabled`() {
        val fromMillis = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 18)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 12)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val expectedNext = ChimeScheduleEvaluator.nextBoundaryMillis(fromMillis, ChimeInterval.EVERY_15_MIN)!!
        KlydoChimeScheduler.syncSchedule(
            context = context,
            enabled = true,
            interval = ChimeInterval.EVERY_15_MIN,
            fromMillis = fromMillis
        )

        val shadowAlarm = shadowOf(alarmManager)
        val scheduled = requireNotNull(shadowAlarm.nextScheduledAlarm) {
            "Expected scheduled chime alarm"
        }
        assertEquals(expectedNext, scheduled.triggerAtTime)

        val pendingShadow = shadowOf(scheduled.operation)
        assertEquals(
            expectedNext,
            pendingShadow.savedIntent.getLongExtra(KlydoChimeScheduler.EXTRA_TARGET_BOUNDARY_MILLIS, -1L)
        )

        KlydoChimeScheduler.syncSchedule(
            context = context,
            enabled = false,
            interval = ChimeInterval.EVERY_15_MIN,
            fromMillis = fromMillis
        )
        assertNull(shadowAlarm.nextScheduledAlarm)
    }

    @Test
    fun `chime dedup slot blocks duplicate chimes in the same minute`() {
        val uniqueKey = "2026-11-18-10-15-${System.nanoTime()}"
        assertTrue(ChimePlayer.tryAcquireChimeSlot(context, uniqueKey))
        assertFalse(ChimePlayer.tryAcquireChimeSlot(context, uniqueKey))
        assertEquals(uniqueKey, ChimePlayer.lastPlayedDedupKey(context))
    }

    @Test
    fun `boot completed receiver reschedules chimes only when background chimes enabled`() = runBlocking {
        val store = ClockSurfaceStore(context)
        store.updateChimePreferences(
            chimeInterval = ChimeInterval.EVERY_1_HOUR,
            chimeInBackground = true
        )
        KlydoChimeScheduler.syncSchedule(context, enabled = false, interval = ChimeInterval.OFF)

        val receiver = KlydoChimeReceiver()
        receiver.handleReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNotNull(
            "BOOT_COMPLETED should schedule the upcoming hourly chime alarm",
            shadowOf(alarmManager).nextScheduledAlarm
        )

        store.updateChimePreferences(chimeInBackground = false)
        receiver.handleReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNull(
            "BOOT_COMPLETED should cancel alarms when background chimes are disabled",
            shadowOf(alarmManager).nextScheduledAlarm
        )
    }
}
