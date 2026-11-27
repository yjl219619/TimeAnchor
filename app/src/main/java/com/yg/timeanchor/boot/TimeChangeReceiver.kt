package com.yg.timeanchor.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yg.timeanchor.core.AlarmScheduler
import com.yg.timeanchor.core.Prefs

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Prefs.log(ctx, "time", "时间/时区变化：${intent.action}")
        AlarmScheduler.rebuildAll(ctx)
    }
}
