package com.aftercall.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.DisconnectCause
import android.telecom.InCallService

class CallMeta(
    var number: String = "",
    var name: String = "",
    var incoming: Boolean = false,
    var answered: Boolean = false
)

object CallManager {
    val calls = mutableListOf<Call>()
    val meta = HashMap<Call, CallMeta>()
    var service: InCallService? = null
    private val listeners = mutableListOf<() -> Unit>()
    private val h = Handler(Looper.getMainLooper())

    fun listen(f: () -> Unit) {
        if (!listeners.contains(f)) listeners.add(f)
    }

    fun unlisten(f: () -> Unit) {
        listeners.remove(f)
    }

    fun changed() {
        h.post { for (f in listeners.toList()) f() }
    }

    fun primary(): Call? =
        calls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: calls.firstOrNull {
                it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING ||
                    it.state == Call.STATE_CONNECTING || it.state == Call.STATE_PULLING_CALL
            }
            ?: calls.firstOrNull()

    fun others(): List<Call> {
        val p = primary()
        return calls.filter { it !== p }
    }
}

class CallService : InCallService() {

    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            if (state == Call.STATE_ACTIVE) CallManager.meta[call]?.answered = true
            refreshNotif()
            CallManager.changed()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            val m = CallManager.meta[call]
            if (m != null && m.number.isBlank()) {
                m.number = numberOf(call)
                if (m.number.isNotBlank()) m.name = Phone.name(this@CallService, m.number)
            }
            CallManager.changed()
        }
    }

    private fun numberOf(call: Call): String = try {
        val h = call.details?.handle
        if (h == null) "" else Uri_decode(h.schemeSpecificPart ?: "")
    } catch (e: Exception) {
        ""
    }

    @Suppress("FunctionName")
    private fun Uri_decode(s: String): String = android.net.Uri.decode(s)

    override fun onCallAdded(call: Call) {
        CallManager.service = this
        val m = CallMeta()
        m.incoming = call.state == Call.STATE_RINGING
        m.number = numberOf(call)
        m.name = Phone.name(this, m.number)
        if (call.state == Call.STATE_ACTIVE) m.answered = true
        CallManager.calls.add(call)
        CallManager.meta[call] = m
        call.registerCallback(cb)
        refreshNotif()
        if (!m.incoming) {
            try {
                startActivity(
                    Intent(this, InCallActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
            }
        }
        CallManager.changed()
    }

    override fun onCallRemoved(call: Call) {
        try {
            call.unregisterCallback(cb)
        } catch (e: Exception) {
        }
        CallManager.calls.remove(call)
        val m = CallManager.meta.remove(call)
        refreshNotif()
        CallManager.changed()
        if (m != null) finished(call, m)
    }

    private fun finished(call: Call, m: CallMeta) {
        val code = try {
            call.details?.disconnectCause?.code ?: DisconnectCause.UNKNOWN
        } catch (e: Exception) {
            DisconnectCause.UNKNOWN
        }
        val type = when {
            !m.incoming -> 2
            m.answered -> 1
            code == DisconnectCause.REJECTED || code == DisconnectCause.LOCAL -> 0
            else -> 3
        }
        if (type == 0) return
        try {
            Notifier.show(applicationContext, 1, "انتهت المكالمة", m.number, m.name, true, type)
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        if (CallManager.service === this) CallManager.service = null
        super.onDestroy()
    }

    private fun channel(nm: NotificationManager) {
        if (nm.getNotificationChannel("calls") == null) {
            val ch = NotificationChannel("calls", "المكالمات", NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(null, null)
            ch.enableVibration(false)
            nm.createNotificationChannel(ch)
        }
    }

    private fun act(action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this, code,
            Intent(this, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun refreshNotif() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val p = CallManager.primary()
        if (p == null) {
            nm.cancel(77)
            return
        }
        channel(nm)
        val m = CallManager.meta[p] ?: CallMeta()
        val who = when {
            m.name.isNotBlank() -> m.name
            m.number.isNotBlank() -> m.number
            else -> "رقم غير معروف"
        }
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val ringing = p.state == Call.STATE_RINGING
        val b = Notification.Builder(this, "calls")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (ringing) "مكالمة واردة" else "مكالمة")
            .setContentText(who)
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (ringing || p.state == Call.STATE_DIALING || p.state == Call.STATE_CONNECTING) {
            b.setFullScreenIntent(open, true)
        }
        if (ringing) {
            b.addAction(Notification.Action.Builder(null, "رد", act("ANSWER", 2)).build())
            b.addAction(Notification.Action.Builder(null, "رفض", act("DECLINE", 3)).build())
        } else {
            b.addAction(Notification.Action.Builder(null, "إنهاء", act("HANGUP", 4)).build())
        }
        try {
            nm.notify(77, b.build())
        } catch (e: Exception) {
        }
    }
}

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = CallManager.primary() ?: return
        try {
            when (intent.action) {
                "ANSWER" -> {
                    p.answer(0)
                    context.startActivity(
                        Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                "DECLINE" -> p.reject(false, null)
                "HANGUP" -> p.disconnect()
            }
        } catch (e: Exception) {
        }
    }
}
