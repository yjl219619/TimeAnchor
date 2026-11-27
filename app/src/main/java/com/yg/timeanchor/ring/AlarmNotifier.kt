package com.yg.timeanchor.ring

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri

/** 通知兜底（需求 3）：全屏界面没弹出也必须能停止/稍后/完成。 */
object AlarmNotifier {
    const val CH_ALARM = "alarm_critical"
    const val CH_SERVICE = "ring_service"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH_ALARM) == null) {
            val c = NotificationChannel(CH_ALARM, "闹钟", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)                       // 声音由前台服务播放，避免双响
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)                         // 尽量穿透勿扰（系统仍可拒绝）
            }
            nm.createNotificationChannel(c)
        }
        if (nm.getNotificationChannel(CH_SERVICE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_SERVICE, "响铃服务", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun serviceNotification(ctx: Context, item: RingItem): Notification {
        val open = PendingIntent.getActivity(ctx, 0,
            Intent(ctx, com.yg.timeanchor.RingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("label", item.label); putExtra("desc", item.desc)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(ctx, CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(item.label)
            .setContentText(item.desc.ifEmpty { "正在响铃" })
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(action(ctx, item, "stop", "停止"))
            .addAction(action(ctx, item, "snooze", "稍后 10 分"))
            .addAction(action(ctx, item, "done", "完成"))
            .build()
    }

    private fun action(ctx: Context, item: RingItem, what: String, label: String): Notification.Action {
        val i = Intent(ctx, RingService::class.java).apply {
            action = ACTION_PREFIX + what
            putExtra("id", item.id); putExtra("label", item.label)
        }
        val pi = PendingIntent.getService(ctx, item.id * 10 + what.hashCode() % 10, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(null, label, pi).build()
    }

    fun ringNotification(ctx: Context, item: RingItem): Notification {
        val full = PendingIntent.getActivity(ctx, 100 + item.id,
            Intent(ctx, com.yg.timeanchor.RingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("label", item.label); putExtra("desc", item.desc)
                putExtra("triggerAt", item.triggerAt)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(ctx, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(item.label)
            .setContentText(item.desc.ifEmpty { "闹钟时间到了" })
            .setFullScreenIntent(full, true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(action(ctx, item, "stop", "停止"))
            .addAction(action(ctx, item, "snooze", "稍后 10 分"))
            .addAction(action(ctx, item, "done", "完成"))
            .build()
    }

    const val ACTION_PREFIX = "com.yg.timeanchor.action."
}
