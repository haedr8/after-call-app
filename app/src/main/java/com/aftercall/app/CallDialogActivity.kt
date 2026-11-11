package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CallDialogActivity : Activity() {

    private class Opt(val label: String, val action: () -> Unit)

    private var number = ""
    private var name = ""

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        number = intent.getStringExtra("number") ?: ""
        name = intent.getStringExtra("name") ?: ""
        if (number.isBlank()) {
            val last = readLastCall(this, 0L)
            if (last != null) {
                number = last.number
                name = last.name
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }

        root.addView(TextView(this).apply {
            text = when {
                name.isNotBlank() -> name
                number.isNotBlank() -> number
                else -> "المكالمة"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(0xFF000000.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        if (number.isNotBlank()) {
            if (name.isNotBlank()) {
                root.addView(TextView(this).apply {
                    text = number
                    textDirection = View.TEXT_DIRECTION_LTR
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(0xFF444444.toInt())
                    gravity = Gravity.CENTER
                })
            }
            addRow(
                root,
                Opt("📍 طلب لوكيشن") { openWhatsApp("سلام عليكم دزلي لوكيشن") },
                Opt("🚪 باركود / نداء") {
                    openWhatsApp("سلام عليكم ممكن دزلي باركود أو تسويلي نداء حتى ادخل المجمع")
                }
            )
            addRow(
                root,
                Opt("💬 واتساب") { openWhatsApp("السلام عليكم") },
                Opt("✉️ SMS") { openSms() }
            )
            addRow(
                root,
                Opt("📞 إعادة اتصال") { callBack() },
                Opt("إلغاء") { finish() }
            )
        } else {
            root.addView(TextView(this).apply {
                text = "لا يوجد رقم لهذه المكالمة. تأكد من منح الصلاحيات من داخل التطبيق."
                setTextColor(0xFF444444.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
            })
            addRow(root, Opt("إلغاء") { finish() }, null)
        }

        setContentView(root)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(true)
    }

    private fun makeBtn(o: Opt): Button = Button(this).apply {
        text = o.label
        isAllCaps = false
        maxLines = 1
        setTextColor(0xFF000000.toInt())
        setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP)
        setOnClickListener { o.action() }
    }

    private fun addRow(root: LinearLayout, a: Opt, b: Opt?) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        fun lp(): LinearLayout.LayoutParams {
            val p = LinearLayout.LayoutParams(0, dp(52), 1f)
            p.setMargins(dp(4), dp(4), dp(4), dp(4))
            return p
        }
        row.addView(makeBtn(a), lp())
        if (b != null) row.addView(makeBtn(b), lp())
        root.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun waNumber(): String {
        val raw = number.trim()
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> "964" + digits.drop(1)
            else -> digits
        }
    }

    private fun openWhatsApp(text: String) {
        val url = "https://wa.me/" + waNumber() + "?text=" + Uri.encode(text)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(this, "واتساب غير موجود", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun openSms() {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الرسائل", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun callBack() {
        val uri = Uri.parse("tel:" + Uri.encode(number))
        val granted = checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        try {
            startActivity(Intent(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر الاتصال", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
