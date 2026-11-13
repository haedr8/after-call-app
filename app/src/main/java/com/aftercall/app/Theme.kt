package com.aftercall.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object T {
    val TEAL = 0xFF0B7A86.toInt()
    val GREEN = 0xFF1CC47A.toInt()
    val BG = 0xFFF2F7F7.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val INK = 0xFF12303A.toInt()
    val MUTED = 0xFF5B7075.toInt()
    val OK = 0xFF0F7A4B.toInt()
    val WARN = 0xFFB45309.toInt()
    val LINE = 0xFFD5E3E5.toInt()
    val TONAL = 0xFFE3F4F3.toInt()

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun shape(c: Context, fill: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = dp(c, radius).toFloat()
        if (stroke != 0) g.setStroke(dp(c, 1), stroke)
        return g
    }

    fun ripple(c: Context, fill: Int, radius: Int, stroke: Int = 0): RippleDrawable {
        val mask = GradientDrawable()
        mask.setColor(0xFF000000.toInt())
        mask.cornerRadius = dp(c, radius).toFloat()
        return RippleDrawable(
            ColorStateList.valueOf(0x30000000),
            shape(c, fill, radius, stroke),
            mask
        )
    }

    fun gap(c: Context, top: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = dp(c, top)
        return p
    }

    fun card(c: Context): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.layoutDirection = View.LAYOUT_DIRECTION_RTL
        l.background = shape(c, WHITE, 20, LINE)
        l.elevation = dp(c, 2).toFloat()
        val p = dp(c, 16)
        l.setPadding(p, p, p, p)
        return l
    }

    fun label(c: Context, t: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(c)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.typeface = Typeface.DEFAULT_BOLD
        return v
    }

    fun button(c: Context, t: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(c)
        b.text = t
        b.isAllCaps = false
        b.textSize = 15f
        b.stateListAnimator = null
        if (filled) {
            b.setTextColor(WHITE)
            b.background = ripple(c, TEAL, 14)
        } else {
            b.setTextColor(TEAL)
            b.background = ripple(c, WHITE, 14, TEAL)
        }
        b.minHeight = dp(c, 48)
        b.setOnClickListener { onClick() }
        return b
    }

    fun tonal(c: Context, t: String, onClick: () -> Unit): Button {
        val b = Button(c)
        b.text = t
        b.isAllCaps = false
        b.maxLines = 1
        b.setTextColor(TEAL)
        b.stateListAnimator = null
        b.background = ripple(c, TONAL, 14)
        b.setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP)
        b.setOnClickListener { onClick() }
        return b
    }

    fun tile(c: Context, emoji: String, t: String, onClick: () -> Unit): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.gravity = Gravity.CENTER
        l.background = ripple(c, WHITE, 18, LINE)
        l.elevation = dp(c, 1).toFloat()
        l.setPadding(dp(c, 10), dp(c, 16), dp(c, 10), dp(c, 14))
        l.isClickable = true
        val e = label(c, emoji, 28f, INK)
        e.gravity = Gravity.CENTER
        l.addView(e)
        val tv = label(c, t, 13f, INK, true)
        tv.gravity = Gravity.CENTER
        tv.setPadding(0, dp(c, 6), 0, 0)
        l.addView(tv)
        l.setOnClickListener { onClick() }
        return l
    }

    fun chip(c: Context, t: String, icon: Int, onClick: () -> Unit): TextView {
        val v = TextView(c)
        v.text = t
        v.textSize = 14f
        v.setTextColor(INK)
        v.gravity = Gravity.CENTER
        v.background = ripple(c, WHITE, 24, LINE)
        v.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10))
        v.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        v.compoundDrawablePadding = dp(c, 8)
        v.isClickable = true
        v.setOnClickListener { onClick() }
        return v
    }
}
