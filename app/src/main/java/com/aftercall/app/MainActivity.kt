package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
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

    private val tiktokUser = "haedr.m1"
    private val instagramUser = "haedr.m1"
    private val supportWa = "9647712384075"

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun versionLabel(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1"
        } catch (e: Exception) {
            "1"
        }
    }

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

    private fun iconBtn(t: String, icon: Int, onClick: () -> Unit): Button = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(0xFF000000.toInt())
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        compoundDrawablePadding = dp(8)
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

        col.addView(btn("🛠 تشخيص المشاكل / إرسال تقرير") { showDiag() })
        col.addView(btn("سياسة الخصوصية") { openUrl(privacyUrl) })

        col.addView(text("الإصدار " + versionLabel(), 13f, 0xFF555555.toInt()))
        col.addView(btn("🔄 فحص التحديثات") { checkUpdates(true) })

        col.addView(text("تصميم وتطوير: حيدر محمد", 15f, 0xFF000000.toInt()))

        val social = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val lp1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val lp2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        social.addView(iconBtn(tiktokUser, R.drawable.ic_tiktok) { openTikTok() }, lp1)
        social.addView(iconBtn(instagramUser, R.drawable.ic_instagram) { openInstagram() }, lp2)
        col.addView(social, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        scroll.addView(col, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
        checkUpdates(false)
    }

    private fun diagText(): String {
        val sb = StringBuilder()
        sb.append("تقرير تطبيق حولني\n")
        sb.append("الإصدار: ").append(versionLabel()).append("\n")
        sb.append("الجهاز: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
        sb.append("أندرويد: ").append(Build.VERSION.RELEASE)
            .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("\nالصلاحيات:\n")
        for (p in needed()) {
            val ok = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
            sb.append(if (ok) "✅ " else "❌ ").append(p.substringAfterLast('.')).append("\n")
        }
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        sb.append(if (enabled.contains(packageName)) "✅" else "❌").append(" مراقبة واتساب\n")
        val pm = getSystemService(PowerManager::class.java)
        val free = pm != null && pm.isIgnoringBatteryOptimizations(packageName)
        sb.append(if (free) "✅" else "❌").append(" الخلفية (البطارية)\n")
        val log = getSharedPreferences("diag", MODE_PRIVATE).getString("log", "") ?: ""
        sb.append("\nسجل الأحداث:\n")
        sb.append(if (log.isBlank()) "(فارغ: لم يصل أي حدث للتطبيق)" else log)
        return sb.toString()
    }

    private fun showDiag() {
        val report = diagText()
        val tv = TextView(this).apply {
            text = report
            textSize = 13f
            setTextColor(0xFF000000.toInt())
            setPadding(dp(20), dp(12), dp(20), dp(12))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setTextIsSelectable(true)
        }
        val sv = ScrollView(this)
        sv.addView(tv)
        AlertDialog.Builder(this)
            .setTitle("تشخيص المشاكل")
            .setView(sv)
            .setPositiveButton("📋 نسخ") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("تقرير حولني", report))
                Toast.makeText(this, "تم النسخ ✅ الصقه وأرسله", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("📲 إرسال لحيدر") { _, _ -> sendReport(report) }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun sendReport(report: String) {
        val url = "https://wa.me/" + supportWa + "?text=" + Uri.encode(report)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(this, "واتساب غير موجود، استخدم زر النسخ", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkUpdates(manual: Boolean) {
        if (manual) Toast.makeText(this, "جاري الفحص...", Toast.LENGTH_SHORT).show()
        Updater.check(this) { info, error ->
            if (!isFinishing && !isDestroyed) {
                if (info != null) {
                    showUpdateDialog(info)
                } else if (manual) {
                    val msg = if (error) "تعذر الفحص، تأكد من الإنترنت" else "أنت على آخر إصدار ✅"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        val notes = if (info.notes.isBlank()) "تحسينات عامة" else info.notes
        AlertDialog.Builder(this)
            .setTitle("تحديث جديد متوفر")
            .setMessage("ما الجديد:\n" + notes)
            .setPositiveButton("تنزيل التحديث") { _, _ -> startDownload(info) }
            .setNegativeButton("لاحقاً", null)
            .show()
    }

    private fun startDownload(info: UpdateInfo) {
        if (!Updater.canInstall(this)) {
            Toast.makeText(
                this,
                "فعّل «السماح من هذا المصدر» ثم ارجع واضغط فحص التحديثات",
                Toast.LENGTH_LONG
            ).show()
            Updater.openInstallSettings(this)
            return
        }
        val d = AlertDialog.Builder(this)
            .setTitle("جاري تنزيل التحديث")
            .setMessage("0%")
            .setCancelable(false)
            .show()
        Updater.download(this, info, { p -> d.setMessage("$p%") }, { f ->
            d.dismiss()
            if (f != null) {
                Updater.install(this, f)
            } else {
                Toast.makeText(this, "فشل التنزيل، حاول مرة ثانية", Toast.LENGTH_LONG).show()
            }
        })
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
                Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/$instagramUser"))
                    .setPackage("com.instagram.android")
            )
        } catch (e: Exception) {
            openUrl("https://instagram.com/$instagramUser")
        }
    }

    private fun openTikTok() {
        val url = Uri.parse("https://www.tiktok.com/@$tiktokUser")
        val packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        for (p in packages) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url).setPackage(p))
                return
            } catch (e: Exception) {
            }
        }
        openUrl("https://www.tiktok.com/@$tiktokUser")
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
                    "تكنو / إنفينكس / إيتل: Phone Master ← إدارة التشغيل التلقائي ← فعّل «حولني»، وفي البطارية اختر «بدون قيود»، واقفل التطبيق 🔒 في التطبيقات الأخيرة.\n\n" +
