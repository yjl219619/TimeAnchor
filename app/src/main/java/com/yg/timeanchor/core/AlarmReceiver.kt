package com.yg.timeanchor.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yg.timeanchor.ring.RingQueue
import com.yg.timeanchor.ring.RingService

/** 闹钟到点：只负责把请求交给前台服务，播放逻辑全在服务里。 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val label = intent.getStringExtra("label") ?: "闹钟"
        val desc = intent.getStringExtra("desc") ?: ""
        val at = intent.getLongExtra("triggerAt", System.currentTimeMillis())

        Prefs.log(ctx, "fire", "到点 id=$id label=$label")
        Prefs.remove(ctx, id)
        RingQueue.push(RingItem(id, label, desc, at))

        val svc = Intent(ctx, RingService::class.java).apply {
            putExtra("id", id); putExtra("label", label)
            putExtra("desc", desc); putExtra("triggerAt", at)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc)
            else ctx.startService(svc)
            Prefs.log(ctx, "svc", "startForegroundService 已提交 id=$id")
        } catch (e: Exception) {
            // 需求 5：记录前台服务是否被拒绝
            Prefs.log(ctx, "svc", "启动前台服务被拒：${e.javaClass.simpleName} ${e.message}")
            Prefs.addMissed(ctx, "svc-denied id=$id ${e.javaClass.simpleName}")
        }
    }
}
