package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object Oem {
    fun allTips(): String =
        "سامسونج: الإعدادات ← البطارية ← حدود استخدام الخلفية ← التطبيقات التي لا تنام أبداً ← أضف «حولني».\n\n" +
            "شاومي / ريدمي / بوكو: فعّل «التشغيل التلقائي» لحولني، ثم في البطارية اختر «بدون قيود».\n\n" +
            "أوبو / ريلمي / ون بلس: فعّل «السماح بالتشغيل التلقائي» و«السماح بالنشاط في الخلفية».\n\n" +
            "فيفو: فعّل «التشغيل التلقائي» و«استهلاك عالي للبطارية في الخلفية».\n\n" +
            "تكنو / إنفينكس / إيتل: Phone Master ← إدارة التشغيل التلقائي ← فعّل «حولني»، وفي البطارية اختر «بدون قيود»، واقفل التطبيق 🔒 في التطبيقات الأخيرة.\n\n" +
            "هواوي / هونر: إدارة التشغيل ← حولني ← إدارة يدوية ← فعّل الثلاثة.\n\n" +
            "وفي كل الهواتف: اقفل التطبيق في قائمة التطبيقات الأخيرة (🔒) إن وُجد الخيار."

    fun tip(): String {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            m.contains("samsung") ->
                "سامسونج: الإعدادات ← البطارية ← حدود استخدام الخلفية ← التطبيقات التي لا تنام أبداً ← أضف «حولني»."
            m.contains("tecno") || m.contains("infinix") || m.contains("itel") ->
                "Phone Master ← إدارة التشغيل التلقائي ← فعّل «حولني». وفي البطارية اختر «بدون قيود»، واقفل التطبيق 🔒 في التطبيقات الأخيرة."
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
                "فعّل «التشغيل التلقائي» لحولني، ثم في البطارية اختر «بدون قيود»."
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ->
                "فعّل «السماح بالتشغيل التلقائي» و«السماح بالنشاط في الخلفية»."
            m.contains("vivo") || m.contains("iqoo") ->
                "فعّل «التشغيل التلقائي» و«استهلاك عالي للبطارية في الخلفية»."
            m.contains("huawei") || m.contains("honor") ->
                "إدارة التشغيل ← حولني ← إدارة يدوية ← فعّل الثلاثة."
            else ->
                "افتح إعدادات البطارية للتطبيق واختر «بدون قيود»، وفعّل التشغيل التلقائي إن وُجد."
        }
    }

    fun openAutostart(a: Activity) {
        val tries = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity",
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutostartActivity"
        )
        for ((pkg, cls) in tries) {
            try {
                a.startActivity(Intent().setComponent(ComponentName(pkg, cls)))
                return
            } catch (e: Exception) {
            }
        }
        try {
            a.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName))
            )
        } catch (e: Exception) {
        }
    }
}

object Wizard {
    private class Step(val title: String, val msg: String, val label: String, val act: (Activity) -> Unit)

    fun start(a: Activity) {
        Store.setBool(a, "wizard_done", true)
        show(a, 0)
    }

    private fun perms(a: Activity) {
        val l = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
        a.requestPermissions(l.toTypedArray(), 1)
    }

    private fun open(a: Activity, i: Intent) {
        try {
            a.startActivity(i)
        } catch (e: Exception) {
        }
    }

    private fun steps(): List<Step> = listOf(
        Step(
            "الصلاحيات",
            "اضغط «السماح» ووافق على كل الصلاحيات (الهاتف، سجل المكالمات، جهات الاتصال، الإشعارات). بدونها التطبيق ما يشتغل.",
            "السماح"
        ) { a -> perms(a) },
        Step(
            "البطارية",
            "اضغط «فتح»، ثم اختر «كل التطبيقات» ← حولني ← «عدم التحسين». هذا يمنع الهاتف من إيقاف التطبيق.",
            "فتح"
        ) { a -> open(a, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        Step(
            "التشغيل التلقائي",
            Oem.tip(),
            "فتح الإعدادات"
        ) { a -> Oem.openAutostart(a) },
        Step(
            "مراقبة واتساب (اختياري)",
            "لو تريد النافذة بعد مكالمات واتساب أيضاً: فعّل «حولني» بقائمة الوصول للإشعارات. إذا الخيار رمادي: معلومات التطبيق ← ⋮ ← «السماح بالإعدادات المقيّدة» ثم ارجع.",
            "فتح"
        ) { a -> open(a, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        Step(
            "النافذة العائمة (اختياري)",
            "تظهر نافذة صغيرة فوق الشاشة بعد المكالمة. اضغط «تفعيل» ثم اسمح بـ«العرض فوق التطبيقات».",
            "تفعيل"
        ) { a ->
            Store.setBool(a, "bubble", true)
            open(
                a,
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.packageName))
            )
        },
        Step(
            "جاهز ✅",
            "سوّي مكالمة تجربة وأنهها، ويطلع لك إشعار «انتهت المكالمة». تقدر تعدّل الرسائل والخيارات من «الإعدادات».",
            "فتح الإعدادات"
        ) { a -> open(a, Intent(a, SettingsActivity::class.java)) }
    )

    private fun show(a: Activity, i: Int) {
        val all = steps()
        if (i >= all.size) return
        val s = all[i]
        val last = i == all.size - 1
        val d = AlertDialog.Builder(a)
            .setTitle("الخطوة " + (i + 1) + " من " + all.size + ": " + s.title)
            .setMessage(s.msg)
            .setCancelable(false)
            .setPositiveButton(if (last) "إنهاء" else "التالي ▶") { _, _ ->
                if (!last) show(a, i + 1)
            }
            .setNeutralButton(s.label, null)
            .setNegativeButton("إغلاق", null)
            .create()
        d.show()
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { s.act(a) }
    }
}
