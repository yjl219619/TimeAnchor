package com.yg.timeanchor.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.yg.timeanchor.MainActivity

/**
 * setAlarmClock：系统时钟图标可见、Doze 下最可靠，且不需要精确闹钟权限。
 * 每条闹钟独立 PendingIntent（requestCode = 自增 id）。
 */
object AlarmScheduler {

    fun register(ctx: Context, a: Alarm): Boolean {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (a.triggerAt <= System.currentTimeMillis()) {
            Prefs.log(ctx, "sched", "拒绝注册过去时间 id=${a.id}")
            return false
        }
        val info = AlarmManager.AlarmClockInfo(a.triggerAt, showIntent(ctx, a))
        return try {
            am.setAlarmClock(info, operation(ctx, a))
            Prefs.upsert(ctx, a)
            Prefs.log(ctx, "sched", "注册 id=${a.id} at=${a.triggerAt}")
            true
        } catch (e: Exception) {
            Prefs.log(ctx, "sched", "注册失败 id=${a.id} ${e.javaClass.simpleName}")
            false
        }
    }

    fun cancel(ctx: Context, a: Alarm) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(operation(ctx, a))
        Prefs.remove(ctx, a.id)
        Prefs.log(ctx, "sched", "取消 id=${a.id}")
    }

    /** 开机/更新/改时间：只重建未来闹钟，不在这里启动播放。 */
    fun rebuildAll(ctx: Context) {
        val now = System.currentTimeMillis()
        var n = 0
        Prefs.all(ctx).forEach { a ->
            if (a.triggerAt > now) {
                register(ctx, a)
                n++
            } else {
                Prefs.remove(ctx, a.id)
                Prefs.addMissed(ctx, "missed id=${a.id} ${a.label} at=${a.triggerAt}")
            }
        }
        Prefs.log(ctx, "sched", "rebuild 完成，重建 $n 条")
    }

    /** 稍后提醒：注册一个新的真闹钟（不是普通通知）。 */
    fun snooze(ctx: Context, a: Alarm, minutes: Int): Alarm {
        val id = Prefs.nextId(ctx)
        val na = Alarm(id, System.currentTimeMillis() + minutes * 60_000L,
            a.label, "稍后提醒 +${minutes}分")
        register(ctx, na)
        return na
    }

    private fun operation(ctx: Context, a: Alarm): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).apply {
            data = Uri.parse("timeanchor://alarm/${a.id}")   // 保证 PendingIntent 唯一
            putExtra("id", a.id)
            putExtra("label", a.label)
            putExtra("desc", a.desc)
            putExtra("triggerAt", a.triggerAt)
        }
        return PendingIntent.getBroadcast(ctx, a.id, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun showIntent(ctx: Context, a: Alarm): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            data = Uri.parse("timeanchor://show/${a.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(ctx, a.id, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
