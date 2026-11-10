package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

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

        status = TextView(this)
        status.textSize = 20f
        status.setTextColor(0xFF000000.toInt())

        val btn = Button(this)
        btn.text = "منح الصلاحيات"
        btn.setOnClickListener { requestPermissions(needed(), 1) }

        root.addView(status)
        root.addView(btn)
        setContentView(root)
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
