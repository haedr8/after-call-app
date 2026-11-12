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
        if (date <= last) retur
