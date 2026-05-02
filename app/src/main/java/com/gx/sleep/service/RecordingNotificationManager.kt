package com.gx.sleep.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gx.sleep.GxSleepApp
import com.gx.sleep.MainActivity
import com.gx.sleep.R

class RecordingNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.gx.sleep.ACTION_STOP_RECORDING"
    }

    fun buildInitNotification(): Notification {
        return buildBaseNotification(
            "正在准备睡眠记录",
            "正在初始化夜间声音监测"
        )
    }

    fun buildRecordingNotification(): Notification {
        return buildBaseNotification(
            context.getString(R.string.notification_recording_title),
            context.getString(R.string.notification_recording_text)
        )
    }

    fun buildCompletionNotification(): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, GxSleepApp.CHANNEL_ID)
            .setContentTitle("本次睡眠记录已完成")
            .setContentText("点击查看昨晚摘要")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
    }

    fun updateToRecordingNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildRecordingNotification())
    }

    private fun buildBaseNotification(title: String, text: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SleepRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, GxSleepApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_share,
                context.getString(R.string.notification_action_open),
                openPendingIntent
            )
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
