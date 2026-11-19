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

    private class SRow(val label: TextView, val fix: Button)

    private lateinit var rPerm: SRow
    private lateinit var rDef: SRow

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

    private fun statusRow(parent: LinearLayout, fixText: String, onFix: () -> Unit): SRow {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val tv = T.label(this, "", 15f, T.INK, true)
        val b = T.button(this, fixText, false, onFix)
        b.textSize = 13f
        b.minHeight = 0
        b.minimumHeight = 0
        b.setPadding(dp(14), dp(6), dp(14), dp(6))
        row.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)))
        parent.addView(row, T.gap(this, 10))
        return SRow(tv, b)
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
        col.setPadding(dp(16), dp(16), dp(16), dp(16))
        Ui.fit(col, dp(16), dp(16))

        // ---- البطاقة العلوية ----
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val hbg = GradientDrawable()
        hbg.colors = intArrayOf(T.TEAL, T.GREEN)
        hbg.orientation = GradientDrawable.Orientation.TL_BR
        hbg.cornerRadius = dp(28).toFloat()
        header.background = hbg
        header.setPadding(dp(18), dp(18), dp(18), dp(18))

        val logoBox = FrameLayout(this)
        logoBox.background = T.shape(this, 0x33FFFFFF, 20)
        logoBox.clipToOutline = true
        val img = ImageView(this)
        img.setImageResource(R.mipmap.ic_fg)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        logoBox.addView(img, FrameLayout.LayoutParams(dp(76), dp(76)))
        header.addView(logoBox, LinearLayout.LayoutParams(dp(76), dp(76)))

        val ht = LinearLayout(this)
        ht.orientation = LinearLayout.VERTICAL
        ht.setPadding(dp(14), 0, dp(14), 0)
        ht.addView(T.label(this, "حولني", 26f, T.WHITE, true))
        ht.addView(T.label(this, "خيارات سريعة بعد كل مكالمة", 13f, 0xE6FFFFFF.toInt()))
        val ver = T.label(this, "الإصدار " + versionLabel(), 12f, T.WHITE)
        ver.background = T.shape(this, 0x33FFFFFF, 12)
        ver.setPadding(dp(10), dp(3), dp(10), dp(3))
        val vp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        vp.topMargin = dp(8)
        ht.addView(ver, vp)
        header.addView(ht, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(header, T.gap(this, 0))

        // ---- حالة التطبيق ----
        val st = T.card(this)
        st.addView(T.label(this, "حالة التطبيق", 16f, T.INK, true))
        rDef = statusRow(st, "تفعيل") { disclosure() }
        rPerm = statusRow(st, "منح") { requestPermissions(needed(), 1) }
        col.addView(st, T.gap(this, 14))

        // ---- الأدوات ----
        col.addView(T.label(this, "الأدوات", 14f, T.MUTED, true), T.gap(this, 18))
        col.addView(
            tileRow(
                T.tile(this, "📞", "لوحة الأرقام") {
                    startActivity(Intent(this, DialerActivity::class.java))
                },
                T.tile(this, "🕘", "سجل المكالمات") {
                    startActivity(Intent(this, LogActivity::class.java))
                }
            ),
            T.gap(this, 8)
        )
        col.addView(
            tileRow(
                T.tile(this, "⚙️", "الإعدادات") {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                T.tile(this, "👤", "جهات الاتصال") {
                    startActivity(Intent(this, ContactsActivity::class.java))
                }
            ),
            T.gap(this, 8)
        )
        col.addView(
            tileRow(
                T.tile(this, "🧭", "دليل الإعداد") { Wizard.start(this) },
                T.tile(this, "ℹ️", "كيف يعمل التطبيق") { showHow() }
            ),
            T.gap(this, 10)
        )
        col.addView(
            tileRow(
                T.tile(this, "🛠", "تشخيص المشاكل") { showDiag() },
                T.tile(this, "🔒", "سياسة الخصوصية") { openUrl(privacyUrl) }
            ),
            T.gap(this, 10)
        )
        col.addView(T.button(this, "🔋 نصائح التشغيل بالخلفية لهاتفك", false) { showOemHelp() }, T.gap(this, 10))

        // ---- الدعم ----

        // ---- التذييل ----
        val credit = T.label(this, "تصميم وتطوير: حيدر محمد", 14f, T.MUTED)
        credit.gravity = Gravity.CENTER
        col.addView(credit, T.gap(this, 22))

        val social = LinearLayout(this)
        social.orientation = LinearLayout.HORIZONTAL
        social.gravity = Gravity.CENTER
        social.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val c1 = T.chip(this, tiktokUser, R.drawable.ic_tiktok) { openTikTok() }
        val c2 = T.chip(this, instagramUser, R.drawable.ic_instagram) { openInstagram() }
        val sp1 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sp1.setMargins(dp(6), 0, dp(6), 0)
        val sp2 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sp2.setMargins(dp(6), 0, dp(6), 0)
        social.addView(c1, sp1)
        social.addView(c2, sp2)
        col.addView(social, T.gap(this, 10))

        scroll.addView(
            col,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)
        if (!Store.getBool(this, "wizard_done", false)) Wizard.start(this)
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 77 && Phone.isDefault(this)) requestPermissions(needed(), 1)
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

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun setRow(r: SRow, ok: Boolean, okText: String, badText: String) {
        r.label.text = if (ok) "✅ " + okText else "⚠️ " + badText
        r.label.setTextColor(if (ok) T.OK else T.WARN)
        r.fix.visibility = if (ok) View.GONE else View.VISIBLE
    }

    private fun refresh() {
        setRow(rDef, Phone.isDefault(this), "حولني هو تطبيق الهاتف", "حولني ليس تطبيق الهاتف الافتراضي")
        val ok = needed().all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        setRow(rPerm, ok, "الصلاحيات ممنوحة", "الصلاحيات ناقصة")

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
