package com.aftercall.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CallDialogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = intent.getStringExtra("number") ?: ""
        val name = intent.getStringExtra("name") ?: ""

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setPadding(48, 48, 48, 32)

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

        addButton(root, "مراسلة واتساب") {
            val text = Uri.encode("السلام عليكم")
            val uri = Uri.parse("https://wa.me/" + waNumber(number) + "?text=" + text)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            finish()
        }
        addButton(root, "إرسال SMS") {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))
            finish()
        }
        addButton(root, "إعادة الاتصال") {
            startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
            finish()
        }
        addButton(root, "إلغاء") {
            finish()
        }

        setContentView(root)
    }

    private fun addButton(root: LinearLayout, label: String, action: () -> Unit) {
        val b = Button(this)
        b.text = label
        b.setOnClickListener { action() }
        root.addView(b)
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
