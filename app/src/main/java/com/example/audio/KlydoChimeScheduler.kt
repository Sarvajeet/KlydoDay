package com.example.audio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object KlydoChimeScheduler {
    private const val REQUEST_CODE = 4012

    fun syncSchedule(
        context: Context,
        enabled: Boolean,
        interval: ChimeInterval
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        if (!enabled || interval == ChimeInterval.OFF) return
        val nextAt = ChimeScheduleEvaluator.nextBoundaryMillis(System.currentTimeMillis(), interval)
            ?: return

        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent)
        } catch (_: SecurityException) {
            // Safe fallback on restricted profiles
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, KlydoChimeReceiver::class.java)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
