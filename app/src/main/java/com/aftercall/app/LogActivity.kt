package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class LogActivity : Activity() {

    private fun dp(v: Int) = T.dp(this, v)

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(dp(12), dp(12), dp(12), dp(12))
        Ui.fit(col, dp(12), dp(12))
        val title = T.label(this, "🕘 سجل المكالمات", 22f, T.INK, true)
        title.gravity = Gravity.CENTER
        col.addView(title, T.gap(this, 4))

        if (!Phone.granted(this, Manifest.permission.READ_CALL_LOG)) {
            val m = T.label(
                this,
                "لعرض السجل اجعل «حولني» تطبيق الهاتف ووافق على صلاحية السجل من الشاشة الرئيسية.",
                15f, T.MUTED
            )
            m.gravity = Gravity.CENTER
            col.addView(m, T.gap(this, 20))
            col.addView(T.button(this, "فتح الشاشة الرئيسية", true) {
                startActivity(Intent(this, MainActivity::class.java))
            }, T.gap(this, 12))
        } else {
            try {
                val args = android.os.Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "date DESC")
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 150)
                }
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                        CallLog.Calls.DATE, CallLog.Calls.DURATION
                    ),
                    args, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        row(col, c.getString(0) ?: "", c.getString(1) ?: "", c.getInt(2), c.getLong(3), c.getLong(4))
                    }
                }
            } catch (e: Exception) {
            }
            if (col.childCount == 1) {
                col.addView(T.label(this, "لا توجد مكالمات", 15f, T.MUTED), T.gap(this, 20))
            }
        }
        val sv = ScrollView(this)
        sv.setBackgroundColor(T.BG)
        sv.addView(col)
        setContentView(sv)
    }

    private fun row(col: LinearLayout, number: String, name: String, type: Int, date: Long, dur: Long) {
        val (icon, color) = when (type) {
            CallLog.Calls.INCOMING_TYPE -> "↙️" to T.OK
            CallLog.Calls.OUTGOING_TYPE -> "↗️" to T.TEAL
            CallLog.Calls.MISSED_TYPE -> "❌" to 0xFFD32F2F.toInt()
            CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> "🚫" to T.MUTED
            else -> "📞" to T.MUTED
        }
        val box = T.card(this)
        val l1 = T.label(
            this, icon + " " + (if (name.isNotBlank()) name else if (number.isBlank()) "رقم مخفي" else number),
            16f, color, true
        )
        box.addView(l1)
        val sub = (if (name.isNotBlank()) number + "  •  " else "") +
            DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) +
            (if (dur > 0) "  •  " + dur / 60 + ":" + String.format(java.util.Locale.US, "%02d", dur % 60) else "")
        val l2 = T.label(this, sub, 13f, T.MUTED)
        l2.textDirection = View.TEXT_DIRECTION_LTR
        box.addView(l2)
        box.setOnClickListener {
            if (number.isNotBlank()) {
                startActivity(
                    Intent(this, CallDialogActivity::class.java)
                        .putExtra("number", number).putExtra("name", name)
                )
            }
        }
        col.addView(box, T.gap(this, 8))
    }

    private fun options(number: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle(if (name.isNotBlank()) name else number)
            .setItems(arrayOf("📞 اتصال", "💬 رسائل واتساب الجاهزة", "✉️ SMS")) { _, w ->
                when (w) {
                    0 -> Phone.place(this, number)
                    1 -> startActivity(
                        Intent(this, CallDialogActivity::class.java)
                            .putExtra("number", number).putExtra("name", name)
                    )
                    2 -> try {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))))
                    } catch (e: Exception) {
                    }
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }
}
