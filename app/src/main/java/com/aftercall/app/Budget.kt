package com.aftercall.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class Entry(
    val id: Long, val t: Long, val amount: Long,
    val cat: String, val note: String, val income: Boolean
)

class Debt(
    val id: Long, val who: String, val total: Long, val paid: Long,
    val owe: Boolean, val note: String, val t: Long
) {
    val left: Long get() = (total - paid).coerceAtLeast(0L)
}

// بيانات الميزانية محفوظة على الهاتف فقط
object Budget {
    val defaultCats = listOf(
        "⛽ بنزين", "🔧 صيانة ودهن", "🏠 إيجار", "🍽 أكل", "🍬 حلويات وسناكات",
        "☕ شاي وقهوة", "📱 رصيد وإنترنت", "🛒 مشتريات البيت", "💊 صحة", "👨‍👩‍👧 عائلة"
    )

    private fun p(c: Context) = c.getSharedPreferences("budget", Context.MODE_PRIVATE)

    fun salary(c: Context): Long = p(c).getLong("salary", 0L)

    fun setSalary(c: Context, v: Long) {
        p(c).edit().putLong("salary", v).apply()
    }

    fun start(c: Context): Long {
        val s = p(c).getLong("start", 0L)
        if (s != 0L) return s
        val n = System.currentTimeMillis()
        p(c).edit().putLong("start", n).apply()
        return n
    }

    fun entries(c: Context): MutableList<Entry> {
        val out = ArrayList<Entry>()
        try {
            val a = JSONArray(p(c).getString("entries", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(
                    Entry(
                        o.getLong("id"), o.getLong("t"), o.getLong("a"),
                        o.optString("k"), o.optString("n"), o.optBoolean("inc")
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun save(c: Context, list: List<Entry>) {
        val a = JSONArray()
        for (e in list) {
            a.put(
                JSONObject().put("id", e.id).put("t", e.t).put("a", e.amount)
                    .put("k", e.cat).put("n", e.note).put("inc", e.income)
            )
        }
        p(c).edit().putString("entries", a.toString()).apply()
    }

    fun add(c: Context, e: Entry) {
        val l = entries(c)
        l.add(e)
        save(c, l)
    }

    fun replace(c: Context, e: Entry) {
        val l = entries(c)
        val i = l.indexOfFirst { it.id == e.id }
        if (i >= 0) l[i] = e
        save(c, l)
    }

    fun delete(c: Context, id: Long) {
        save(c, entries(c).filter { it.id != id })
    }

    fun customCats(c: Context): List<String> {
        val out = ArrayList<String>()
        try {
            val a = JSONArray(p(c).getString("cats", "[]"))
            for (i in 0 until a.length()) out.add(a.getString(i))
        } catch (e: Exception) {
        }
        return out
    }

    fun addCat(c: Context, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        val all = defaultCats + customCats(c)
        if (all.contains(n)) return
        val a = JSONArray()
        for (x in customCats(c)) a.put(x)
        a.put(n)
        p(c).edit().putString("cats", a.toString()).apply()
    }

    fun spent(l: List<Entry>): Long = l.filter { !it.income }.sumOf { it.amount }
    fun income(l: List<Entry>): Long = l.filter { it.income }.sumOf { it.amount }

    fun remaining(c: Context): Long {
        val l = entries(c)
        return salary(c) + income(l) - spent(l)
    }

    fun byCategory(l: List<Entry>): List<Pair<String, Long>> =
        l.filter { !it.income }.groupBy { it.cat }
            .map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }

    fun fmt(n: Long): String = String.format(Locale.US, "%,d", n) + " د.ع"

    fun parse(s: String): Long {
        val sb = StringBuilder()
        for (ch in s) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '٠'..'٩' -> sb.append(ch - '٠')
                in '۰'..'۹' -> sb.append(ch - '۰')
            }
        }
        return sb.toString().toLongOrNull() ?: 0L
    }

    fun history(c: Context): JSONArray = try {
        JSONArray(p(c).getString("hist", "[]"))
    } catch (e: Exception) {
        JSONArray()
    }

    // يحفظ الدورة الحالية بالسجل ويبدأ دورة جديدة
    fun newCycle(c: Context, newSalary: Long, carry: Boolean) {
        val l = entries(c)
        val rem = salary(c) + income(l) - spent(l)
        val cats = JSONObject()
        for ((k, v) in byCategory(l)) cats.put(k, v)
        val h = history(c)
        h.put(
            JSONObject().put("start", start(c)).put("end", System.currentTimeMillis())
                .put("salary", salary(c)).put("income", income(l)).put("spent", spent(l))
                .put("cats", cats)
        )
        val trimmed = JSONArray()
        val from = maxOf(0, h.length() - 24)
        for (i in from until h.length()) trimmed.put(h.get(i))
        p(c).edit()
            .putString("hist", trimmed.toString())
            .putString("entries", "[]")
            .putLong("start", System.currentTimeMillis())
            .putLong("salary", if (carry) newSalary + rem else newSalary)
            .apply()
    }

    fun debts(c: Context): MutableList<Debt> {
        val out = ArrayList<Debt>()
        try {
            val a = JSONArray(p(c).getString("debts", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(
                    Debt(
                        o.getLong("id"), o.optString("w"), o.getLong("a"), o.optLong("p", 0L),
                        o.optBoolean("owe", true), o.optString("n"), o.optLong("t")
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun saveDebts(c: Context, l: List<Debt>) {
        val a = JSONArray()
        for (d in l) {
            a.put(
                JSONObject().put("id", d.id).put("w", d.who).put("a", d.total).put("p", d.paid)
                    .put("owe", d.owe).put("n", d.note).put("t", d.t)
            )
        }
        p(c).edit().putString("debts", a.toString()).apply()
    }

    fun addDebt(c: Context, d: Debt) {
        val l = debts(c)
        l.add(d)
        saveDebts(c, l)
    }

    fun replaceDebt(c: Context, d: Debt) {
        val l = debts(c)
        val i = l.indexOfFirst { it.id == d.id }
        if (i >= 0) l[i] = d
        saveDebts(c, l)
    }

    fun deleteDebt(c: Context, id: Long) {
        saveDebts(c, debts(c).filter { it.id != id })
    }
}
