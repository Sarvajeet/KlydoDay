package com.example.audio

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object KlydoChimeScheduler {
    private const val REQUEST_CODE = 4012
    const val ACTION_CHIME_ALARM = "com.example.audio.ACTION_CHIME_ALARM"
    const val EXTRA_TARGET_BOUNDARY_MILLIS = "extra_target_boundary_millis"

    @SuppressLint("MissingPermission")
    fun syncSchedule(
        context: Context,
        enabled: Boolean,
        interval: ChimeInterval,
        fromMillis: Long = System.currentTimeMillis()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cancelIntent = buildPendingIntent(context, targetBoundaryMillis = null)
        alarmManager.cancel(cancelIntent)

        if (!enabled || interval == ChimeInterval.OFF) return
        val nextAt = ChimeScheduleEvaluator.nextBoundaryMillis(fromMillis, interval)
            ?: return

        val pendingIntent = buildPendingIntent(context, targetBoundaryMillis = nextAt)
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent)
            }
        }
    }

    private fun buildPendingIntent(context: Context, targetBoundaryMillis: Long?): PendingIntent {
        val intent = Intent(context.applicationContext, KlydoChimeReceiver::class.java).apply {
            action = ACTION_CHIME_ALARM
            if (targetBoundaryMillis != null) {
                putExtra(EXTRA_TARGET_BOUNDARY_MILLIS, targetBoundaryMillis)
            }
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
