package com.aftercall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private var waLabel: TextView? = null

    private fun dp(v: Int): Int = T.dp(this, v)

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    private fun sw(label: String, key: String, def: Boolean, after: ((Boolean) -> Unit)?): Switch {
        val s = Switch(this)
        s.text = label
        s.textSize = 15f
        s.setTextColor(T.INK)
        s.setPadding(0, dp(10), 0, dp(10))
        s.isChecked = Store.getBool(this, key, def)
        s.setOnCheckedChangeListener { _, v ->
            Store.setBool(this, key, v)
            if (after != null) after(v)
        }
        return s
    }

    private fun section(col: LinearLayout, title: String): LinearLayout {
        col.addView(T.label(this, title, 14f, T.MUTED, true), T.gap(this, 18))
        val c = T.card(this)
        col.addView(c, T.gap(this, 8))
        return c
    }

    private fun waName(v: String): String = when (v) {
        "regular" -> "واتساب العادي"
        "business" -> "واتساب للأعمال (Business)"
        "ask" -> "اسأل كل مرة"
        else -> "تلقائي"
    }

    private fun chooseWa() {
        val vals = arrayOf("auto", "regular", "business", "ask")
        val names = arrayOf(
            "تلقائي (العادي إذا موجود، وإلا Business)",
            "واتساب العادي",
            "واتساب للأعمال (Business)",
            "اسأل كل مرة"
        )
        var cur = vals.indexOf(Store.waPref(this))
        if (cur < 0) cur = 0
        AlertDialog.Builder(this)
            .setTitle("أي واتساب تستخدم للرسائل؟")
            .setSingleChoiceItems(names, cur) { d, w ->
                Store.setWaPref(this, vals[w])
                waLabel?.text = "المختار: " + waName(vals[w])
                if (vals[w] == "business" && !Store.isInstalled(this, Store.WA_B)) {
                    toast("واتساب للأعمال غير مثبت على هذا الهاتف")
                }
                if (vals[w] == "regular" && !Store.isInstalled(this, Store.WA)) {
                    toast("واتساب العادي غير مثبت على هذا الهاتف")
                }
                d.dismiss()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = ScrollView(this)
        sv.setBackgroundColor(T.BG)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(dp(16), dp(16), dp(16), dp(16))
        Ui.fit(col, dp(16), dp(16))

        val title = T.label(this, "⚙️ الإعدادات", 24f, T.INK, true)
        title.gravity = Gravity.CENTER
        col.addView(title, T.gap(this, 4))

        val a = section(col, "متى يظهر إشعار «انتهت المكالمة»؟")
        a.addView(sw("المكالمات الواردة", "in", true, null))
        a.addView(sw("المكالمات الصادرة", "out", true, null))
        a.addView(sw("المكالمات الفائتة (مع أزرار اتصال وواتساب)", "missed", true, null))

        val b = section(col, "المراقبة")
        b.addView(sw("الخدمة الدائمة (موصى بها لتكنو وغيرها)", "service", true) { v ->
            try {
                if (v) {
                    MonitorService.start(this)
                } else {
                    stopService(Intent(this, MonitorService::class.java))
                }
            } catch (e: Exception) {
            }
        })
        b.addView(sw("نافذة عائمة بعد المكالمة", "bubble", false) { v ->
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

        val w = section(col, "تطبيق واتساب")
        val wl = T.label(this, "المختار: " + waName(Store.waPref(this)), 14f, T.MUTED)
        waLabel = wl
        w.addView(wl)
        w.addView(T.button(this, "📱 اختيار واتساب (العادي / للأعمال)", false) { chooseWa() }, T.gap(this, 10))

        val c = section(col, "الرسائل")
        c.addView(T.button(this, "✏️ تعديل رسائل الأزرار", true) { editTemplates() })

        val d = section(col, "النسخ الاحتياطي")
        d.addView(T.button(this, "📤 نسخ احتياطي (نسخ للحافظة)", false) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("حولني", Store.backup(this))
            if (Build.VERSION.SDK_INT >= 33) {
                val ex = PersistableBundle()
                ex.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                clip.description.extras = ex
            }
            cm.setPrimaryClip(clip)
            toast("تم النسخ ✅ الصقه بمكان آمن (مثل ملاحظات واتساب)")
        })
        d.addView(T.button(this, "📥 استعادة (من الحافظة)", false) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            val t = if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(this).toString()
            } else {
                ""
            }
            toast(
                if (Store.restore(this, t)) "تمت الاستعادة ✅ أعد فتح الإعدادات"
                else "ما لقيت نسخة صالحة بالحافظة"
            )
        }, T.gap(this, 10))

        col.addView(T.button(this, "رجوع", true) { finish() }, T.gap(this, 22))

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
                    val x = labels[i].text.toString().trim()
                    val y = texts[i].text.toString().trim()
                    if (x.isNotEmpty() && y.isNotEmpty()) out.add(Tpl(x, y))
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
