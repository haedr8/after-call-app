package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var waStatus: TextView

    private fun needed(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 300, 48, 48)

        val logoBox = FrameLayout(this)
        val boxBg = GradientDrawable()
        boxBg.cornerRadius = 70f
        boxBg.setColor(0xFF12997F.toInt())
        logoBox.background = boxBg
        logoBox.outlineProvider = ViewOutlineProvider.BACKGROUND
        logoBox.clipToOutline = true
        logoBox.layoutParams = LinearLayout.LayoutParams(300, 300)

        val logo = ImageView(this)
        logo.setImageResource(R.mipmap.ic_launcher)
        logo.scaleType = ImageView.ScaleType.CENTER_CROP
        logo.layoutParams = FrameLayout.LayoutParams(400, 400, Gravity.CENTER)
        logoBox.addView(logo)

        status = TextView(this)
        status.textSize = 20f
        status.setTextColor(0xFF000000.toInt())
        status.setPadding(0, 32, 0, 0)

        val btn = Button(this)
        btn.text = "منح الصلاحيات"
        btn.setOnClickListener { requestPermissions(needed(), 1) }

        waStatus = TextView(this)
        waStatus.textSize = 20f
        waStatus.setTextColor(0xFF000000.toInt())
        waStatus.setPadding(0, 48, 0, 0)

        val btn2 = Button(this)
        btn2.text = "تفعيل مراقبة واتساب"
        btn2.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val credit = TextView(this)
        credit.text = "تصميم وتطوير: حيدر محمد"
        credit.textSize = 16f
        credit.setTextColor(0xFF000000.toInt())
        credit.setPadding(0, 96, 0, 0)

        val btn3 = Button(this)
        btn3.text = "📷 haedr.m1"
        btn3.setOnClickListener { openInstagram() }

        root.addView(logoBox)
        root.addView(status)
        root.addView(btn)
        root.addView(waStatus)
        root.addView(btn2)
        root.addView(credit)
        root.addView(btn3)
        setContentView(root)
    }

    private fun openInstagram() {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/haedr.m1"))
        app.setPackage("com.instagram.android")
        try {
            startActivity(app)
        } catch (e: Exception) {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/haedr.m1"))
            )
        }
    }

    private fun refresh() {
        val missing = needed().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        status.text = if (missing.isEmpty()) {
            "✅ كل الصلاحيات ممنوحة"
        } else {
            "⚠️ صلاحيات ناقصة: " + missing.size
        }

        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        waStatus.text = if (enabled.contains(packageName)) {
            "✅ مراقبة واتساب مفعّلة"
        } else {
            "⚠️ مراقبة واتساب غير مفعّلة"
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }
}
