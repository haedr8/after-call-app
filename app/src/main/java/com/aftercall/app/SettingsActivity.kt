package com.aftercall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    private fun label(t: String, size: Float, color: Int): TextView = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(0, dp(12), 0, dp(4))
    }

    private fun btn(t: String, onClick: () -> Unit): Button = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(0xFF000000.toInt())
        setOnClickListener { onClick() }
    }

    private fun sw(label: String, key: String, def: Boolean, after: ((Boolean) -> Unit)?): Switch {
        val s = Switch(this)
        s.text = label
        s.textSize = 16f
        s.setTextColor(0xFF000000.toInt())
        s.setPadding(0, dp(10), 0, dp(10))
        s.isChecked = Store.getBool(this, key, def)
        s.setOnCheckedChangeListener { _, v ->
            Store.setBool(this, key, v)
            if (after != null) after(v)
        }
        return s
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = ScrollView(this)
        sv.setBackgroundColor(0xFFFFFFFF.toInt())
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(dp(20), dp(24), dp(20), dp(24))
        Ui.fit(col, dp(20), dp(24))

        col.addView(label("⚙️ الإعدادات", 26f, 0xFF000000.toInt()))

        col.addView(label("متى يظهر إشعار «انتهت المكالمة»؟", 15f, 0xFF555555.toInt()))
        col.addView(sw("المكالمات الواردة", "in", true, null))
        col.addView(sw("المكالمات الصادرة", "out", true, null))
        col.addView(sw("المكالمات الفائتة (مع أزرار اتصال وواتساب)", "missed", true, null))

        col.addView(label("المراقبة", 15f, 0xFF555555.toInt()))
        col.addView(sw("الخدمة الدائمة (موصى بها لتكنو وغيرها)", "service", true) { v ->
            try {
                if (v) {
                    MonitorService.start(this)
                } else {
                    stopService(Intent(this, MonitorService::class.java))
                }
            } catch (e: Exception) {
            }
        })
        col.addView(sw("نافذة عائمة بعد المكالمة", "bubble", false) { v ->
            if (v && !Settings.canDrawOverlays(this)) {
                toast("اسمح بـ«العرض فوق التطبيقات» ثم ارجع")
                try {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    )
                } catch (e: Exception) {
                }
            }
        })

        col.addView(label("الرسائل", 15f, 0xFF555555.toInt()))
        col.addView(btn("✏️ تعديل رسائل الأزرار") { editTemplates() })

        col.addView(label("النسخ الاحتياطي", 15f, 0xFF555555.toInt()))
        col.addView(btn("📤 نسخ احتياطي (نسخ للحافظة)") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("حولني", Store.backup(this)))
            toast("تم النسخ ✅ الصقه بمكان آمن (مثل ملاحظات واتساب)")
        })
        col.addView(btn("📥 استعادة (من الحافظة)") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            val t = if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(this).toString()
            } else {
                ""
            }
            toast(if (Store.restore(this, t)) "تمت الاستعادة ✅ أعد فتح الإعدادات" else "ما لقيت نسخة صالحة بالحافظة")
        })

        col.addView(btn("رجوع") { finish() })

        sv.addView(
            col,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        setContentView(sv)
    }

    private fun editTemplates() {
        val cur = Store.templates(this)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.layoutDirection = View.LAYOUT_DIRECTION_RTL
        box.setPadding(dp(16), dp(8), dp(16), dp(8))
        val labels = ArrayList<EditText>()
        val texts = ArrayList<EditText>()
        for (i in 0 until 8) {
            val l = EditText(this)
            l.hint = "اسم الزر (مثال: 📍 لوكيشن)"
            l.setSingleLine()
            l.setText(if (i < cur.size) cur[i].label else "")
            val t = EditText(this)
            t.hint = "نص الرسالة"
            t.setText(if (i < cur.size) cur[i].text else "")
            box.addView(l)
            box.addView(t)
            labels.add(l)
            texts.add(t)
        }
        val scroll = ScrollView(this)
        scroll.addView(box)
        AlertDialog.Builder(this)
            .setTitle("رسائل الأزرار (اترك الفارغ لحذفه)")
            .setView(scroll)
            .setPositiveButton("حفظ") { _, _ ->
                val out = ArrayList<Tpl>()
                for (i in 0 until 8) {
                    val a = labels[i].text.toString().trim()
                    val b = texts[i].text.toString().trim()
                    if (a.isNotEmpty() && b.isNotEmpty()) out.add(Tpl(a, b))
                }
                if (out.isEmpty()) {
                    toast("لازم زر واحد على الأقل")
                } else {
                    Store.saveTemplates(this, out)
                    toast("تم الحفظ ✅")
                }
            }
            .setNeutralButton("الافتراضي") { _, _ ->
                Store.resetTemplates(this)
                toast("رجعت الرسائل الافتراضية ✅")
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
