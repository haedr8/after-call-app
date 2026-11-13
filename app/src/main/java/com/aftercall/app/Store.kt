package com.aftercall.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import org.json.JSONArray
import org.json.JSONObject

class Tpl(val label: String, val text: String)

object Ui {
    fun fit(v: View, side: Int, vert: Int) {
        v.setOnApplyWindowInsetsListener { view, ins ->
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
            view.setPadding(side, top + vert, side, bottom + vert)
            ins
        }
    }
}

object Store {
    const val WA = "com.whatsapp"
    const val WA_B = "com.whatsapp.w4b"

    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private fun notes(c: Context) = c.getSharedPreferences("notes", Context.MODE_PRIVATE)

    fun defaults(): List<Tpl> = listOf(
        Tpl("📍 طلب لوكيشن", "سلام عليكم، دز لي لوكيشن (موقع)"),
        Tpl("🚪 باركود / نداء", "سلام عليكم، ممكن تدز لي باركود أو تسوي لي نداء حتى أدخل للمجمع"),
        Tpl("💬 واتساب", "السلام عليكم"),
        Tpl("✅ وصلت", "سلام عليكم، وصلت عند الباب"),
        Tpl("🛵 بالطريق", "سلام عليكم، أنا بالطريق إلك")
    )

    fun templates(c: Context): List<Tpl> {
        val raw = sp(c).getString("tpl", null) ?: return defaults()
        return try {
            val a = JSONArray(raw)
            val l = ArrayList<Tpl>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                l.add(Tpl(o.getString("l"), o.getString("t")))
            }
            if (l.isEmpty()) defaults() else l
        } catch (e: Exception) {
            defaults()
        }
    }

    fun saveTemplates(c: Context, l: List<Tpl>) {
        val a = JSONArray()
        for (t in l) a.put(JSONObject().put("l", t.label).put("t", t.text))
        sp(c).edit().putString("tpl", a.toString()).apply()
    }

    fun resetTemplates(c: Context) {
        sp(c).edit().remove("tpl").apply()
    }

    fun getBool(c: Context, k: String, d: Boolean): Boolean = sp(c).getBoolean(k, d)

    fun setBool(c: Context, k: String, v: Boolean) {
        sp(c).edit().putBoolean(k, v).apply()
    }

    fun allows(c: Context, type: Int): Boolean = when (type) {
        1 -> getBool(c, "in", true)
        2 -> getBool(c, "out", true)
        3, 5 -> getBool(c, "missed", true)
        else -> true
    }

    // ---- اختيار واتساب (العادي / للأعمال) ----
    fun waPref(c: Context): String = sp(c).getString("wa", "auto") ?: "auto"

    fun setWaPref(c: Context, v: String) {
        sp(c).edit().putString("wa", v).apply()
    }

    fun isInstalled(c: Context, pkg: String): Boolean {
        return try {
            c.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun waPackage(c: Context): String? = when (waPref(c)) {
        "regular" -> WA
        "business" -> WA_B
        else -> if (isInstalled(c, WA)) WA else if (isInstalled(c, WA_B)) WA_B else null
    }

    fun waIntent(number: String, text: String, pkg: String?): Intent {
        val i = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/" + waNumber(number) + "?text=" + Uri.encode(text))
        )
        if (pkg != null) i.setPackage(pkg)
        return i
    }

    // ---- ملاحظات الأرقام ----
    private fun key(number: String): String {
        val d = number.filter { it.isDigit() }
        return if (d.length > 9) d.takeLast(9) else d
    }

    fun getNote(c: Context, number: String): String {
        val k = key(number)
        if (k.isEmpty()) return ""
        return notes(c).getString(k, "") ?: ""
    }

    fun setNote(c: Context, number: String, text: String) {
        val k = key(number)
        if (k.isEmpty()) return
        val ed = notes(c).edit()
        if (text.isBlank()) ed.remove(k) else ed.putString(k, text.trim())
        ed.apply()
    }

    fun waNumber(number: String): String {
        val raw = number.trim()
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> "964" + digits.drop(1)
            else -> digits
        }
    }

    private val boolKeys = listOf("in", "out", "missed", "service", "bubble")

    fun backup(c: Context): String {
        val o = JSONObject()
        val a = JSONArray()
        for (t in templates(c)) a.put(JSONObject().put("l", t.label).put("t", t.text))
        o.put("templates", a)
        val n = JSONObject()
        for ((k, v) in notes(c).all) n.put(k, v.toString())
        o.put("notes", n)
        for (k in boolKeys) o.put(k, getBool(c, k, k != "bubble"))
        o.put("wa", waPref(c))
        return "حولني-نسخة:" + o.toString()
    }

    fun restore(c: Context, text: String): Boolean {
        return try {
            val o = JSONObject(text.trim().removePrefix("حولني-نسخة:"))
            val a = o.getJSONArray("templates")
            val l = ArrayList<Tpl>()
            for (i in 0 until a.length()) {
                val t = a.getJSONObject(i)
                l.add(Tpl(t.getString("l"), t.getString("t")))
            }
            if (l.isNotEmpty()) saveTemplates(c, l)
            val n = o.getJSONObject("notes")
            val ed = notes(c).edit()
            val ks = n.keys()
            while (ks.hasNext()) {
                val k = ks.next()
                ed.putString(k, n.getString(k))
            }
            ed.apply()
            for (k in boolKeys) {
                if (o.has(k)) setBool(c, k, o.getBoolean(k))
            }
            if (o.has("wa")) setWaPref(c, o.getString("wa"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
