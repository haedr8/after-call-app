package com.aftercall.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class Opt(val label: String, val action: () -> Unit)

class CallDialogActivity : Activity() {

    private var number = ""
    private var name = ""
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        number = intent.getStringExtra("number") ?: ""
        name = intent.getStringExtra("name") ?: ""

        window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setPadding(32, 40, 32, 24)
        setContentView(root)

        addHeader()

        addRow(
            Opt("📍 طلب لوكيشن") { openWhatsApp("سلام عليكم دزلي لوكيشن") },
            Opt("🚪 باركود / نداء") {
                openWhatsApp("سلام عليكم ممكن دزلي باركود أو تسويلي نداء حتى ادخل المجمع")
            }
        )
        addRow(
            Opt("💬 واتساب") { openWhatsApp("السلام عليكم") },
            Opt("✉️ SMS") {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))
                finish()
            }
        )
        addRow(
            Opt("📞 إعادة اتصال") {
                startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
                finish()
            },
            Opt("إلغاء") { finish() }
        )
    }

    private fun addHeader() {
        val title = TextView(this)
        title.text = if (name.isNotEmpty()) name else number
        title.textSize = 22f
        root.addView(title)

        if (name.isNotEmpty()) {
            val sub = TextView(this)
            sub.text = number
            sub.textSize = 16f
            root.addView(sub)
        }
    }

    private fun addRow(a: Opt, b: Opt) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutDirection = View.LAYOUT_DIRECTION_RTL
        row.addView(makeButton(a))
        row.addView(makeButton(b))
        root.addView(row)
    }

    private fun makeButton(o: Opt): Button {
        val btn = Button(this)
        btn.text = o.label
        btn.maxLines = 1
        btn.setAutoSizeTextTypeUniformWithConfiguration(
            10, 16, 1, TypedValue.COMPLEX_UNIT_SP
        )
        btn.setOnClickListener { o.action() }
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.setMargins(8, 8, 8, 8)
        btn.layoutParams = lp
        return btn
    }

    private fun openWhatsApp(text: String) {
        val uri = Uri.parse(
            "https://wa.me/" + waNumber(number) + "?text=" + Uri.encode(text)
        )
        val wa = Intent(Intent.ACTION_VIEW, uri)
        wa.setPackage("com.whatsapp")
        try {
            startActivity(wa)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        finish()
    }

    private fun waNumber(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00")) {
            d = d.substring(2)
        } else if (d.startsWith("0")) {
            d = "964" + d.substring(1)
        }
        return d
    }
}
