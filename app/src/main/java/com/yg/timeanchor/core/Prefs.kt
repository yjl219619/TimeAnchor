package com.yg.timeanchor.core

import android.content.Context
import android.content.SharedPreferences

/** 阶段 1 用 SharedPreferences；阶段 3 换 Room，接口保持不变。 */
object Prefs {
    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("timeanchor", Context.MODE_PRIVATE)

    // ---- 闹钟记录（阶段1只存一份简单清单：id|triggerAt|label|desc）----
    fun all(c: Context): List<Alarm> =
        (sp(c).getString("alarms", "") ?: "").lines().filter { it.isNotBlank() }.mapNotNull(Alarm::decode)

    fun save(c: Context, list: List<Alarm>) {
        sp(c).edit().putString("alarms", list.joinToString("\n") { it.encode() }).apply()
    }

    fun upsert(c: Context, a: Alarm) {
        val l = all(c).toMutableList()
        l.removeAll { it.id == a.id }
        l.add(a)
        save(c, l)
    }

    fun remove(c: Context, id: Int) {
        save(c, all(c).filterNot { it.id == id })
    }

    // ---- 自增 requestCode（需求 3：不能用时间戳）----
    fun nextId(c: Context): Int {
        val n = sp(c).getInt("nextId", 1)
        sp(c).edit().putInt("nextId", n + 1).apply()
        return n
    }

    // ---- 设置 ----
    fun maxRingMinutes(c: Context): Int = sp(c).getInt("maxRingMin", 0)   // 0 = 一直响
    fun setMaxRingMinutes(c: Context, m: Int) = sp(c).edit().putInt("maxRingMin", m).apply()

    // ---- 错过的闹钟 ----
    fun addMissed(c: Context, s: String) {
        val old = sp(c).getString("missed", "") ?: ""
        val cut = (listOf(s) + old.lines()).filter { it.isNotBlank() }.take(30)
        sp(c).edit().putString("missed", cut.joinToString("\n")).apply()
    }

    fun missed(c: Context): List<String> =
        (sp(c).getString("missed", "") ?: "").lines().filter { it.isNotBlank() }

    // ---- 日志（诊断用）----
    fun log(c: Context, tag: String, msg: String) {
        val old = sp(c).getString("log", "") ?: ""
        val line = "%tF %tT [%s] %s".format(System.currentTimeMillis(), System.currentTimeMillis(), tag, msg)
        sp(c).edit().putString("log", (line + "\n" + old).lines().take(200).joinToString("\n")).apply()
    }

    fun logs(c: Context): String = sp(c).getString("log", "") ?: ""
}

data class Alarm(val id: Int, val triggerAt: Long, val label: String, val desc: String) {
    fun encode() = "$id|$triggerAt|${label.replace("|", " ")}|${desc.replace("|", " ")}"
    companion object {
        fun decode(s: String): Alarm? {
            val p = s.split("|")
            if (p.size < 2) return null
            return Alarm(p[0].toIntOrNull() ?: return null, p[1].toLongOrNull() ?: return null,
                p.getOrElse(2) { "" }, p.getOrElse(3) { "" })
        }
    }
}
