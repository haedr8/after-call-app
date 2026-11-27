package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

// مركز الإعداد: كل ما يحتاجه التطبيق في شاشة واحدة.
// - يفحص الحالة الفعلية في كل مرة تفتح الشاشة (وعند الرجوع من إعدادات النظام)
// - زر واحد كبير يكمل الخطوة التالية تلقائيًا
// - إذا منع النظام نافذة الإذن، يفتح صفحة الإذن الصحيحة بدل ما يعلق
class SetupActivity : Activity() {

    private class Item(
        val key: String,
        val title: String,
        val desc: String,
        val required: Boolean,
        val ok: () -> Boolean,
        val act: () -> Unit
    )

    private lateinit var body: LinearLayout
    private lateinit var scroll: ScrollView
    private var autoAsked = false

    private fun dp(v: Int) = T.dp(this, v)

    // ---------- فحوصات ----------
    private fun permsOk(p: Array<String>) = p.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun fullScreenOk(): Boolean = Build.VERSION.SDK_INT < 34 ||
        (getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true)

    private fun batteryOk(): Boolean = try {
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    } catch (e: Exception) {
        true
    }

    private val camPerm = arrayOf(Manifest.permission.CAMERA)
    private val locPerm = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun missingNames(): String {
        val m = Grant.missing(this, Phone.perms)
        val names = LinkedHashSet<String>()
        for (p in m) {
            when (p) {
                Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE -> names.add("الهاتف")
                Manifest.permission.READ_CALL_LOG -> names.add("سجل المكالمات")
                Manifest.permission.READ_CONTACTS -> names.add("جهات الاتصال")
                Manifest.permission.POST_NOTIFICATIONS -> names.add("الإشعارات")
            }
        }
        return names.joinToString("، ")
    }

    private fun requestPerms(p: Array<String>, code: Int) {
        // Grant يطلب النافذة أول مرة، وإذا النظام يمنعها يفتح صفحة الأذونات مباشرة
        Grant.request(this, p, code)
    }

    private fun items(): List<Item> {
        val l = ArrayList<Item>()
        l.add(
            Item(
                "default", "تطبيق الهاتف الافتراضي",
                "حتى تظهر شاشة المكالمة وخيارات ما بعد المكالمة.", true,
                { Phone.isDefault(this) }, { Phone.requestDefault(this) }
            )
        )
        l.add(
            Item(
                "perms", "الصلاحيات الأساسية",
                if (permsOk(Phone.perms)) "الهاتف، السجل، الجهات، الإشعارات."
                else "ناقص: " + missingNames(), true,
                { permsOk(Phone.perms) }, { requestPerms(Phone.perms, 1) }
            )
        )
        if (Build.VERSION.SDK_INT >= 34) {
            l.add(
                Item(
                    "fs", "عرض المكالمة الواردة فوق الشاشة",
                    "حتى تظهر المكالمة حتى لو الهاتف مقفول.", true,
                    { fullScreenOk() }, { Grant.fullScreen(this) }
                )
            )
        }
        l.add(
            Item(
                "camera", "الكاميرا (لمسح الأرقام)",
                "اختياري: تُستخدم فقط عند فتح «مسح رقم من ورقة».", false,
                { permsOk(camPerm) }, { requestPerms(camPerm, 2) }
            )
        )
        l.add(
            Item(
                "loc", "الموقع (لزر «موقعي»)",
                "اختياري: لإرسال موقعك للزبون بضغطة.", false,
                { permsOk(locPerm) }, { requestPerms(locPerm, 3) }
            )
        )
        l.add(
            Item(
                "battery", "عدم إيقاف التطبيق بالخلفية",
                Oem.tip(), false,
                { batteryOk() }, { Oem.requestBattery(this) }
            )
        )
        return l
    }

    // ---------- واجهة ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this)
        scroll.setBackgroundColor(T.BG)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.layoutDirection = View.LAYOUT_DIRECTION_RTL
        body.setPadding(dp(16), dp(16), dp(16), dp(24))
        Ui.fit(body, dp(16), dp(16))
        scroll.addView(body, ViewGroup.LayoutParams(-1, -2))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        body.removeAllViews()
        val all = items()
        val req = all.filter { it.required }
        val reqDone = req.count { it.ok() }
        val allDone = reqDone == req.size
        val next = req.firstOrNull { !it.ok() }

        // رأس: عنوان + تقدم
        body.addView(T.label(this, "مركز الإعداد", 26f, T.INK, true))
        body.addView(
            T.label(
                this,
                if (allDone) "كل شيء جاهز ✅" else "أكمل الخطوات الأساسية ليعمل التطبيق بالكامل",
                14f, T.MUTED
            ), T.gap(this, 4)
        )
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        pb.max = req.size
        pb.progress = reqDone
        body.addView(pb, T.gap(this, 12))
        body.addView(T.label(this, "اكتمل $reqDone من ${req.size} (الأساسية)", 13f, T.MUTED), T.gap(this, 4))

        // زر الخطوة التالية
        if (next != null) {
            body.addView(
                T.button(this, "⚡ أكمل الخطوة التالية: " + next.title, true) { next.act() },
                T.gap(this, 14)
            )
        } else {
            body.addView(T.button(this, "تم، ارجع للرئيسية", true) { finish() }, T.gap(this, 14))
        }

        // البطاقات
        for (it in all) body.addView(card(it), T.gap(this, 10))

        // مساعدة
        val help = T.card(this)
        help.addView(T.label(this, "إذا ما ظهرت نافذة الإذن", 15f, T.INK, true))
        help.addView(
            T.label(
                this,
                "بعض الأجهزة تمنع النافذة بعد رفضها مرتين. اضغط الزر وافتح «الأذونات» ثم اسمح بكل شي.",
                13f, T.MUTED
            ), T.gap(this, 6)
        )
        help.addView(T.button(this, "فتح إعدادات التطبيق", false) { Grant.appSettings(this) }, T.gap(this, 10))
        help.addView(
            T.button(this, "التشغيل التلقائي (شاومي/أوبو/فيفو/هونر…)", false) { Oem.openAutostart(this) },
            T.gap(this, 8)
        )
        body.addView(help, T.gap(this, 14))
    }

    private fun card(it: Item): LinearLayout {
        val ok = it.ok()
        val c = T.card(this)
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val mark = T.label(this, if (ok) "✅" else if (it.required) "⚠️" else "⚪", 22f, T.INK)
        top.addView(mark)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(10), 0, dp(10), 0)
        col.addView(T.label(this, it.title + if (it.required) "" else "  (اختياري)", 16f, T.INK, true))
        col.addView(T.label(this, it.desc, 13f, T.MUTED))
        top.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        c.addView(top)
        if (!ok) {
            c.addView(T.button(this, "تفعيل", it.required) { it.act() }, T.gap(this, 10))
        }
        return c
    }

    // ---------- نتائج ----------
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        when (rc) {
            1 -> Grant.afterResult(this, Phone.perms)
            2 -> Grant.afterResult(this, camPerm)
            3 -> Grant.afterResult(this, locPerm)
        }
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        render()
        // بعد تفعيل تطبيق الهاتف نطلب الصلاحيات تلقائيًا مرة واحدة
        if (requestCode == 77 && Phone.isDefault(this) && !autoAsked && !permsOk(Phone.perms)) {
            autoAsked = true
            requestPerms(Phone.perms, 1)
        }
    }
}
