package com.aftercall.app

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

object Bubble {
    private var view: View? = null
    private val h = Handler(Looper.getMainLooper())

    fun show(ctx0: Context, number: String, name: String) {
        val ctx = ctx0.applicationContext
        if (!Store.getBool(ctx, "bubble", false)) return
        if (!Settings.canDrawOverlays(ctx)) return
        h.post {
            try {
                if (view == null) add(ctx, number, name)
            } catch (e: Exception) {
                Diag.log(ctx, "bubble error: " + e.message)
            }
        }
    }

    private fun hide(ctx: Context) {
        try {
            val v = view
            if (v != null) {
                (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
            }
        } catch (e: Exception) {
        }
        view = null
    }

    private fun add(ctx: Context, number: String, name: String) {
        val d = ctx.resources.displayMetrics.density
        val tv = TextView(ctx)
        tv.text = "📞 انتهت المكالمة" +
            (if (name.isNotBlank()) "\n" + name else "") +
            "\nاضغط لعرض الخيارات"
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.textSize = 15f
        tv.gravity = Gravity.CENTER
        val pad = (14 * d).toInt()
        tv.setPadding(pad, pad, pad, pad)
        val bg = GradientDrawable()
        bg.setColor(0xEE0B7A86.toInt())
        bg.cornerRadius = 22 * d
        tv.background = bg
        tv.setOnClickListener {
            hide(ctx)
            val i = Intent(ctx, CallDialogActivity::class.java)
            i.putExtra("number", number)
            i.putExtra("name", name)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ctx.startActivity(i)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = (90 * d).toInt()
        (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(tv, lp)
        view = tv
        h.removeCallbacksAndMessages(null)
        h.postDelayed({ hide(ctx) }, 20000)
    }
}
