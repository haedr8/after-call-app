package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CallDialogActivity : Activity() {

    private class Opt(val label: String, val action: () -> Unit)

    private var number = ""
    private var name = ""

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
