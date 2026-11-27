package com.yg.timeanchor

import android.content.Context
import android.os.Build
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/** 一条日程。at = 当天 时:分 */
data class Item(val at: String, val what: String, val detail: String)

object Day {
    private const val PATH = "/storage/emulated/0/paisms/day.json"

    fun canReadAll(): Boolean =
        Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    fun todayKey(): String {
        val c = Calendar.getInstance()
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH))
    }

    fun load(ctx: Context): List<Item> {
        val f = File(PATH)
        if (!f.exists()) { Prefs.log(ctx, "day", "找不到 $PATH"); return emptyList() }
        if (!canReadAll()) { Prefs.log(ctx, "day", "缺「所有文件访问」权限"); return emptyList() }
        return try {
            val days = JSONObject(f.readText()).optJSONObject("days") ?: return emptyList()
            val arr = days.optJSONArray(todayKey()) ?: return emptyList()
            val out = ArrayList<Item>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Item(o.optString("start"), o.optString("what"), o.optString("detail")))
            }
            Prefs.log(ctx, "day", "读到 ${out.size} 条日程")
            out
        } catch (e: Exception) {
            Prefs.log(ctx, "day", "解析失败：${e.javaClass.simpleName}")
            emptyList()
        }
    }

    /** 把今天的日程注册成闹钟，跳过已经过去的时刻。返回注册条数。 */
    fun registerToday(ctx: Context): Int {
        val now = Calendar.getInstance()
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        var n = 0
        load(ctx).forEach { it ->
            val p = it.at.split(":")
            if (p.size != 2) return@forEach
            val h = p[0].toIntOrNull() ?: return@forEach
            val m = p[1].toIntOrNull() ?: return@forEach
            if (h * 60 + m <= nowMin) return@forEach
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val a = com.yg.timeanchor.core.Alarm(
                com.yg.timeanchor.core.Prefs.nextId(ctx), cal.timeInMillis, it.what, it.detail)
            if (com.yg.timeanchor.core.AlarmScheduler.register(ctx, a)) n++
        }
        Prefs.log(ctx, "day", "本次注册 $n 条")
        return n
    }
}
