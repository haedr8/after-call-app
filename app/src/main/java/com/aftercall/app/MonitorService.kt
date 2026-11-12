package com.aftercall.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class MonitorService : Service() {

    private var receiver: CallReceiver? = null
    private var observer: ContentObserver? = null
    private var telCb: Any? = null
    private val handler = Handler(Looper.getMainLooper())

    private val check = Runnable {
        val ctx = applicationContext
        val info = readLastCall(ctx, 0L)
        if (info != null && CallGate.claim(ctx, info.date)) {
            val nm = getSystemService(NotificationManager::class.java)
            val shown = try {
                nm.activeNotifications.any { it.id == 1 }
            } catch (e: Exception) {
                false
            }
            Diag.log(ctx, "obs claimed -> notify")
            Notifier.show(ctx, 1, "انتهت المكالمة", info.number, info.name, !shown)
            getSharedPreferences("aftercall", Context.MODE_PRIVATE)
                .edit().putBoolean("active", false).apply()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("monitor", "حولني يعمل", NotificationManager.IMPORTANCE_MIN)
            )
            val n = Notification.Builder(this, "monitor")
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("حولني يعمل")
                .setContentText("يراقب نهاية المكالمات")
                .setOngoing(true)
                .build()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(77, n)
            }
        } catch (e: Exception) {
            Diag.log(this, "service error: " + e.message)
            stopSelf()
            return
        }

        try {
            val r = CallReceiver()
            ContextCompat.registerReceiver(
                this, r,
                IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
            receiver = r
        } catch (e: Exception) {
            Diag.log(this, "receiver error: " + e.message)
        }

        try {
            val o = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    Diag.log(applicationContext, "obs change")
                    handler.removeCallbacks(check)
                    handler.postDelayed(check, 800)
                }
            }
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, o)
            observer = o
        } catch (e: Exception) {
            Diag.log(this, "observer error: " + e.message)
        }

        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val tm = getSystemService(TelephonyManager::class.java)
                val c = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        Diag.log(applicationContext, "cb=$state")
                    }
                }
                tm.registerTelephonyCallback(mainExecutor, c)
                telCb = c
            }
        } catch (e: Exception) {
            Diag.log(this, "callback error: " + e.message)
        }

        Diag.log(this, "service on")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try {
            receiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
        }
        receiver = null
        try {
            observer?.let { contentResolver.unregisterContentObserver(it) }
        } catch (e: Exception) {
        }
        observer = null
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                (telCb as? TelephonyCallback)?.let {
                    getSystemService(TelephonyManager::class.java).unregisterTelephonyCallback(it)
                }
            }
        } catch (e: Exception) {
        }
        telCb = null
        handler.removeCallbacks(check)
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, MonitorService::class.java))
            } catch (e: Exception) {
                Diag.log(ctx, "service start failed: " + e.message)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MonitorService.start(context)
        }
    }
}
