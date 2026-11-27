package com.yg.timeanchor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** 全屏闹钟界面：锁屏显示、亮屏；按钮与通知里的三个动作同义。 */
class RingActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var ticker: TextView
    private var startAt = 0L

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        startAt = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val label = intent.getStringExtra("label") ?: "闹钟"
        val desc = intent.getStringExtra("desc") ?: ""
        val id = intent.getIntExtra("id", -1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F1419"))
            setPadding(60, 60, 60, 60)
        }
        root.addView(TextView(this).apply {
            text = label; textSize = 32f; setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = desc; textSize = 16f; setTextColor(Color.parseColor("#8B97A5"))
            setPadding(0, 16, 0, 40)
        })
        ticker = TextView(this).apply { textSize = 14f; setTextColor(Color.parseColor("#8B97A5")) }
        root.addView(ticker)

        root.addView(Button(this).apply {
            text = "停止"; setOnClickListener { act("stop", id) }
        })
        root.addView(Button(this).apply {
            text = "稍后 10 分钟"; setOnClickListener { act("snooze", id) }
        })
        root.addView(Button(this).apply {
            text = "完成"; setOnClickListener { act("done", id) }
        })

        setContentView(root)
        tick()
    }

    private fun tick() {
        val s = (System.currentTimeMillis() - startAt) / 1000
        ticker.text = "已响 %d:%02d".format(s / 60, s % 60)
        h.postDelayed({ if (!isFinishing) tick() }, 1000)
    }

    private fun act(what: String, id: Int) {
        val i = Intent(this, com.yg.timeanchor.ring.RingService::class.java).apply {
            action = com.yg.timeanchor.ring.AlarmNotifier.ACTION_PREFIX + what
            putExtra("id", id)
        }
        startService(i)
        finish()
    }

    override fun onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy() }
}
