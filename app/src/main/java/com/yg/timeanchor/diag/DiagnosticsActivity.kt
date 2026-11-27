package com.yg.timeanchor.diag

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.yg.timeanchor.core.Prefs

class DiagnosticsActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 56, 36, 36)
            setBackgroundColor(Color.parseColor("#0F1419"))
        }
        root.addView(TextView(this).apply {
            text = "设备诊断"; textSize = 22f; setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        })

        Diagnostics.run(this).forEach { c ->
            val mark = when (c.ok) { true -> "✓"; false -> "✗"; null -> "?" }
            val color = when (c.ok) { true -> Color.parseColor("#3ECF8E")
                false -> Color.parseColor("#D9605A"); null -> Color.parseColor("#E0A33E") }
            root.addView(TextView(this).apply {
                text = "$mark  ${c.name}\n     ${c.detail}"
                textSize = 14f; setTextColor(color); setPadding(0, 12, 0, 0)
            })
            c.action?.let { i ->
                root.addView(Button(this).apply {
                    text = "去处理"; textSize = 13f
                    setOnClickListener { runCatching { startActivity(i) } }
                })
            }
        }

        root.addView(TextView(this).apply {
            text = "最近日志"; textSize = 16f; setTextColor(Color.WHITE)
            setPadding(0, 26, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = Prefs.logs(this@DiagnosticsActivity).ifBlank { "（暂无）" }
            textSize = 11f; setTextColor(Color.parseColor("#8B97A5"))
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
