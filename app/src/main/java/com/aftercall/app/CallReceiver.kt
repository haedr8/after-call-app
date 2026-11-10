package com.aftercall.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val prefs = context.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
        val wasActive = prefs.getBoolean("active", false)

        if (state == TelephonyManager.EXTRA_STATE_RINGING ||
            state == TelephonyManager.EXTRA_STATE_OFFHOOK
        ) {
            prefs.edit().putBoolean("active", true).apply()
        } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            prefs.edit().putBoolean("active", false).apply()
            if (wasActive) {
                val pending = goAsync()
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        showNotification(context)
                    } finally {
                        pending.finish()
                    }
                }, 2000)
            }
        }
    }

    private fun showNotification(context: Context) {
        var number = ""
        var name = ""

        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val args = Bundle()
            args.putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(CallLog.Calls.DATE)
            )
            args.putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            args.putInt(ContentResolver.QUERY_ARG_LIMIT, 1)

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME),
                args,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    number = it.getString(0) ?: ""
                    name = it.getString(1) ?: ""
                }
            }
        }

        val shown = if (name.isNotEmpty()) name + " - " + number else number
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "after_call"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "بعد المكالمة", NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("انتهت المكالمة")
            .setContentText(shown)
            .setAutoCancel(true)
            .build()
        nm.notify(1, notification)
    }
}
