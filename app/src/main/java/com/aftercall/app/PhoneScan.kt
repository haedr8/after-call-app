package com.aftercall.app

// استخراج رقم هاتف من نص مقروء بالكاميرا (موبايل عراقي بأي صيغة، أو رقم دولي بمفتاح)
// الموبايل العراقي: 07 + [3-9] + 8 أرقام = 11 رقم محلياً، أو +964 7XX XXX XXXX دولياً.
object PhoneScan {
    private val cand = Regex("""(?:\+|00)?\d[\d\s\-().]{6,20}\d""")
    private val iq = Regex("""7[3-9]\d{8}""")

    // أخطاء القراءة الشائعة: O بدل 0، و l/I بدل 1 عندما تكون ملاصقة لأرقام
    private fun fixOcr(s: String): String {
        var t = s
        repeat(3) {
            t = t.replace(Regex("""(?<=[\d+])[Oo](?=[\d\s\-().]|$)|(?<=^|[\s\-().])[Oo](?=\d)"""), "0")
            t = t.replace(Regex("""(?<=\d)[lI|](?=[\d\s\-().]|$)|(?<=^|[\s\-().])[lI|](?=\d)"""), "1")
        }
        return t
    }

    private fun norm(raw: String): String? {
        val t = raw.trim()
        val plus = t.startsWith("+") || t.startsWith("00")
        var d = t.filter { it in '0'..'9' }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.length < 8 || d.length > 15) return null
        // محلي: 07XXXXXXXXX
        if (d.length == 11 && d.startsWith("0") && iq.matches(d.drop(1))) return d
        // بدون الصفر: 7XXXXXXXXX
        if (d.length == 10 && !plus && iq.matches(d)) return "0$d"
        // دولي عراقي: 9647XXXXXXXXX أو 96407XXXXXXXXX (بعضهم يكتب الصفر بعد المفتاح)
        if (d.startsWith("964")) {
            val rest = d.drop(3).removePrefix("0")
            if (iq.matches(rest)) return "0$rest"
            if (plus) return null
        }
        // رقم دولي آخر بمفتاح صريح
        if (plus && d.length >= 9 && !d.startsWith("964")) return "+$d"
        return null
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

    fun find(text: String): String? {
        val src = mapDigits(fixOcr(text))
        val lines = src.lines() + listOf(src.replace('\n', ' '))
        for (ln in lines) {
            for (m in cand.findAll(ln)) {
                val n = norm(m.value)
                if (n != null) return n
            }
        }
        return null
    }
}
