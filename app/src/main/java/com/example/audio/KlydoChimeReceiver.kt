package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ClockSurfaceStore
import com.example.ui.viewmodel.KlydoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class KlydoChimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val surfaceStore = ClockSurfaceStore(appContext)
                val state = surfaceStore.surfaceFlow.first()
                val effectiveInterval = ChimeScheduleEvaluator.effectiveInterval(
                    hourlyChimeEnabled = state.hourlyChimeEnabled,
                    configuredInterval = state.chimeInterval
                )
                if (!state.chimeInBackground || effectiveInterval == ChimeInterval.OFF) {
                    KlydoChimeScheduler.syncSchedule(appContext, enabled = false, interval = ChimeInterval.OFF)
                    return@launch
                }

                val nowMillis = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                val hour24 = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                val minuteOfDay = hour24 * 60 + minute
                val settings = state.toDisplaySettingsSnapshot()

                if (!KlydoViewModel.isMinuteInNightWindow(minuteOfDay, settings)) {
                    val isSubInterval = effectiveInterval.minutes < 60 && minute in 10..55
                    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
                    val strikes = if (!isSubInterval && state.strikeHourCount) hour12 else 0
                    val customFile = surfaceStore.resolveActiveCustomChimeFile(state)
                    ChimePlayer.playChimeSync(
                        tone = state.chimeTone,
                        subInterval = isSubInterval,
                        strikeHourCount = strikes,
                        customFile = customFile
                    )
                }

                KlydoChimeScheduler.syncSchedule(
                    context = appContext,
                    enabled = true,
                    interval = effectiveInterval
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
