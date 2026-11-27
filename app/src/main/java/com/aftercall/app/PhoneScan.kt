package com.aftercall.app

import android.content.Context
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

// استخراج أرقام الهواتف من نص مقروء بالكاميرا:
// - موبايل عراقي بأي صيغة (07XXXXXXXXX، 7XXXXXXXXX، +964…، 00964…، أرقام عربية)
// - أرقام دولية بمفتاح صريح (+ أو 00) بعد التحقق منها بمكتبة libphonenumber
// الموبايل العراقي: 07 + [3-9] + 8 أرقام.
object PhoneScan {
    private val cand = Regex("""(?:\+|00)?\d[\d\s\-().]{6,20}\d""")
    private val iq = Regex("""7[3-9]\d{8}""")
    @Volatile private var util: PhoneNumberUtil? = null

    private fun util(ctx: Context?): PhoneNumberUtil? {
        util?.let { return it }
        if (ctx == null) return null
        return try {
            PhoneNumberUtil.createInstance(ctx.applicationContext).also { util = it }
        } catch (e: Throwable) {
            null
        }
    }

    // أخطاء القراءة الشائعة: O بدل 0، و l/I بدل 1 عندما تكون ملاصقة لأرقام
    private fun fixOcr(s: String): String {
        var t = s
        repeat(3) {
            t = t.replace(Regex("""(?<=[\d+])[Oo](?=[\d\s\-().]|$)|(?<=^|[\s\-().])[Oo](?=\d)"""), "0")
            t = t.replace(Regex("""(?<=\d)[lI|](?=[\d\s\-().]|$)|(?<=^|[\s\-().])[lI|](?=\d)"""), "1")
        }
        return t
    }

    // يحوّل الأرقام العربية/الكردية إلى لاتينية مع إبقاء الفواصل والمسافات
    private fun mapDigits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            val d = Character.digit(ch, 10)
            sb.append(if (d >= 0) ('0' + d) else ch)
        }
        return sb.toString()
    }

    // رقم عراقي من أرقام فقط (بدون + أو 00)، أو null
    private fun iraqi(d0: String, plus: Boolean): String? {
        val d = d0
        if (d.length == 11 && d.startsWith("0") && iq.matches(d.drop(1))) return d
        if (d.length == 10 && !plus && iq.matches(d)) return "0$d"
        if (d.startsWith("964")) {
            val rest = d.drop(3).removePrefix("0")
            if (iq.matches(rest)) return "0$rest"
        }
        return null
    }

    private fun norm(ctx: Context?, raw: String): String? {
        val t = raw.trim()
        val plus = t.startsWith("+") || t.startsWith("00")
        var d = t.filter { it in '0'..'9' }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.length < 8 || d.length > 15) return null
        iraqi(d, plus)?.let { return it }
        // قراءة معكوسة (الأرقام العربية أحيانًا تُقرأ من اليمين): نقبلها فقط إذا صارت رقمًا عراقيًا صحيحًا
        iraqi(d.reversed(), false)?.let { if (!plus) return it }
        // رقم دولي بمفتاح صريح: تحقق بالمكتبة
        if (plus && !d.startsWith("964")) {
            val u = util(ctx)
            if (u != null) {
                try {
                    val p = u.parse("+$d", null)
                    if (u.isValidNumber(p)) return "+$d"
                } catch (e: Exception) {
                }
                return null
            }
            if (d.length >= 9) return "+$d"
        }
        return null
    }

    // كل الأرقام الصحيحة في النص بترتيب ظهورها، بدون تكرار
    fun findAll(ctx: Context?, text: String): List<String> {
        val src = mapDigits(fixOcr(text))
        val out = LinkedHashSet<String>()
        val lines = src.lines() + listOf(src.replace('\n', ' '))
        for (ln in lines) {
            for (m in cand.findAll(ln)) {
                val n = norm(ctx, m.value)
                if (n != null) out.add(n)
            }
        }
        return out.toList()
    }
}
