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
import android.content.pm.PackageManager
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

class CallInfo(val number: String, val name: String, val date: Long)

fun readLastCall(ctx: Context, start: Long): CallInfo? {
    if (ctx.checkSelfPermission(Manifest.permission.READ_CALL_LOG) !=
        PackageManager.PERMISSION_GRANTED
    ) return null
    return try {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "date DESC")
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        }
        ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE),
            args,
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                val date = c.getLong(2)
                if (date >= start - 5000) {
                    CallInfo(c.getString(0) ?: "", c.getString(1) ?: "", date)
                } else null
            } else null
        }
    } catch (e: Exception) {
        null
    }
}

object CallGate {
    @Synchronized
    fun claim(ctx: Context, date: Long): Boolean {
        val p = ctx.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
        var last = p.getLong("last_date", 0L)
        if (last == 0L) last = System.currentTimeMillis() - 10 * 60 * 1000
        if (date <= last) return false
        p.edit().putLong("last_date", date).apply()
        return true
    }
}

object Notifier {
    fun show(
        ctx: Context, id: Int, title: String, number: String, name: String,
        alert: Boolean = true
    ) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel("after_call") == null) {
            nm.createNotificationChannel(
                NotificationChannel("after_call", "حولني", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        if (alert) nm.cancel(id)
        val i = Intent(ctx, CallDialogActivity::class.java).apply {
            putExtra("number", number)
            putExtra("name", name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            ctx, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = when {
            name.isNotBlank() && number.isNotBlank() -> "$name - $number"
            name.isNotBlank() -> name
            number.isNotBlank() -> number
            else -> "اضغط لعرض الخيارات"
        }
        val n = Notification.Builder(ctx, "after_call")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            nm.notify(id, n)
        } catch (e: SecurityException) {
        }
    }
}

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        Diag.log(context, "state=$state")
        val p = context.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
        @Suppress("DEPRECATION")
        val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (!p.getBoolean("active", false)) {
                    p.edit().putBoolean("active", true)
                        .putLong("start", System.currentTimeMillis())
                        .putString("last_number", "").apply()
                }
                if (num.isNotBlank()) p.edit().putString("last_number", num).apply()
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasActive = p.getBoolean("active", false)
                val known = p.getString("last_number", "") ?: ""
                p.edit().putBoolean("active", false).apply()
                val app = context.applicationContext
                Diag.log(app, "IDLE wasActive=$wasActive")
                if (wasActive) {
                    // إشعار فوري بدون انتظار
                    Notifier.show(app, 1, "انتهت المكالمة", known, "")
                }
                val pending = goAsync()
                poll(app, wasActive, pending, 0)
            }
        }
    }

    private fun poll(
        ctx: Context, shownBefore: Boolean,
        pending: BroadcastReceiver.PendingResult, attempt: Int
    ) {
        val info = readLastCall(ctx, 0L)
        if (attempt == 0 || attempt == 12) Diag.log(ctx, "poll#$attempt info=${info?.date}")
        if (info != null && CallGate.claim(ctx, info.date)) {
            Diag.log(ctx, "poll claimed -> notify")
            Notifier.show(ctx, 1, "انتهت المكالمة", info.number, info.name, !shownBefore)
            pending.finish()
        } else if (attempt >= 12) {
            pending.finish()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                poll(ctx, shownBefore, pending, attempt + 1)
            }, 250)
        }
    }
}
