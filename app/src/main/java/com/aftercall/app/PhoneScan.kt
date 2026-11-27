package com.aftercall.app

// استخراج رقم هاتف من نص مقروء بالكاميرا (عراقي أو دولي)
object PhoneScan {
    private val cand = Regex("""(?:\+|00)?\d[\d\s\-().]{6,20}\d""")

    private fun norm(raw: String): String? {
        val t = Store.ascii(raw).trim()
        val plus = t.startsWith("+") || t.startsWith("00")
        var d = t.filter { it in '0'..'9' }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.length < 8 || d.length > 15) return null
        // عراقي: 07XXXXXXXXX
        if (d.length == 11 && d.startsWith("07")) return d
        // 7XXXXXXXXX بدون صفر
        if (d.length == 10 && d.startsWith("7") && !plus) return "0$d"
        // 9647XXXXXXXXX أو 96407XXXXXXXXX
        if (d.startsWith("9640") && d.length == 14) return "+964" + d.drop(4)
        if (d.startsWith("964") && d.length == 13) return "+$d"
        // رقم دولي آخر يبدأ بـ + أو 00
        if (plus && d.length >= 9) return "+$d"
        return null
    }

    fun find(text: String): String? {
        val lines = text.lines() + listOf(text.replace('\n', ' '))
        for (ln in lines) {
            for (m in cand.findAll(Store.ascii(ln))) {
                val n = norm(m.value)
                if (n != null) return n
            }
        }
        return null
    }
}
