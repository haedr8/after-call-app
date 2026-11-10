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

class CallInfo(val number: String, val name: String, val date: Long)

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val prefs = context.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
        val wasActive = prefs.getBoolean("active", false)

        if (state == TelephonyManager.EXTRA_STATE_RINGING ||
            state == TelephonyManager.EXTRA_STATE_OFFHOOK
        ) {
            if (!wasActive) {
                prefs.edit()
                    .putLong("start", System.currentTimeMillis())
                    .putBoolean("active", true)
                    .apply()
            }
        } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            prefs.edit().putBoolean("active", false).apply()
            if (wasActive) {
                val start = prefs.getLong("start", 0L)
                val pending = goAsync()
                poll(context, pending, start, 0)
            }
        }
    }

    private fun poll(
        context: Context,
        pending: BroadcastReceiver.PendingResult,
        start: Long,
        attempt: Int
    ) {
        val info = readLastCall(context)
        val fresh = info != null && info.date >= start - 5000
        if (fresh || attempt >= 12) {
            try {
                showNotification(context, info?.number ?: "", info?.name ?: "")
            } finally {
                pending.finish()
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                poll(context, pending, start, attempt + 1)
            }, 250)
        }
    }

    private fun readLastCall(context: Context): CallInfo? {
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) return null

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

        var result: CallInfo? = null
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE),
            args,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                result = CallInfo(
                    it.getString(0) ?: "",
                    it.getString(1) ?: "",
                    it.getLong(2)
                )
            }
        }
        return result
    }

    private fun showNotification(context: Context, number: String, name: String) {
        val shown = if (name.isNotEmpty()) name + " - " + number else number

        val open = Intent(context, CallDialogActivity::class.java)
        open.putExtra("number", number)
        open.putExtra("name", name)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pi = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "after_call"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "بعد المكالمة", NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("انتهت المكالمة")
            .setContentText(shown)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1, notification)
    }
}
