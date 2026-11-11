package com.aftercall.app

import android.app.Application
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.CallLog

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CallLogJobService.ensure(this)
    }
}

class CallLogJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        val ctx = applicationContext
        schedule(ctx)
        Diag.log(ctx, "JOB fired")
        Thread {
            try {
                val info = readLastCall(ctx, 0L)
                val ok = info != null && CallGate.claim(ctx, info.date)
                Diag.log(ctx, "JOB info=${info?.date} claimed=$ok")
                if (info != null && ok) {
                    ctx.getSharedPreferences("aftercall", Context.MODE_PRIVATE)
                        .edit().putBoolean("active", false).apply()
                    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val exists = nm.activeNotifications.any { it.id == 1 }
