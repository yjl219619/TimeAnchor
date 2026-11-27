package com.yg.timeanchor

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.yg.timeanchor.core.*
import com.yg.timeanchor.diag.DiagnosticsActivity
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var nameBox: EditText
    private lateinit var minBox: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        com.yg.timeanchor.ring.AlarmNotifier.ensureChannels(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 56, 36, 36)
            setBackgroundColor(Color.parseColor("#0F1419"))
        }

        root.addView(tv("时间锚点 · 阶段1", 22f, Color.WHITE))
        root.addView(tv("到点真正响铃，压过正在播放的媒体", 13f, Color.parseColor("#8B97A5")).also {
            it.setPadding(0, 6, 0, 26)
        })

        nameBox = EditText(this).apply {
            hint = "名称，例如：工图作业补完"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        minBox = EditText(this).apply {
            hint = "多少分钟后响（测试用，例如 1）"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        root.addView(nameBox)
        root.addView(minBox)

        root.addView(row(
            btn("注册闹钟") { addAlarm(minutes = minBox.text.toString().toIntOrNull() ?: 1) },
            btn("测试 30 秒") { addAlarm(minutes = 0, seconds = 30, name = "测试闹钟") }
        ))
        root.addView(row(
            btn("所有文件权限") {
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            android.net.Uri.parse("package:$packageName")))
                    } catch (e: Exception) {
                        startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
            },
            btn("悬浮窗权限") {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")))
            },
            btn("诊断页") { startActivity(Intent(this, DiagnosticsActivity::class.java)) },
            btn("按日程重排") {
                val n = Day.registerToday(this)
                Toast.makeText(this, "注册 $n 条", Toast.LENGTH_SHORT).show(); render()
            },
            btn("取消全部") { Prefs.all(this).forEach { AlarmScheduler.cancel(this, it) }; render() }
        ))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sv = ScrollView(this).apply { addView(root) }
        root.addView(tv("已注册闹钟", 15f, Color.WHITE).also { it.setPadding(0, 28, 0, 8) })
        root.addView(list)
        setContentView(sv)

        requestNotificationPermission()
        render()
        // 打开即按今天日程重排（需「所有文件访问」权限）
        if (Day.canReadAll()) {
            val n = Day.registerToday(this)
            if (n > 0) Toast.makeText(this, "已按日程注册 $n 条", Toast.LENGTH_SHORT).show()
            render()
        }
    }

    private fun addAlarm(minutes: Int, seconds: Int = 0, name: String? = null) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutes)
            if (seconds > 0) add(Calendar.SECOND, seconds)
        }
        val label = name ?: nameBox.text.toString().ifBlank { "闹钟" }
        val id = Prefs.nextId(this)
        val a = Alarm(id, cal.timeInMillis, label, "由阶段1入口创建")
        val ok = AlarmScheduler.register(this, a)
        Toast.makeText(this, if (ok) "已注册：$label" else "注册失败，看诊断页日志",
            Toast.LENGTH_SHORT).show()
        render()
    }

    private fun render() {
        list.removeAllViews()
        val all = Prefs.all(this).sortedBy { it.triggerAt }
        if (all.isEmpty()) {
            list.addView(tv("（无）", 13f, Color.GRAY))
            return
        }
        all.forEach { a ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 10, 0, 10)
            }
            val t = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                .format(java.util.Date(a.triggerAt))
            row.addView(tv("$t  ${a.label}  #${a.id}", 14f, Color.WHITE).also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(btn("取消") { AlarmScheduler.cancel(this, a); render() })
            list.addView(row)
        }
        val missed = Prefs.missed(this)
        if (missed.isNotEmpty()) {
            list.addView(tv("错过的闹钟 ${missed.size} 条", 12f, Color.parseColor("#D9605A"))
                .also { it.setPadding(0, 16, 0, 0) })
            missed.take(3).forEach { list.addView(tv("· $it", 11f, Color.GRAY)) }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
            }
        }
    }

    private fun tv(s: String, size: Float, color: Int) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); setPadding(0, 4, 0, 4)
    }

    private fun btn(s: String, on: () -> Unit) = Button(this).apply {
        text = s; setOnClickListener { on() }
    }

    private fun row(vararg v: android.view.View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        v.forEach { addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
    }
}
