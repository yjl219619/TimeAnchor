package com.yg.timeanchor.ring

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.yg.timeanchor.core.Prefs

/**
 * 第三层接管：悬浮窗全屏覆盖（TYPE_APPLICATION_OVERLAY）。
 * 用户正在刷抖音、屏幕亮着且已解锁时，用这个盖住当前应用。
 *
 * 约束：
 * - 拦截返回键（吃掉 KEYCODE_BACK）
 * - 不能靠点空白关闭（根视图铺满且吞掉点击）
 * - 只有三个按钮能结束
 * - 不强制关闭别的 App
 */
class OverlayController(private val ctx: Context) {

    private var wm: WindowManager? = null
    private var root: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var startAt = 0L
    private var ticker: TextView? = null

    fun canDraw(): Boolean =
        Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

    fun show(item: RingItem, onStop: () -> Unit, onSnooze: () -> Unit, onDone: () -> Unit): Boolean {
        if (!canDraw()) {
            Prefs.log(ctx, "overlay", "无悬浮窗权限，跳过覆盖（依赖 fullScreenIntent）")
            return false
        }
        hide()
        wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAt = System.currentTimeMillis()

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F20F1419"))
            setPadding(70, 70, 70, 70)
            isClickable = true          // 吞掉点击，点空白无效
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener { /* 什么都不做 */ }
            // 拦截返回键
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE
            }
        }

        box.addView(TextView(ctx).apply {
            text = item.label; textSize = 30f; setTextColor(Color.WHITE)
        })
        box.addView(TextView(ctx).apply {
            text = item.desc.ifEmpty { "时间到了" }
            textSize = 16f
            setTextColor(Color.parseColor("#8B97A5"))
            setPadding(0, 14, 0, 34)
        })
        ticker = TextView(ctx).apply {
            textSize = 14f; setTextColor(Color.parseColor("#8B97A5"))
        }
        box.addView(ticker)

        box.addView(Button(ctx).apply {
            text = "停止"
            setOnClickListener { onStop() }
        })
        box.addView(Button(ctx).apply {
            text = "稍后 10 分钟"
            setOnClickListener { onSnooze() }
        })
        box.addView(Button(ctx).apply {
            text = "完成"
            setOnClickListener { onDone() }
        })

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // 可获焦点（能吃到返回键），强制铺满
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        return try {
            wm?.addView(box, lp)
            root = box
            tick()
            Prefs.log(ctx, "overlay", "已覆盖屏幕 id=${item.id}")
            true
        } catch (e: Exception) {
            Prefs.log(ctx, "overlay", "addView 失败：${e.javaClass.simpleName} ${e.message}")
            false
        }
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        root?.let {
            try { wm?.removeView(it) } catch (_: Exception) {}
        }
        root = null
    }

    private fun tick() {
        val s = (System.currentTimeMillis() - startAt) / 1000
        ticker?.text = "已响 %d:%02d".format(s / 60, s % 60)
        handler.postDelayed({ if (root != null) tick() }, 1000)
    }
}
