package com.yg.timeanchor.diag

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.yg.timeanchor.core.Prefs

data class Check(
    val name: String,
    val ok: Boolean?,          // null = 无法检测，必须人工确认
    val detail: String,
    val action: Intent? = null
)

object Diagnostics {

    fun run(ctx: Context): List<Check> {
        val out = ArrayList<Check>()
        val pkg = ctx.packageName

        // 1 通知权限
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifOn = nm.areNotificationsEnabled()
        out.add(Check("通知权限", notifOn,
            if (notifOn) "已开启" else "未开启，任何提醒都不会出现",
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)))

        // 2 全屏通知（Android 14+ 需单独授权）
        if (Build.VERSION.SDK_INT >= 34) {
            val ok = nm.canUseFullScreenIntent()
            out.add(Check("全屏通知", ok,
                if (ok) "已允许" else "未允许：锁屏时不会自动弹出全屏界面（声音仍会响）",
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg"))))
        } else {
            out.add(Check("全屏通知", true, "系统版本不需要单独授权", null))
        }

        // 3 闹钟音量
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        out.add(Check("闹钟音量 > 0", cur > 0,
            "当前 $cur/$max（这一档独立于媒体音量）",
            Intent(Settings.ACTION_SOUND_SETTINGS)))

        // 4 勿扰状态
        val filter = nm.currentInterruptionFilter
        val dndOff = filter == NotificationManager.INTERRUPTION_FILTER_ALL
        out.add(Check("勿扰模式", dndOff,
            when (filter) {
                NotificationManager.INTERRUPTION_FILTER_ALL -> "已关闭"
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "仅允许优先项（可能压掉提醒）"
                NotificationManager.INTERRUPTION_FILTER_NONE -> "完全静音（一定会压掉提醒）"
                else -> "勿扰已开启"
            },
            Intent("android.settings.ZEN_MODE_ACCESS_SETTINGS")))

        // 5 通知"覆盖勿扰"（仅能给出入口，无法读）
        out.add(Check("通知类别可覆盖勿扰", null,
            "请手动确认：应用通知 → Reminders/Critical 类别 → 覆盖勿扰（MIUI 可能未提供）",
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)))

        // 6 电池无限制
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val whitelisted = pm.isIgnoringBatteryOptimizations(pkg)
        out.add(Check("电池：无限制", whitelisted,
            if (whitelisted) "已在白名单" else "未加白：夜间可能被冻结，闹钟延后",
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))

        // 7 精确闹钟权限（setAlarmClock 不需要，但记录下来供参考）
        val alm = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31) {
            val can = alm.canScheduleExactAlarms()
            out.add(Check("精确闹钟权限（参考项）", can,
                if (can) "已授予" else "未授予；本应用用 setAlarmClock，理论上仍可准点，需要实测",
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg"))))
        }

        // 7.5 悬浮窗权限（第三层接管）
        val canOverlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)
        out.add(Check("悬浮窗权限（接管前台应用）", canOverlay,
            if (canOverlay) "已授予" else "未授予：刷抖音时只能弹横幅，无法整屏覆盖",
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$pkg"))))

        // 8 小米专有：自启动 / 后台弹出 / 锁屏显示 —— 无法检测
        out.add(Check("小米：后台弹出界面（接管关键）", null,
            "请手动确认：设置 → 应用管理 → 时间锚点 → 其他权限 → 后台弹出界面 → 允许。这一项不开，悬浮窗会被系统拦。", null))
        out.add(Check("小米：自启动 / 锁屏显示", null,
            "请手动确认两项都允许：设置 → 应用管理 → 时间锚点 → 权限", null))

        // 9 已注册闹钟数 / 日志
        out.add(Check("已注册闹钟", Prefs.all(ctx).isNotEmpty(),
            "共 ${Prefs.all(ctx).size} 条；错过 ${Prefs.missed(ctx).size} 条", null))

        return out
    }
}
