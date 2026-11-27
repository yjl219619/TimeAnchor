package com.yg.timeanchor.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.yg.timeanchor.core.Prefs

/**
 * 播放器：USAGE_ALARM（独立闹钟音量档）。
 * 需求 1：失去音频焦点时**继续播放**，不暂停、不停止；
 * 也不做“每 2 秒重申请焦点”的轮询——只在开始时请求一次 EXCLUSIVE。
 */
class RingPlayer(private val ctx: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopAt = 0L
    private var running = false

    fun start() {
        stop(); running = true
        val uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(ctx, uri)
                isLooping = true
                prepare()
                start()
            }
            Prefs.log(ctx, "ring", "MediaPlayer 启动 uri=$uri")
        } catch (e: Exception) {
            Prefs.log(ctx, "ring", "播放失败：${e.javaClass.simpleName} ${e.message}")
        }

        // 只请求一次；之后即使被抢走也继续播（需求 1）
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= 26) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { change ->
                    Prefs.log(ctx, "focus", "focus 变化=$change（继续播放）")
                }
                .build()
            focus = req
            val r = am.requestAudioFocus(req)
            Prefs.log(ctx, "focus", "请求 EXCLUSIVE 结果=$r")
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }

        startVibrate()

        val maxMin = Prefs.maxRingMinutes(ctx)
        if (maxMin > 0) {
            stopAt = System.currentTimeMillis() + maxMin * 60_000L
            handler.postDelayed({ timeout(false) }, maxMin * 60_000L)
        }
    }

    /** 超时或被用户停止时的收尾；timeout=true 时写“错过的闹钟”。 */
    fun timeout(recordMissed: Boolean) {
        if (recordMissed) {
            Prefs.addMissed(ctx, "timeout at=${System.currentTimeMillis()}")
            Prefs.log(ctx, "ring", "达到最长响铃时间，已停止")
        }
        stop()
    }

    fun stop() {
        if (!running && player == null) return
        running = false
        handler.removeCallbacksAndMessages(null)
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        focus?.let {
            (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
        }
        focus = null
    }

    fun isRunning() = running
    fun stopAtMs() = stopAt

    private fun startVibrate() {
        val pattern = longArrayOf(0, 700, 500)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Prefs.log(ctx, "ring", "振动失败：${e.javaClass.simpleName}")
        }
    }
}
