package com.aftercall.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.TelephonyManager

object Diag {
    @Synchronized
    fun log(ctx: Context, msg: String) {
        try {
            val p = ctx.getSharedPreferences("diag", Context.MODE_PRIVATE)
            val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val lines = ((p.getString("log", "") ?: "") + "$t $msg\n").lines().filter { it.isNotBlank() }
            p.edit().putString("log", lines.takeLast(30).joinToString("\n")).apply()
        } catch (e: Exception) {
        }
    }
}

object Notifier {
    private var rt: android.media.Ringtone? = null

    private fun playSound(ctx: Context) {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
            rt?.stop()
            val r = RingtoneManager.getRingtone(
                ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            r.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            r.play()
            rt = r
        } catch (e: Exception) {
            Diag.log(ctx, "sound error: " + e.message)
        }
    }

    fun show(
        ctx: Context, id: Int, title: String, number: String, name: String,
        alert: Boolean = true, type: Int = 0
    ) {
        if (type != 0 && !Store.allows(ctx, type)) return
        val missed = type == 3 || type == 5
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel("after_call3") == null) {
            val ch = NotificationChannel(
                "after_call3", "انتهاء المكالمة", NotificationManager.IMPORTANCE_HIGH
            )
            ch.setSound(null, null)
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
            try {
                nm.deleteNotificationChannel("after_call")
                nm.deleteNotificationChannel("after_call2")
            } catch (e: Exception) {
            }
        }
        if (alert) nm.cancel(id)
        val i = Intent(ctx, CallDialogActivity::class.java).apply {
            putExtra("number", number)
            putExtra("name", name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(ctx, id, i, flags)
        val text = when {
            name.isNotBlank() && number.isNotBlank() -> "$name - $number"
            name.isNotBlank() -> name
            number.isNotBlank() -> number
            else -> "اضغط لعرض الخيارات"
        }
        val b = Notification.Builder(ctx, "after_call3")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (missed) "مكالمة فائتة" else title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        if (missed && number.isNotBlank()) {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val wa = Store.waIntent(ctx, number, "السلام عليكم", Store.waPackage(ctx))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val icon = Icon.createWithResource(ctx, R.drawable.ic_notif)
            b.addAction(
                Notification.Action.Builder(
                    icon, "اتصال", PendingIntent.getActivity(ctx, id + 100, dial, flags)
                ).build()
            )
            b.addAction(
                Notification.Action.Builder(
                    icon, "واتساب", PendingIntent.getActivity(ctx, id + 200, wa, flags)
                ).build()
            )
        }
        try {
            nm.notify(id, b.build())
        } catch (e: SecurityException) {
        }
        if (alert) playSound(ctx)
    }
}

class CallReceiver : BroadcastReceiver() {

    private fun begin(p: SharedPreferences) {
        if (!p.getBoolean("active", false)) {
            p.edit().putBoolean("active", true)
                .putLong("start", System.currentTimeMillis())
                .putString("last_number", "")
                .putString("kind", "")
                .apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (Phone.isDefault(context)) return
        Diag.log(context, "state=$state")
        val p = context.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
        @Suppress("DEPRECATION")
        val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                begin(p)
                if ((p.getString("kind", "") ?: "").isEmpty()) {
                    p.edit().putString("kind", "ring").apply()
                }
                if (num.isNotBlank()) p.edit().putString("last_number", num).apply()
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                begin(p)
                val k = p.getString("kind", "") ?: ""
                p.edit().putString("kind", if (k == "ring" || k == "in") "in" else "out").apply()
                if (num.isNotBlank()) p.edit().putString("last_number", num).apply()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val nowIdle = System.currentTimeMillis()
                if (nowIdle - p.getLong("last_idle", 0L) < 1500) return
                p.edit().putLong("last_idle", nowIdle).apply()
                val type = when (p.getString("kind", "") ?: "") {
                    "ring" -> 3
                    "in" -> 1
                    "out" -> 2
                    else -> 0
                }
                p.edit().putBoolean("active", false).putString("kind", "").apply()
                if (type == 0) return
                Notifier.show(context.applicationContext, 1, "انتهت المكالمة", "", "", true, type)
            }
        }
    }
}
