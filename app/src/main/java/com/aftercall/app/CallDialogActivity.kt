package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CallDialogActivity : Activity() {

    private class Opt(val label: String, val action: () -> Unit)

    private var number = ""
    private var name = ""
    private var noteView: TextView? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        number = intent.getStringExtra("number") ?: ""
        name = intent.getStringExtra("name") ?: ""
        if (number.isBlank()) {
            val last = readLastCall(this, 0L)
            if (last != null) {
                number = last.number
                name = last.name
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }

        root.addView(TextView(this).apply {
            text = when {
                name.isNotBlank() -> name
                number.isNotBlank() -> number
                else -> "المكالمة"
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(0xFF000000.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        if (number.isNotBlank()) {
            if (name.isNotBlank()) {
                root.addView(TextView(this).apply {
                    text = number
                    textDirection = View.TEXT_DIRECTION_LTR
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(0xFF444444.toInt())
                    gravity = Gravity.CENTER
                })
            }
            val nv = TextView(this).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(0xFFB45309.toInt())
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, dp(4))
            }
            noteView = nv
            root.addView(nv)
            refreshNote()

            val t = Store.templates(this)
            var i = 0
            while (i < t.size) {
                val a = t[i]
                val b = if (i + 1 < t.size) t[i + 1] else null
                addRow(
                    root,
                    Opt(a.label) { openWhatsApp(a.text) },
                    if (b != null) Opt(b.label) { openWhatsApp(b.text) } else null
                )
                i += 2
            }
            addRow(root, Opt("📍 موقعي") { sendLocation() }, Opt("📝 ملاحظة") { editNote() })
            addRow(root, Opt("✉️ SMS") { openSms() }, Opt("📞 إعادة اتصال") { callBack() })
            addRow(root, Opt("إلغاء") { finish() }, null)
        } else {
            root.addView(TextView(this).apply {
                text = "لا يوجد رقم لهذه المكالمة. تأكد من منح الصلاحيات من داخل التطبيق."
                setTextColor(0xFF444444.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
            })
            addRow(root, Opt("إلغاء") { finish() }, null)
        }

        val sv = android.widget.ScrollView(this)
        sv.addView(root)
        setContentView(sv)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(true)
    }

    private fun refreshNote() {
        val n = Store.getNote(this, number)
        noteView?.text = if (n.isBlank()) "" else "📝 $n"
        noteView?.visibility = if (n.isBlank()) View.GONE else View.VISIBLE
    }

    private fun editNote() {
        val et = EditText(this)
        et.hint = "مثال: عمارة 5 / الطابق 3 / يحتاج نداء"
        et.layoutDirection = View.LAYOUT_DIRECTION_RTL
        et.setText(Store.getNote(this, number))
        AlertDialog.Builder(this)
            .setTitle("ملاحظة عن هذا الرقم")
            .setView(et)
            .setPositiveButton("حفظ") { _, _ ->
                Store.setNote(this, number, et.text.toString())
                refreshNote()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun makeBtn(o: Opt): Button = Button(this).apply {
        text = o.label
        isAllCaps = false
        maxLines = 1
        setTextColor(0xFF000000.toInt())
        setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP)
        setOnClickListener { o.action() }
    }

    private fun addRow(root: LinearLayout, a: Opt, b: Opt?) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        fun lp(): LinearLayout.LayoutParams {
            val p = LinearLayout.LayoutParams(0, dp(52), 1f)
            p.setMargins(dp(4), dp(4), dp(4), dp(4))
            return p
        }
        row.addView(makeBtn(a), lp())
        if (b != null) row.addView(makeBtn(b), lp())
        root.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun lastKnown(lm: LocationManager): Location? {
        var best: Location? = null
        for (p in lm.getProviders(true)) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.time > best.time)) best = l
            } catch (e: SecurityException) {
            }
        }
        return best
    }

    private fun shareLoc(l: Location) {
        openWhatsApp(
            "سلام عليكم، موقعي الحالي:\nhttps://maps.google.com/?q=" + l.latitude + "," + l.longitude
        )
    }

    private fun sendLocation() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                21
            )
            return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best = lastKnown(lm)
        if (best != null && System.currentTimeMillis() - best.time < 120000) {
            shareLoc(best)
            return
        }
        if (Build.VERSION.SDK_INT >= 30) {
            val prov = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull {
                    try {
                        lm.isProviderEnabled(it)
                    } catch (e: Exception) {
                        false
                    }
                }
            if (prov != null) {
                Toast.makeText(this, "جاري تحديد الموقع...", Toast.LENGTH_SHORT).show()
                try {
                    lm.getCurrentLocation(prov, null, mainExecutor) { loc ->
                        if (loc != null) {
                            shareLoc(loc)
                        } else if (best != null) {
                            shareLoc(best)
                        } else {
                            Toast.makeText(this, "تعذر تحديد الموقع", Toast.LENGTH_LONG).show()
                        }
                    }
                    return
                } catch (e: SecurityException) {
                }
            }
        }
        if (best != null) {
            shareLoc(best)
        } else {
            Toast.makeText(this, "شغّل الموقع (GPS) وحاول مرة ثانية", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 21 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            sendLocation()
        }
    }

    private fun openWhatsApp(text: String) {
        val url = "https://wa.me/" + Store.waNumber(number) + "?text=" + Uri.encode(text)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.whatsapp"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(this, "واتساب غير موجود", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private fun openSms() {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الرسائل", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun callBack() {
        val uri = Uri.parse("tel:" + Uri.encode(number))
        val granted = checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        try {
            startActivity(Intent(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر الاتصال", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
