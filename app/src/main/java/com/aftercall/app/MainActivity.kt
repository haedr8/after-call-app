package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var waStatus: TextView
    private lateinit var batStatus: TextView

    private val privacyUrl = "https://haedr8.github.io/after-call-app/privacy.html"

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun needed(): Array<String> {
        val l = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
        return l.toTypedArray()
    }

    private fun text(t: String, size: Float, color: Int): TextView = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        gravity = Gravity.CENTER
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun btn(t: String, onClick: () -> Unit): Button = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(0xFF000000.toInt())
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(0xFFFFFFFF.toInt())

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        col.setOnApplyWindowInsetsListener { v, ins ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val b = ins.getInsets(WindowInsets.Type.systemBars())
                top = b.top
                bottom = b.bottom
            } else {
                @Suppress("DEPRECATION")
                top = ins.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = ins.systemWindowInsetBottom
            }
            v.setPadding(dp(20), top + dp(24), dp(20), bottom + dp(24))
            ins
        }

        val box = FrameLayout(this)
        box.background = GradientDrawable().apply {
            cornerRadius = dp(34).toFloat()
            colors = intArrayOf(0xFF0B7A86.toInt(), 0xFF1CC47A.toInt())
            orientation = GradientDrawable.Orientation.TL_BR
        }
        box.clipToOutline = true
        val img = ImageView(this)
        img.setImageResource(R.mipmap.ic_fg)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        box.addView(img, FrameLayout.LayoutParams(dp(140), dp(140), Gravity.CENTER))
        col.addView(box, LinearLayout.LayoutParams(dp(140), dp(140)))

        col.addView(text("حولني", 28f, 0xFF000000.toInt()))
        col.addView(text("خيارات سريعة بعد كل مكالمة", 15f, 0xFF555555.toInt()))

        status = text("", 16f, 0xFF000000.toInt())
        col.addView(status)
        col.addView(btn("منح الصلاحيات") { requestPermissions(needed(), 1) })

        waStatus = text("", 16f, 0xFF000000.toInt())
        col.addView(waStatus)
        col.addView(btn("تفعيل مراقبة واتساب") {
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (e: Exception) {
                openAppInfo()
            }
        })
        if (Build.VERSION.SDK_INT >= 33) {
            col.addView(btn("الخيار رمادي ولا يمكن تفعيله؟") { showRestrictedHelp() })
        }

        batStatus = text("", 16f, 0xFF000000.toInt())
        col.addView(batStatus)
        col.addView(btn("السماح بالعمل في الخلفية (البطارية)") {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                Toast.makeText(
                    this, "اختر «كل التطبيقات» ثم حولني ثم «عدم التحسين»", Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                openAppInfo()
            }
        })
        col.addView(btn("التشغيل التلقائي (شاومي / أوبو / فيفو / هواوي / سامسونج)") { showOemHelp() })

        col.addView(btn("سياسة الخصوصية") { openUrl(privacyUrl) })

        col.addView(text("تصميم وتطوير: حيدر محمد", 15f, 0xFF000000.toInt()))
        col.addView(btn("📷 haedr.m1") { openInstagram() })

        scroll.addView(col, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val ok = needed().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        status.text = if (ok) "✅ الصلاحيات ممنوحة" else "⚠️ الصلاحيات ناقصة"

        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        waStatus.text = if (enabled.contains(packageName)) {
            "✅ مراقبة مكالمات واتساب مفعّلة"
        } else {
            "⚠️ مراقبة مكالمات واتساب غير مفعّلة"
        }

        val pm = getSystemService(PowerManager::class.java)
        val free = pm != null && pm.isIgnoringBatteryOptimizations(packageName)
        batStatus.text = if (free) "✅ الخلفية مسموحة" else "⚠️ البطارية قد توقف التطبيق"
    }

    private fun openAppInfo() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInstagram() {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/haedr.m1"))
                    .setPackage("com.instagram.android")
            )
        } catch (e: Exception) {
            openUrl("https://instagram.com/haedr.m1")
        }
    }

    private fun showRestrictedHelp() {
        AlertDialog.Builder(this)
            .setTitle("السماح بالإعدادات المقيّدة")
            .setMessage(
                "أندرويد يمنع التطبيقات المثبتة من خارج المتجر من مراقبة الإشعارات.\n\n" +
                    "1) اضغط «فتح معلومات التطبيق»\n" +
                    "2) اضغط النقاط الثلاث ⋮ أعلى الشاشة\n" +
                    "3) اختر «السماح بالإعدادات المقيّدة»\n" +
                    "4) ارجع وفعّل «مراقبة واتساب»"
            )
            .setPositiveButton("فتح معلومات التطبيق") { _, _ -> openAppInfo() }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun showOemHelp() {
        AlertDialog.Builder(this)
            .setTitle("حتى لا يوقف الهاتف التطبيق")
            .setMessage(
                "سامسونج: الإعدادات ← البطارية ← حدود استخدام الخلفية ← التطبيقات التي لا تنام أبداً ← أضف «حولني».\n\n" +
                    "شاومي / ريدمي / بوكو: فعّل «التشغيل التلقائي» لحولني، ثم في البطارية اختر «بدون قيود».\n\n" +
                    "أوبو / ريلمي / ون بلس: فعّل «السماح بالتشغيل التلقائي» و«السماح بالنشاط في الخلفية».\n\n" +
                    "فيفو: فعّل «التشغيل التلقائي» و«استهلاك عالي للبطارية في الخلفية».\n\n" +
                    "هواوي / هونر: إدارة التشغيل ← حولني ← إدارة يدوية ← فعّل الثلاثة.\n\n" +
                    "وفي كل الهواتف: اقفل التطبيق في قائمة التطبيقات الأخيرة (🔒) إن وُجد الخيار."
            )
            .setPositiveButton("فتح الإعدادات") { _, _ -> openAutostart() }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun openAutostart() {
        val tries = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"
        )
        for ((pkg, cls) in tries) {
            try {
                startActivity(Intent().setComponent(ComponentName(pkg, cls)))
                return
            } catch (e: Exception) {
            }
        }
        openAppInfo()
    }
}
