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
import android.telephony.TelephonyManager

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
        Thread {
            try {
                if (isIdle(ctx)) {
                    val info = readLastCall(ctx, 0L)
                    if (info != null && CallGate.claim(ctx, info.date)) {
                        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val exists = nm.activeNotifications.any { it.id == 1 }
                        Notifier.show(ctx, 1, "انتهت المكالمة", info.number, info.name, !exists)
                    }
                }
            } catch (e: Exception) {
            }
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 4711

        @Suppress("DEPRECATION")
        private fun isIdle(ctx: Context): Boolean {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return true
            return try {
                tm.callState == TelephonyManager.CALL_STATE_IDLE
            } catch (e: Exception) {
                true
            }
        }

        fun ensure(ctx: Context) {
            try {
                val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                if (js.getPendingJob(JOB_ID) == null) schedule(ctx)
            } catch (e: Exception) {
            }
        }

        fun schedule(ctx: Context) {
            try {
                val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val job = JobInfo.Builder(JOB_ID, ComponentName(ctx, CallLogJobService::class.java))
                    .addTriggerContentUri(
                        JobInfo.TriggerContentUri(
                            CallLog.Calls.CONTENT_URI,
                            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                        )
                    )
                    .setTriggerContentUpdateDelay(300)
                    .setTriggerContentMaxDelay(1500)
                    .setPersisted(true)
                    .build()
                js.schedule(job)
            } catch (e: Exception) {
            }
        }
    }
}
