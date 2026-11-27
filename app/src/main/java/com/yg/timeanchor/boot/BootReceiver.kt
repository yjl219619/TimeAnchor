package com.yg.timeanchor.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yg.timeanchor.core.AlarmScheduler
import com.yg.timeanchor.core.Prefs

/** 开机 / 应用更新：只重建未来闹钟，绝不在这里启动播放（需求 1）。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Prefs.log(ctx, "boot", "收到 ${intent.action}")
        AlarmScheduler.rebuildAll(ctx)
    }
}
