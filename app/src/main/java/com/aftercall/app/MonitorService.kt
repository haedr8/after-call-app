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
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class MonitorService : Service() {

    private var receiver: CallReceiver? = null

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
            val r = CallReceiver()
            ContextCompat.registerReceiver(
                this, r,
                IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
            receiver = r
            Diag.log(this, "service on")
        } catch (e: Exception) {
            Diag.log(this, "service error: " + e.message)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try {
            receiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
        }
        receiver = null
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
