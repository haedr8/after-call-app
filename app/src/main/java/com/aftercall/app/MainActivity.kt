package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusPill: TextView
    private lateinit var setupBanner: LinearLayout

    private val privacyUrl = "https://haedr8.github.io/after-call-app/privacy-play.html"

    private val tiktokUser = "haedr.m1"
    private val instagramUser = "haedr.m1"
    private val supportWa = "9647712384075"
    private fun dp(v: Int): Int = T.dp(this, v)

    private fun versionLabel(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1"
        } catch (e: Exception) {
            "1"
        }
    }

    private fun needed(): Array<String> = Phone.perms

    private fun listRow(parent: LinearLayout, emoji: String, text: String, last: Boolean, onClick: () -> Unit) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.layoutDirection = View.LAYOUT_DIRECTION_RTL
        r.setPadding(0, dp(13), 0, dp(13))
        r.isClickable = true
        r.addView(T.label(this, emoji, 20f, T.INK))
        val t = T.label(this, text, 15f, T.INK, true)
        t.setPadding(dp(12), 0, 0, 0)
        r.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.setOnClickListener { onClick() }
        parent.addView(r)
        if (!last) {
            val d = View(this)
            d.setBackgroundColor(T.LINE)
            parent.addView(d, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        }
    }

    private fun tileRow(a: View, b: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val p1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        p1.setMargins(dp(5), 0, dp(5), 0)
        val p2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        p2.setMargins(dp(5), 0, dp(5), 0)
        r.addView(a, p1)
        r.addView(b, p2)
        return r
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(T.BG)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(dp(16), dp(16), dp(16), dp(24))
        Ui.fit(col, dp(16), dp(16))

        // ---- الرأس: الشعار + الاسم + حالة التطبيق ----
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val logoBox = FrameLayout(this)
        logoBox.background = T.shape(this, T.TEAL, 14)
        logoBox.clipToOutline = true
        val img = ImageView(this)
        img.setImageResource(R.mipmap.ic_fg)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        logoBox.addView(img, FrameLayout.LayoutParams(dp(48), dp(48)))
        header.addView(logoBox, LinearLayout.LayoutParams(dp(48), dp(48)))
        val ht = LinearLayout(this)
        ht.orientation = LinearLayout.VERTICAL
        ht.setPadding(dp(12), 0, dp(12), 0)
        ht.addView(T.label(this, "حولني", 22f, T.INK, true))
        ht.addView(T.label(this, "الإصدار " + versionLabel(), 12f, T.MUTED))
        header.addView(ht, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        statusPill = T.label(this, "", 12f, T.OK, true)
        statusPill.setPadding(dp(12), dp(6), dp(12), dp(6))
        header.addView(statusPill)
        col.addView(header)

        // ---- شريط إكمال الإعداد (يظهر فقط عند وجود نقص) ----
        setupBanner = T.card(this)
        setupBanner.addView(T.label(this, "⚠️ يلزم إكمال الإعداد ليعمل التطبيق", 15f, T.WARN, true))
        setupBanner.addView(
            T.button(this, "فتح مركز الإعداد", true) { startActivity(Intent(this, SetupActivity::class.java)) },
            T.gap(this, 10)
        )
        col.addView(setupBanner, T.gap(this, 14))

        // ---- بطاقة المسح الكبيرة ----
        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.VERTICAL
        hero.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val hbg = GradientDrawable()
        hbg.colors = intArrayOf(T.TEAL, T.GREEN)
        hbg.orientation = GradientDrawable.Orientation.TL_BR
        hbg.cornerRadius = dp(28).toFloat()
        hero.background = hbg
        hero.setPadding(dp(22), dp(24), dp(22), dp(22))
        hero.addView(T.label(this, "📷", 40f, T.WHITE))
        hero.addView(T.label(this, "امسح رقم من ورقة", 24f, T.WHITE, true), T.gap(this, 6))
        hero.addView(
            T.label(this, "وجّه الكاميرا على الأوردر، ويظهر لك الاتصال والواتساب مباشرة. يقرأ الأرقام العربية والإنجليزية.", 14f, 0xEBFFFFFF.toInt()),
            T.gap(this, 6)
        )
        val go = T.button(this, "ابدأ المسح", false) { startActivity(Intent(this, ScanActivity::class.java)) }
        hero.addView(go, T.gap(this, 14))
        col.addView(hero, T.gap(this, 14))

        // ---- مربعات سريعة ----
        col.addView(
            tileRow(
                T.tile(this, "🕘", "سجل المكالمات") { startActivity(Intent(this, LogActivity::class.java)) },
                T.tile(this, "📞", "لوحة الأرقام") { startActivity(Intent(this, DialerActivity::class.java)) }
            ),
            T.gap(this, 12)
        )
        col.addView(
            tileRow(
                T.tile(this, "👤", "جهات الاتصال") { startActivity(Intent(this, ContactsActivity::class.java)) },
                T.tile(this, "💰", "مصاريفي") { startActivity(Intent(this, BudgetActivity::class.java)) }
            ),
            T.gap(this, 10)
        )

        // ---- قائمة ----
        val list = T.card(this)
        list.setPadding(dp(16), dp(4), dp(16), dp(4))
        listRow(list, "⚙️", "الإعدادات", false) { startActivity(Intent(this, SettingsActivity::class.java)) }
        listRow(list, "🧭", "مركز الإعداد والأذونات", false) { startActivity(Intent(this, SetupActivity::class.java)) }
        listRow(list, "ℹ️", "كيف يعمل التطبيق", false) { showHow() }
        listRow(list, "🔋", "نصائح التشغيل بالخلفية لهاتفك", false) { showOemHelp() }
        listRow(list, "🛠", "تشخيص المشاكل", false) { showDiag() }
        listRow(list, "🔒", "سياسة الخصوصية", true) { openUrl(privacyUrl) }
        col.addView(list, T.gap(this, 12))

        // ---- تواصل مع المطوّر ----
        val dev = T.card(this)
        dev.addView(T.label(this, "تواصل مع المطوّر", 16f, T.INK, true))
        dev.addView(T.label(this, "تصميم وتطوير: حيدر محمد", 13f, T.MUTED), T.gap(this, 2))
        val social = LinearLayout(this)
        social.orientation = LinearLayout.HORIZONTAL
        social.gravity = Gravity.CENTER
        social.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val c1 = T.chip(this, tiktokUser, R.drawable.ic_tiktok) { openTikTok() }
        val c2 = T.chip(this, instagramUser, R.drawable.ic_instagram) { openInstagram() }
        val sp1 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        sp1.setMargins(dp(6), 0, dp(6), 0)
        val sp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        sp2.setMargins(dp(6), 0, dp(6), 0)
        social.addView(c1, sp1)
        social.addView(c2, sp2)
        dev.addView(social, T.gap(this, 12))
        col.addView(dev, T.gap(this, 12))

        scroll.addView(
            col,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        setContentView(scroll)
        if (!Store.getBool(this, "wizard_done", false)) {
            Store.setBool(this, "wizard_done", true)
            startActivity(Intent(this, SetupActivity::class.java))
        } else showWelcomeOnce()
    }

    private fun disclosure() {
        AlertDialog.Builder(this)
            .setTitle("اجعل حولني تطبيق الهاتف")
            .setMessage(
                "«حولني» تطبيق اتصال كامل: لوحة أرقام، سجل مكالمات، واستقبال وإنهاء المكالمات.\n\n" +
                    "لهذا يطلب أن يكون تطبيق الهاتف الافتراضي، ويطلب صلاحيات: الهاتف، سجل المكالمات، " +
                    "جهات الاتصال، والاتصال، والإشعارات.\n\n" +
                    "يستخدمها فقط لعرض اسم ورقم المتصل والسجل، وإظهار رسائل واتساب جاهزة بعد كل مكالمة.\n\n" +
                    "• لا يسجّل المكالمات.\n" +
                    "• لا يرسل أي بيانات لأي جهة، كل شي يبقى على هاتفك.\n" +
                    "• تقدر ترجع تطبيق الهاتف القديم بأي وقت من إعدادات الهاتف."
            )
            .setPositiveButton("موافق") { _, _ -> Phone.requestDefault(this) }
            .setNegativeButton("ليس الآن", null)
            .show()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        if (rc == 1) {
            Grant.afterResult(this, needed())
            refresh()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && Phone.isDefault(this)) Grant.request(this, needed())
    }

    private fun showHow() {
        AlertDialog.Builder(this)
            .setTitle("كيف يعمل التطبيق")
            .setMessage(
                "1) فعّل «تطبيق الهاتف الافتراضي» من الشاشة الرئيسية.\n" +
                    "2) بعد انتهاء أي مكالمة يظهر إشعار «انتهت المكالمة» برقم الزبون.\n" +
                    "3) اضغطه واختر رسالة جاهزة (مثل: أنا وصلت) فتفتح محادثة واتساب مع الرسالة.\n\n" +
                    "تقدر تعدّل الرسائل من الإعدادات."
            )
            .setPositiveButton("تمام", null)
            .show()
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
        val log = getSharedPreferences("diag", MODE_PRIVATE).getString("log", "") ?: ""
        sb.append("\nسجل الأحداث:\n")
        sb.append(if (log.isBlank()) "(فارغ: لم يصل أي حدث للتطبيق)" else log)
        return sb.toString()
    }

    private fun showDiag() {
        val report = diagText()
        val tv = TextView(this)
        tv.text = report
        tv.textSize = 13f
        tv.setTextColor(0xFF000000.toInt())
        tv.setPadding(dp(20), dp(12), dp(20), dp(12))
        tv.layoutDirection = View.LAYOUT_DIRECTION_RTL
        tv.setTextIsSelectable(true)
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

    // رسالة تظهر مرة واحدة عند أول فتح، ومرة عند كل تحديث جديد
    private fun showWelcomeOnce() {
        val ver = try {
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        } catch (e: Exception) {
            0L
        }
        val sp = getSharedPreferences("welcome", MODE_PRIVATE)
        if (sp.getLong("shown_ver", -1L) == ver) return
        sp.edit().putLong("shown_ver", ver).apply()
        val tv = TextView(this)
        tv.text = "هذا التطبيق صدقة جارية، مجاني بالكامل 🌹\n\nإذا نفعك، ادعُ لي ولوالديّ بالخير 🤲"
        tv.textSize = 17f
        tv.setTextColor(0xFF000000.toInt())
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(20), dp(16), dp(20), dp(8))
        tv.layoutDirection = View.LAYOUT_DIRECTION_RTL
        AlertDialog.Builder(this)
            .setView(tv)
            .setPositiveButton("آمين 🤍", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val fsOk = Build.VERSION.SDK_INT < 34 ||
            (getSystemService(android.app.NotificationManager::class.java)?.canUseFullScreenIntent() ?: true)
        val ok = Phone.isDefault(this) && fsOk &&
            needed().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        statusPill.text = if (ok) "✅ كل شيء يعمل" else "⚠️ يحتاج إعداد"
        statusPill.setTextColor(if (ok) T.OK else T.WARN)
        statusPill.background = T.shape(this, if (ok) 0xFFE3F5EA.toInt() else 0xFFFEF3C7.toInt(), 18)
        setupBanner.visibility = if (ok) View.GONE else View.VISIBLE
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

    private fun showOemHelp() {
        AlertDialog.Builder(this)
            .setTitle("حتى لا يوقف الهاتف التطبيق")
            .setMessage(Oem.allTips())
            .setPositiveButton("فتح الإعدادات") { _, _ -> Oem.openAutostart(this) }
            .setNegativeButton("إغلاق", null)
            .show()
    }
}
