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

        val pp = section(col, "بعد إنهاء المكالمة")
        pp.addView(sw("افتح نافذة الخيارات مباشرة بعد إنهاء المكالمة", "popup", true, null))

        val ic = section(col, "الأرقام من التطبيقات الثانية")
        ic.addView(sw("عند الضغط على رقم بتطبيق آخر (طلبات…) افتح خيارات حولني", "intercept", true, null))

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

    // قائمة الأزرار: كل زر يُعدَّل لحاله ويُحفظ فوراً، والاستعادة الافتراضية تحتاج تأكيداً
    private fun editTemplates() {
        val cur = Store.templates(this)
        val items = ArrayList<String>()
        for (t in cur) items.add(t.label + "\n" + t.text)
        items.add("➕ إضافة زر جديد")
        items.add("↩️ استعادة الأزرار الافتراضية")
        AlertDialog.Builder(this)
            .setTitle("أزرار الرسائل (اضغط على زر لتعديله)")
            .setItems(items.toTypedArray()) { _, w ->
                when {
                    w < cur.size -> editOne(w)
                    w == cur.size -> editOne(-1)
                    else -> confirmReset()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun editOne(idx: Int) {
        val cur = Store.templates(this)
        val old: Tpl? = if (idx >= 0 && idx < cur.size) cur[idx] else null
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.layoutDirection = View.LAYOUT_DIRECTION_RTL
        box.setPadding(dp(20), dp(10), dp(20), dp(4))
        val l = EditText(this)
        l.hint = "اسم الزر (مثال: 📍 لوكيشن)"
        l.setSingleLine()
        l.setText(old?.label ?: "")
        val t = EditText(this)
        t.hint = "نص الرسالة (اتركه فارغاً ليفتح المحادثة فقط)"
        t.minLines = 2
        t.setText(old?.text ?: "")
        box.addView(l)
        box.addView(t)
        val b = AlertDialog.Builder(this)
            .setTitle(if (old == null) "زر جديد" else "تعديل الزر")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("حفظ", null)
            .setNegativeButton("إلغاء") { _, _ -> editTemplates() }
        if (old != null) {
            b.setNeutralButton("🗑 حذف") { _, _ -> confirmDelete(idx) }
        }
        val d = b.create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val x = l.text.toString().trim()
            val y = t.text.toString().trim()
            if (x.isEmpty()) {
                toast("اكتب اسم الزر")
                return@setOnClickListener
            }
            val list = ArrayList(Store.templates(this))
            if (old != null && idx < list.size) {
                list[idx] = Tpl(x, y)
            } else {
                if (list.size >= 12) {
                    toast("الحد الأقصى 12 زراً")
                    return@setOnClickListener
                }
                list.add(Tpl(x, y))
            }
            Store.saveTemplates(this, list)
            d.dismiss()
            toast("تم الحفظ ✅")
            editTemplates()
        }
    }

    private fun confirmDelete(idx: Int) {
        val list = ArrayList(Store.templates(this))
        if (idx < 0 || idx >= list.size) {
            editTemplates()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("حذف هذا الزر؟")
            .setMessage(list[idx].label)
            .setPositiveButton("حذف") { _, _ ->
                if (list.size <= 1) {
                    toast("لازم يبقى زر واحد على الأقل")
                } else {
                    list.removeAt(idx)
                    Store.saveTemplates(this, list)
                    toast("تم الحذف")
                }
                editTemplates()
            }
            .setNegativeButton("إلغاء") { _, _ -> editTemplates() }
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("استعادة الأزرار الافتراضية؟")
            .setMessage("كل تعديلاتك على الأزرار راح تنحذف.")
            .setPositiveButton("استعادة") { _, _ ->
                Store.resetTemplates(this)
                toast("رجعت الأزرار الافتراضية ✅")
                editTemplates()
            }
            .setNegativeButton("إلغاء") { _, _ -> editTemplates() }
            .show()
    }
}
