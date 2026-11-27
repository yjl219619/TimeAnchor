package com.yg.timeanchor.ring

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.yg.timeanchor.core.Prefs

/**
 * 前台服务：负责播放与生命周期。界面是否弹出与它无关（需求 3）。
 * 前台服务类型 mediaPlayback —— 需真机验证 HyperOS 是否放行（需求 5）。
 */
class RingService : Service() {

    private lateinit var player: RingPlayer
    private lateinit var overlay: OverlayController
    private var current: RingItem? = null

    override fun onCreate() {
        super.onCreate()
        AlarmNotifier.ensureChannels(this)
        player = RingPlayer(this)
        overlay = OverlayController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 通知动作
        val act = intent?.action
        if (act != null && act.startsWith(AlarmNotifier.ACTION_PREFIX)) {
            val id = intent.getIntExtra("id", -1)
            when (act.removePrefix(AlarmNotifier.ACTION_PREFIX)) {
                "stop" -> finishOne(id, snooze = false)
                "done" -> finishOne(id, snooze = false)
                "snooze" -> finishOne(id, snooze = true)
            }
            return START_NOT_STICKY
        }

        val id = intent?.getIntExtra("id", -1) ?: -1
        val label = intent?.getStringExtra("label") ?: "闹钟"
        val desc = intent?.getStringExtra("desc") ?: ""
        val at = intent?.getLongExtra("triggerAt", System.currentTimeMillis()) ?: System.currentTimeMillis()
        val item = RingItem(id, label, desc, at)
        current = item

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = AlarmNotifier.serviceNotification(this, item)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(ID, notif)
            }
            Prefs.log(this, "svc", "startForeground 成功 id=$id")
        } catch (e: Exception) {
            Prefs.log(this, "svc", "startForeground 失败：${e.javaClass.simpleName} ${e.message}")
            Prefs.addMissed(this, "fg-fail id=$id ${e.javaClass.simpleName}")
        }

        // 全屏兜底通知
        try { nm.notify(RING_ID, AlarmNotifier.ringNotification(this, item)) } catch (_: Exception) {}

        // 第三层：悬浮窗整屏覆盖（正在使用别的 App 时也接管）
        showOverlay(item)

        if (!player.isRunning()) {
            player.start()
            Prefs.log(this, "svc", "开始播放 id=$id")
        } else {
            Prefs.log(this, "svc", "已有闹钟在响，id=$id 进入排队（共 ${RingQueue.size()} 条）")
        }
        return START_STICKY
    }

    private fun finishOne(id: Int, snooze: Boolean) {
        val item = current
        if (snooze && item != null) {
            // 稍后提醒 = 注册一个新的真闹钟（不是普通通知）
            com.yg.timeanchor.core.AlarmScheduler.snooze(this,
                com.yg.timeanchor.core.Alarm(id, System.currentTimeMillis(), item.label, item.desc), 10)
        }
        RingQueue.remove(id)
        overlay.hide()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(RING_ID)
        player.stop()
        current = null

        val next = RingQueue.peek()
        if (next != null) {                       // 排队中的下一个接着响
            current = next
            val n = AlarmNotifier.serviceNotification(this, next)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID, n)
            player.start()
            showOverlay(next)
            Prefs.log(this, "svc", "接续下一个 id=${next.id}")
        } else {
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun showOverlay(item: RingItem) {
        val ok = overlay.show(
            item,
            onStop = { finishOne(item.id, false) },
            onSnooze = { finishOne(item.id, true) },
            onDone = { finishOne(item.id, false) }
        )
        if (!ok) Prefs.log(this, "overlay", "覆盖未生效，仅靠 fullScreenIntent/通知 id=${item.id}")
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onDestroy() {
        overlay.hide()
        player.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ID = 1001
        const val RING_ID = 1002
    }
}
