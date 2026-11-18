package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ClockSurfaceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KlydoChimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReceive(appContext, intent)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    internal suspend fun handleReceive(appContext: Context, intent: Intent?) {
        val action = intent?.action
        val targetBoundaryMillis = intent
            ?.getLongExtra(KlydoChimeScheduler.EXTRA_TARGET_BOUNDARY_MILLIS, -1L)
            ?.takeIf { it > 0L }

        val surfaceStore = ClockSurfaceStore(appContext)
        val state = surfaceStore.surfaceFlow.first()
        val effectiveInterval = ChimeScheduleEvaluator.effectiveInterval(
            hourlyChimeEnabled = state.hourlyChimeEnabled,
            configuredInterval = state.chimeInterval
        )
        if (!state.chimeInBackground || effectiveInterval == ChimeInterval.OFF) {
            KlydoChimeScheduler.syncSchedule(appContext, enabled = false, interval = ChimeInterval.OFF)
            return
        }

        val isRescheduleOnly = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED

        val nowMillis = System.currentTimeMillis()
        if (!isRescheduleOnly) {
            val settings = state.toDisplaySettingsSnapshot()
            val decision = ChimeScheduleEvaluator.evaluateDelayedBoundary(
                targetBoundaryMillis = targetBoundaryMillis,
                nowMillis = nowMillis,
                interval = effectiveInterval,
                settings = settings,
                lastChimeKey = ChimePlayer.lastPlayedDedupKey(appContext)
            )
            if (decision != null && ChimePlayer.tryAcquireChimeSlot(appContext, decision.dedupKey)) {
                val isSubInterval = decision.kind == ChimeTriggerKind.SUB_INTERVAL
                val strikes = if (!isSubInterval && state.strikeHourCount) decision.hour12 else 0
                val customFile = surfaceStore.resolveActiveCustomChimeFile(state)
                ChimePlayer.playChimeSync(
                    tone = state.chimeTone,
                    subInterval = isSubInterval,
                    strikeHourCount = strikes,
                    customFile = customFile,
                    context = appContext,
                    maxTotalDurationMs = RECEIVER_MAX_AUDIO_MS
                )
            }
        }

        val scheduleFrom = maxOf(nowMillis, (targetBoundaryMillis ?: nowMillis) + 1_000L)
        KlydoChimeScheduler.syncSchedule(
            context = appContext,
            enabled = true,
            interval = effectiveInterval,
            fromMillis = scheduleFrom
        )
    }

    companion object {
        // Stay safely below Android's 10s BroadcastReceiver goAsync() timeout
        private const val RECEIVER_MAX_AUDIO_MS = 8_200L
    }
}
