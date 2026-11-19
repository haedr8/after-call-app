package com.aftercall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

@Suppress("DEPRECATION")
class InCallActivity : Activity() {

    private val h = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var ended = false
    private val bg = 0xFF0F2A30.toInt()

    private val listener: () -> Unit = { render() }
    private val tick = object : Runnable {
        override fun run() {
            render()
            h.postDelayed(this, 1000)
        }
    }

    private fun dp(v: Int) = T.dp(this, v)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        render()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        render()
    }

    override fun onResume() {
        super.onResume()
        CallManager.listen(listener)
        h.post(tick)
    }

    override fun onPause() {
        super.onPause()
        CallManager.unlisten(listener)
        h.removeCallbacks(tick)
        releaseWake()
    }

    private fun releaseWake() {
        try {
            if (wake?.isHeld == true) wake?.release()
        } catch (e: Exception) {
        }
        wake = null
    }

    private fun proximity(on: Boolean) {
        try {
            if (!on) {
                releaseWake()
                return
            }
            if (wake?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) return
            wake = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "haulni:prox")
            wake?.acquire(60 * 60 * 1000L)
        } catch (e: Exception) {
        }
    }

    private fun stateText(c: Call): String = when (c.state) {
        Call.STATE_RINGING -> "مكالمة واردة"
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW,
        Call.STATE_SELECT_PHONE_ACCOUNT, Call.STATE_PULLING_CALL -> "جاري الاتصال..."
        Call.STATE_HOLDING -> "معلّقة"
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "انتهت المكالمة"
        Call.STATE_ACTIVE -> {
            val t = try {
                c.details.connectTimeMillis
            } catch (e: Exception) {
                0L
            }
            if (t > 0) {
                val s = ((System.currentTimeMillis() - t) / 1000).coerceAtLeast(0)
                String.format("%02d:%02d", s / 60, s % 60)
            } else "متصل"
        }
        else -> ""
    }

    private fun big(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        t.setTextColor(color)
        t.gravity = Gravity.CENTER
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun circle(label: String, color: Int, on: Boolean = false, f: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.setTextColor(0xFFFFFFFF.toInt())
        t.setAutoSizeTextTypeUniformWithConfiguration(9, 15, 1, TypedValue.COMPLEX_UNIT_SP)
        t.maxLines = 2
        t.gravity = Gravity.CENTER
        t.background = T.shape(this, if (on) 0xFF1CC47A.toInt() else color, 40)
        t.setOnClickListener { f() }
        return t
    }

    private fun lp(): LinearLayout.LayoutParams {
        val h = (resources.displayMetrics.heightPixels * 0.095f).toInt().coerceIn(dp(56), dp(84))
        val p = LinearLayout.LayoutParams(0, h, 1f)
        p.setMargins(dp(6), dp(6), dp(6), dp(6))
        return p
    }

    private fun render() {
        val p = CallManager.primary()
        if (p == null) {
            if (!ended) {
                ended = true
                setContentView(View(this).also { it.setBackgroundColor(bg) })
                h.postDelayed({ finish() }, 400)
            }
            return
        }
        val m = CallManager.meta[p] ?: CallMeta()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(bg)
        root.setPadding(dp(16), dp(48), dp(16), dp(28))
        Ui.fit(root, dp(16), dp(24))
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL

        root.addView(big(stateText(p), 16f, 0xFFB7D3D6.toInt()))
        val title = when {
            m.name.isNotBlank() -> m.name
            m.number.isNotBlank() -> m.number
            else -> "رقم غير معروف"
        }
        val nameV = big(title, 30f, 0xFFFFFFFF.toInt(), true)
        nameV.setPadding(0, dp(24), 0, dp(4))
        root.addView(nameV)
        if (m.name.isNotBlank() && m.number.isNotBlank()) {
            val n = big(m.number, 16f, 0xFFB7D3D6.toInt())
            n.textDirection = View.TEXT_DIRECTION_LTR
            root.addView(n)
        }

        for (o in CallManager.others()) {
            val om = CallManager.meta[o] ?: CallMeta()
            val row = big(
                "مكالمة أخرى: " + (om.name.ifBlank { om.number.ifBlank { "غير معروف" } }) + "  ⇄ تبديل",
                14f, 0xFFFFD27A.toInt()
            )
            row.setPadding(0, dp(18), 0, 0)
            row.setOnClickListener { swap(p, o) }
            root.addView(row)
        }

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(0, 0, 1f))

        val ringing = p.state == Call.STATE_RINGING
        val active = !ringing && p.state != Call.STATE_DISCONNECTED && p.state != Call.STATE_DISCONNECTING
        val ctl = 0xFF1F4A52.toInt()

        if (active) {
            val route = currentRoute()
            val muted = try {
                CallManager.service?.callAudioState?.isMuted ?: false
            } catch (e: Exception) {
                false
            }
            val speaker = route == CallAudioState.ROUTE_SPEAKER
            proximity(p.state == Call.STATE_ACTIVE && route == CallAudioState.ROUTE_EARPIECE)
            val r1 = LinearLayout(this)
            r1.orientation = LinearLayout.HORIZONTAL
            r1.addView(circle(if (muted) "🔇\nمكتوم" else "🎤\nكتم", ctl, muted) {
                CallManager.service?.setMuted(!muted)
                h.postDelayed({ render() }, 200)
            }, lp())
            val mask = try {
                CallManager.service?.callAudioState?.supportedRouteMask ?: 0
            } catch (e: Exception) {
                0
            }
            val bt = route == CallAudioState.ROUTE_BLUETOOTH
            val wired = route == CallAudioState.ROUTE_WIRED_HEADSET
            val audioLabel = when {
                bt -> "🎧\nبلوتوث"
                wired -> "🎧\nسماعة سلكية"
                speaker -> "🔊\nسبيكر"
                else -> "📱\nالأذن"
            }
            r1.addView(circle(audioLabel, ctl, speaker || bt || wired) {
                if (android.os.Build.VERSION.SDK_INT >= 34 && CallManager.endpoints.isNotEmpty()) endpointPicker()
                else audioPicker(mask, route)
            }, lp())
            r1.addView(circle("⌨️\nأرقام", ctl) { dtmf(p) }, lp())
            val canHold = p.state == Call.STATE_ACTIVE || p.state == Call.STATE_HOLDING
            r1.addView(
                circle(if (!canHold) "⏸\nانتظار" else if (p.state == Call.STATE_HOLDING) "▶️\nاستئناف" else "⏸\nانتظار", ctl) {
                    if (p.state == Call.STATE_HOLDING) p.unhold() else if (canHold) p.hold()
                }, lp()
            )
            root.addView(r1)
        } else {
            proximity(false)
        }

        val r2 = LinearLayout(this)
        r2.orientation = LinearLayout.HORIZONTAL
        r2.gravity = Gravity.CENTER
        if (ringing) {
            r2.addView(circle("رد", 0xFF1CC47A.toInt()) { p.answer(0) }, lp())
            r2.addView(circle("رفض", 0xFFD32F2F.toInt()) { p.reject(false, null) }, lp())
        } else {
            r2.addView(circle("إنهاء", 0xFFD32F2F.toInt()) { p.disconnect() }, lp())
        }
        val q = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        q.topMargin = dp(12)
        root.addView(r2, q)
        setContentView(root)
    }

    private fun currentRoute(): Int {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val e = CallManager.currentEndpoint
            if (e is android.telecom.CallEndpoint) {
                return when (e.endpointType) {
                    android.telecom.CallEndpoint.TYPE_BLUETOOTH -> CallAudioState.ROUTE_BLUETOOTH
                    android.telecom.CallEndpoint.TYPE_WIRED_HEADSET -> CallAudioState.ROUTE_WIRED_HEADSET
                    android.telecom.CallEndpoint.TYPE_SPEAKER -> CallAudioState.ROUTE_SPEAKER
                    else -> CallAudioState.ROUTE_EARPIECE
                }
            }
        }
        return try {
            CallManager.service?.callAudioState?.route ?: 0
        } catch (e: Exception) {
            0
        }
    }

    @android.annotation.TargetApi(34)
    private fun endpointPicker() {
        val list = CallManager.endpoints.filterIsInstance<android.telecom.CallEndpoint>()
            .filter { it.endpointType != android.telecom.CallEndpoint.TYPE_STREAMING }
        val names = list.map {
            when (it.endpointType) {
                android.telecom.CallEndpoint.TYPE_BLUETOOTH -> "🎧 " + it.endpointName
                android.telecom.CallEndpoint.TYPE_WIRED_HEADSET -> "🎧 سماعة سلكية"
                android.telecom.CallEndpoint.TYPE_SPEAKER -> "🔊 السبيكر"
                else -> "📱 سماعة الأذن"
            }
        }.toTypedArray()
        val cur = CallManager.currentEndpoint
        val i = list.indexOfFirst { it == cur }
        AlertDialog.Builder(this)
            .setTitle("مخرج الصوت")
            .setSingleChoiceItems(names, i) { d, w ->
                try {
                    CallManager.service?.requestCallEndpointChange(
                        list[w], mainExecutor,
                        object : android.os.OutcomeReceiver<Void, android.telecom.CallEndpointException> {
                            override fun onResult(result: Void?) {}
                            override fun onError(error: android.telecom.CallEndpointException) {}
                        }
                    )
                } catch (e: Exception) {
                }
                d.dismiss()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun setRoute(r: Int) {
        try {
            CallManager.service?.setAudioRoute(r)
        } catch (e: Exception) {
        }
        h.postDelayed({ render() }, 250)
    }

    private fun audioPicker(mask: Int, current: Int) {
        val names = ArrayList<String>()
        val routes = ArrayList<Int>()
        if (mask and CallAudioState.ROUTE_BLUETOOTH != 0) {
            names.add("🎧 سماعة بلوتوث"); routes.add(CallAudioState.ROUTE_BLUETOOTH)
        }
        if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
            names.add("🎧 سماعة سلكية"); routes.add(CallAudioState.ROUTE_WIRED_HEADSET)
        }
        if (mask and CallAudioState.ROUTE_EARPIECE != 0) {
            names.add("📱 سماعة الأذن"); routes.add(CallAudioState.ROUTE_EARPIECE)
        }
        if (mask and CallAudioState.ROUTE_SPEAKER != 0) {
            names.add("🔊 السبيكر"); routes.add(CallAudioState.ROUTE_SPEAKER)
        }
        if (routes.size <= 2 && routes.contains(CallAudioState.ROUTE_SPEAKER) &&
            routes.contains(CallAudioState.ROUTE_EARPIECE)
        ) {
            setRoute(
                if (current == CallAudioState.ROUTE_SPEAKER) CallAudioState.ROUTE_EARPIECE
                else CallAudioState.ROUTE_SPEAKER
            )
            return
        }
        val i = routes.indexOf(current)
        AlertDialog.Builder(this)
            .setTitle("مخرج الصوت")
            .setSingleChoiceItems(names.toTypedArray(), i) { d, w ->
                setRoute(routes[w])
                d.dismiss()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun swap(p: Call, o: Call) {
        try {
            if (p.state == Call.STATE_ACTIVE) p.hold()
            if (o.state == Call.STATE_HOLDING) o.unhold()
        } catch (e: Exception) {
        }
    }

    private fun dtmf(c: Call) {
        val keys = "123456789*0#".toCharArray().map { it.toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("إرسال أرقام")
            .setItems(keys) { _, i ->
                try {
                    c.playDtmfTone(keys[i][0])
                    h.postDelayed({ try { c.stopDtmfTone() } catch (e: Exception) {} }, 150)
                } catch (e: Exception) {
                }
                dtmf(c)
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    override fun onDestroy() {
        releaseWake()
        super.onDestroy()
    }
}
