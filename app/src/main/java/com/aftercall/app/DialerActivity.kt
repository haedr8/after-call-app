package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class DialerActivity : Activity() {

    private var number = StringBuilder()
    private lateinit var display: TextView
    private lateinit var nameV: TextView
    private lateinit var hint: TextView
    private var tone: ToneGenerator? = null

    private fun dp(v: Int) = T.dp(this, v)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = intent?.data
        if (intent?.action == Intent.ACTION_DIAL && !Phone.isDefault(this) && Phone.forwardDial(this, d)) {
            finish()
            return
        }
        if (d != null && d.scheme == "tel") {
            number.append(Uri.decode(d.schemeSpecificPart ?: ""))
            if (number.isNotBlank() && Store.getBool(this, "intercept", true)) {
                val n = number.toString()
                startActivity(
                    Intent(this, CallDialogActivity::class.java)
                        .putExtra("number", n).putExtra("name", Phone.name(this, n))
                )
                finish()
                return
            }
        }
        try {
            tone = ToneGenerator(AudioManager.STREAM_DTMF, 60)
        } catch (e: Exception) {
        }
        build()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val d = intent?.data
        if (d != null && d.scheme == "tel") {
            val n = Uri.decode(d.schemeSpecificPart ?: "")
            if (n.isNotBlank() && Store.getBool(this, "intercept", true)) {
                startActivity(
                    Intent(this, CallDialogActivity::class.java)
                        .putExtra("number", n).putExtra("name", Phone.name(this, n))
                )
            } else {
                number = StringBuilder(n)
                update()
            }
        }
    }

    private fun build() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(T.BG)
        root.layoutDirection = View.LAYOUT_DIRECTION_LTR
        root.setPadding(dp(12), dp(12), dp(12), dp(12))
        Ui.fit(root, dp(12), dp(12))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.layoutDirection = View.LAYOUT_DIRECTION_RTL
        top.addView(T.tonal(this, "🕘 السجل") {
            startActivity(Intent(this, LogActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).also { it.setMargins(dp(4), 0, dp(4), 0) })
        top.addView(T.tonal(this, "🏠 حولني") {
            startActivity(Intent(this, MainActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(48), 1f).also { it.setMargins(dp(4), 0, dp(4), 0) })
        root.addView(top)

        nameV = T.label(this, "", 15f, T.MUTED)
        nameV.gravity = Gravity.CENTER
        display = TextView(this)
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
        display.setTextColor(T.INK)
        display.gravity = Gravity.CENTER
        display.textDirection = View.TEXT_DIRECTION_LTR
        display.maxLines = 1
        display.setAutoSizeTextTypeUniformWithConfiguration(20, 34, 1, TypedValue.COMPLEX_UNIT_SP)
        display.minHeight = dp(56)
        display.setOnLongClickListener {
            showClipMenu()
            true
        }
        root.addView(display, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(nameV)
        val hintV = T.label(this, "اضغط مطولاً على الرقم للصق أو النسخ", 12f, T.MUTED)
        hintV.gravity = Gravity.CENTER
        hintV.tag = "hint"
        root.addView(hintV)
        hint = hintV

        val keyH = (resources.displayMetrics.heightPixels * 0.075f).toInt().coerceIn(dp(50), dp(66))

        val rows = listOf("123", "456", "789", "*0#")
        for (r in rows) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (ch in r) row.addView(key(ch), LinearLayout.LayoutParams(0, keyH, 1f).also {
                it.setMargins(dp(4), dp(3), dp(4), dp(3))
            })
            root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val last = LinearLayout(this)
        last.orientation = LinearLayout.HORIZONTAL
        val contacts = T.tonal(this, "👤") { startActivity(Intent(this, ContactsActivity::class.java)) }
        val call = T.button(this, "📞", true) { doCall() }
        val del = T.tonal(this, "⌫") { backspace() }
        del.setOnLongClickListener {
            number.setLength(0)
            update()
            true
        }
        val lpx = { LinearLayout.LayoutParams(0, keyH, 1f).also { it.setMargins(dp(4), dp(3), dp(4), dp(3)) } }
        last.addView(contacts, lpx())
        last.addView(call, lpx())
        last.addView(del, lpx())
        root.addView(last, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
        update()
    }

    private fun cleanNumber(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw) {
            when (ch) {
                in '0'..'9', '+', '*', '#' -> sb.append(ch)
                in '٠'..'٩' -> sb.append(ch - '٠')
                in '۰'..'۹' -> sb.append(ch - '۰')
            }
        }
        return sb.toString()
    }

    private fun showClipMenu() {
        val items = arrayOf("📋 لصق رقم", "📄 نسخ الرقم", "🗑 مسح")
        android.app.AlertDialog.Builder(this)
            .setItems(items) { _, w ->
                val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                when (w) {
                    0 -> {
                        val t = try {
                            cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                        val n = cleanNumber(t)
                        if (n.isEmpty()) {
                            Toast.makeText(this, "ما كو رقم منسوخ", Toast.LENGTH_SHORT).show()
                        } else {
                            number = StringBuilder(n)
                            update()
                        }
                    }
                    1 -> if (number.isNotEmpty()) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("number", number.toString()))
                        Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        number.setLength(0)
                        update()
                    }
                }
            }
            .show()
    }

    private fun key(ch: Char): View {
        val b = T.tonal(this, ch.toString()) {
            number.append(ch)
            beep(ch)
            update()
        }
        b.setAutoSizeTextTypeUniformWithConfiguration(14, 28, 1, TypedValue.COMPLEX_UNIT_SP)
        if (ch == '0') {
            b.setOnLongClickListener {
                number.append('+')
                update()
                true
            }
        }
        return b
    }

    private fun beep(ch: Char) {
        val t = when (ch) {
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> ch - '0'
        }
        try {
            tone?.startTone(t, 80)
        } catch (e: Exception) {
        }
    }

    private fun backspace() {
        if (number.isNotEmpty()) number.setLength(number.length - 1)
        update()
    }

    private fun update() {
        display.text = number.toString()
        hint.visibility = if (number.isEmpty()) View.VISIBLE else View.GONE
        nameV.text = if (number.length >= 3) Phone.name(this, number.toString()) else ""
    }

    private fun doCall() {
        if (number.isEmpty()) {
            if (Phone.granted(this, Manifest.permission.READ_CALL_LOG)) {
                try {
                    val l = CallLog.Calls.getLastOutgoingCall(this)
                    if (!l.isNullOrBlank()) {
                        number = StringBuilder(l)
                        update()
                    }
                } catch (e: Exception) {
                }
            }
            return
        }
        Phone.place(this, number.toString())
    }

    private fun pick() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), 31
            )
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val u = data?.data
        if (requestCode == 31 && resultCode == RESULT_OK && u != null) {
            try {
                contentResolver.query(
                    u, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null
                )?.use { if (it.moveToFirst()) number = StringBuilder(it.getString(0) ?: "") }
            } catch (e: Exception) {
            }
            update()
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        if (rc == 41 && g.isNotEmpty() && g[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) doCall()
    }

    override fun onDestroy() {
        try {
            tone?.release()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
