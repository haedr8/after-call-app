package com.aftercall.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetActivity : Activity() {

    private lateinit var sv: ScrollView
    private var showAll = false
    private val red = 0xFFC62828.toInt()

    private fun dp(v: Int) = T.dp(this, v)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        sv = ScrollView(this)
        sv.setBackgroundColor(T.BG)
        setContentView(sv)
        refresh()
        if (Budget.salary(this) == 0L) askSalary()
    }

    private fun lp(): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(0, dp(50), 1f)
        p.setMargins(dp(4), dp(4), dp(4), dp(4))
        return p
    }

    private fun row(vararg v: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.layoutDirection = View.LAYOUT_DIRECTION_RTL
        for (x in v) r.addView(x, lp())
        return r
    }

    private fun small(b: android.widget.Button): android.widget.Button {
        b.maxLines = 1
        b.setAutoSizeTextTypeUniformWithConfiguration(10, 15, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        return b
    }

    private fun bar(pct: Int, color: Int): View {
        val track = LinearLayout(this)
        track.orientation = LinearLayout.HORIZONTAL
        track.layoutDirection = View.LAYOUT_DIRECTION_RTL
        track.background = T.shape(this, T.LINE, 6)
        val p = pct.coerceIn(0, 100)
        val fill = View(this)
        fill.background = T.shape(this, color, 6)
        track.addView(fill, LinearLayout.LayoutParams(0, dp(8), p.toFloat().coerceAtLeast(1f)))
        track.addView(View(this), LinearLayout.LayoutParams(0, dp(8), (100 - p).toFloat()))
        return track
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a }
        val y = Calendar.getInstance().apply { timeInMillis = b }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) &&
            x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }

    private fun refresh() {
        val list = Budget.entries(this).sortedByDescending { it.t }
        val salary = Budget.salary(this)
        val extra = Budget.income(list)
        val spent = Budget.spent(list)
        val rem = salary + extra - spent
        val total = salary + extra

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutDirection = View.LAYOUT_DIRECTION_RTL
        col.setPadding(dp(16), dp(16), dp(16), dp(24))
        Ui.fit(col, dp(16), dp(16))

        val title = T.label(this, "💰 مصاريفي", 24f, T.INK, true)
        title.gravity = Gravity.CENTER
        col.addView(title)

        // بطاقة المتبقي
        val ratio = if (total > 0) rem.toDouble() / total else 0.0
        val color = when {
            rem < 0 -> red
            ratio < 0.25 -> T.WARN
            else -> T.OK
        }
        val h = T.card(this)
        h.addView(T.label(this, "المتبقي من راتبك", 14f, T.MUTED))
        val big = T.label(this, Budget.fmt(rem), 34f, color, true)
        big.gravity = Gravity.CENTER
        big.textDirection = View.TEXT_DIRECTION_LTR
        h.addView(big, T.gap(this, 4))
        val sub = T.label(
            this,
            "الراتب " + Budget.fmt(salary) + (if (extra > 0) "  +  دخل إضافي " + Budget.fmt(extra) else "") +
                "\nصرفت " + Budget.fmt(spent),
            13f, T.MUTED
        )
        sub.gravity = Gravity.CENTER
        h.addView(sub, T.gap(this, 4))
        val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        pb.max = 100
        pb.progress = if (total > 0) ((spent * 100) / total).toInt().coerceIn(0, 100) else 0
        pb.progressTintList = ColorStateList.valueOf(color)
        h.addView(pb, T.gap(this, 8))
        if (rem < 0) {
            val w = T.label(this, "⚠️ صرفت أكثر من راتبك بـ " + Budget.fmt(-rem), 14f, red, true)
            w.gravity = Gravity.CENTER
            h.addView(w, T.gap(this, 6))
        }
        col.addView(h, T.gap(this, 12))

        col.addView(
            row(
                small(T.button(this, "💵 دخل إضافي", false) { askAmount("دخل إضافي", "💵 دخل", null, true) }),
                small(T.button(this, "✏️ تغيير الراتب", false) { askSalary() })
            )
        )

        // إضافة مصروف
        col.addView(T.label(this, "➕ سجّل مصروف", 16f, T.INK, true), T.gap(this, 14))
        val cats = Budget.defaultCats + Budget.customCats(this)
        var i = 0
        while (i < cats.size) {
            val a = cats[i]
            val b = if (i + 1 < cats.size) cats[i + 1] else null
            val ba = small(T.tonal(this, a) { askAmount("مصروف", a, null, false) })
            if (b != null) {
                val bb = small(T.tonal(this, b) { askAmount("مصروف", b, null, false) })
                col.addView(row(ba, bb))
            } else {
                col.addView(row(ba, small(T.tonal(this, "➕ تصنيف جديد") { askNewCat() })))
            }
            i += 2
        }
        if (cats.size % 2 == 0) col.addView(row(small(T.tonal(this, "➕ تصنيف جديد") { askNewCat() })))

        // إحصائيات
        val now = System.currentTimeMillis()
        val days = maxOf(1L, (now - Budget.start(this) + 86399999L) / 86400000L)
        val avg = spent / days
        val today = list.filter { !it.income && sameDay(it.t, now) }.sumOf { it.amount }
        val week = list.filter { !it.income && now - it.t < 7L * 86400000L }.sumOf { it.amount }
        val st = T.card(this)
        st.addView(T.label(this, "📊 إحصائيات", 16f, T.INK, true))
        st.addView(T.label(this, "صرفت اليوم: " + Budget.fmt(today), 14f, T.INK), T.gap(this, 6))
        st.addView(T.label(this, "آخر 7 أيام: " + Budget.fmt(week), 14f, T.INK), T.gap(this, 4))
        st.addView(T.label(this, "المعدل اليومي: " + Budget.fmt(avg), 14f, T.INK), T.gap(this, 4))
        if (avg > 0 && rem > 0) {
            st.addView(
                T.label(this, "بهذا المعدل يكفيك تقريباً " + (rem / avg) + " يوم", 14f, T.MUTED),
                T.gap(this, 4)
            )
        }
        col.addView(st, T.gap(this, 14))

        // الديون
        val debts = Budget.debts(this)
        val active = debts.filter { it.left > 0 }
        val oweTotal = active.filter { it.owe }.sumOf { it.left }
        val owedTotal = active.filter { !it.owe }.sumOf { it.left }
        val dc = T.card(this)
        dc.addView(T.label(this, "💳 الديون", 16f, T.INK, true))
        dc.addView(T.label(this, "عليك (مداين): " + Budget.fmt(oweTotal), 14f, if (oweTotal > 0) red else T.INK, true), T.gap(this, 6))
        dc.addView(T.label(this, "لك عند الناس: " + Budget.fmt(owedTotal), 14f, if (owedTotal > 0) T.OK else T.INK, true), T.gap(this, 2))
        for (d in active.sortedByDescending { it.t }) {
            val r = T.card(this)
            r.addView(
                T.label(
                    this,
                    (if (d.owe) "🔴 عليّ لـ " + d.who else "🟢 " + d.who + " عليه لي") +
                        "  —  " + Budget.fmt(d.left),
                    15f, if (d.owe) red else T.OK, true
                )
            )
            val sub2 = "من أصل " + Budget.fmt(d.total) + (if (d.paid > 0) "  •  سُدد " + Budget.fmt(d.paid) else "") +
                (if (d.note.isNotBlank()) "  •  " + d.note else "")
            r.addView(T.label(this, sub2, 12f, T.MUTED))
            r.setOnClickListener { debtMenu(d) }
            dc.addView(r, T.gap(this, 8))
        }
        dc.addView(T.button(this, "➕ دين جديد", false) { askDebt() }, T.gap(this, 10))
        col.addView(dc, T.gap(this, 14))

        // وين راحت فلوسك
        val by = Budget.byCategory(list)
        if (by.isNotEmpty()) {
            val bc = T.card(this)
            bc.addView(T.label(this, "🧾 وين راحت فلوسك", 16f, T.INK, true))
            for ((k, v) in by) {
                val pct = if (spent > 0) ((v * 100) / spent).toInt() else 0
                val l = T.label(this, k + "  —  " + Budget.fmt(v) + "  (" + pct + "%)", 14f, T.INK)
                bc.addView(l, T.gap(this, 10))
                bc.addView(bar(pct, T.TEAL), T.gap(this, 4))
            }
            col.addView(bc, T.gap(this, 14))
        }

        // آخر العمليات
        col.addView(T.label(this, "🕘 العمليات", 16f, T.INK, true), T.gap(this, 16))
        if (list.isEmpty()) {
            col.addView(T.label(this, "ما كو عمليات بعد. اضغط على تصنيف لتسجل مصروف.", 14f, T.MUTED), T.gap(this, 6))
        }
        val fmt = SimpleDateFormat("d/M  HH:mm", Locale.US)
        val shown = if (showAll) list else list.take(25)
        for (e in shown) {
            val box = T.card(this)
            val l1 = T.label(
                this,
                (if (e.income) "+ " else "− ") + Budget.fmt(e.amount) + "   " + e.cat,
                16f, if (e.income) T.OK else red, true
            )
            box.addView(l1)
            val extraTxt = (if (e.note.isNotBlank()) e.note + "  •  " else "") + fmt.format(Date(e.t))
            box.addView(T.label(this, extraTxt, 12f, T.MUTED))
            box.setOnClickListener { entryMenu(e) }
            col.addView(box, T.gap(this, 6))
        }
        if (!showAll && list.size > 25) {
            col.addView(T.button(this, "عرض كل العمليات (" + list.size + ")", false) {
                showAll = true
                refresh()
            }, T.gap(this, 8))
        }

        // أدوات
        col.addView(
            row(
                small(T.button(this, "📤 مشاركة الملخص", false) { share(list, salary, extra, spent, rem, by) }),
                small(T.button(this, "📚 الدورات السابقة", false) { showHistory() })
            ), T.gap(this, 14)
        )
        col.addView(T.button(this, "🔄 راتب جديد (بدء دورة جديدة)", true) { askNewCycle(rem) }, T.gap(this, 6))

        sv.removeAllViews()
        sv.addView(col)
    }

    private fun input(hint: String, number: Boolean, text: String = ""): EditText {
        val et = EditText(this)
        et.hint = hint
        et.layoutDirection = View.LAYOUT_DIRECTION_RTL
        if (number) {
            et.inputType = InputType.TYPE_CLASS_NUMBER
            et.textDirection = View.TEXT_DIRECTION_LTR
        }
        et.setText(text)
        return et
    }

    private fun box(vararg v: View): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(20), dp(8), dp(20), 0)
        for (x in v) l.addView(x)
        return l
    }

    private fun askSalary() {
        val cur = Budget.salary(this)
        val et = input("مثال: 750000", true, if (cur > 0) cur.toString() else "")
        AlertDialog.Builder(this)
            .setTitle("راتبك (أو المبلغ اللي تبدأ فيه)")
            .setView(box(et))
            .setPositiveButton("حفظ") { _, _ ->
                val v = Budget.parse(et.text.toString())
                if (v <= 0) {
                    Toast.makeText(this, "اكتب مبلغ صحيح", Toast.LENGTH_SHORT).show()
                } else {
                    Budget.setSalary(this, v)
                    Budget.start(this)
                    refresh()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun askAmount(title: String, cat: String, old: Entry?, income: Boolean) {
        val a = input("المبلغ", true, if (old != null) old.amount.toString() else "")
        val n = input("ملاحظة (اختياري)", false, old?.note ?: "")
        AlertDialog.Builder(this)
            .setTitle(title + " — " + cat)
            .setView(box(a, n))
            .setPositiveButton("حفظ") { _, _ ->
                val v = Budget.parse(a.text.toString())
                if (v <= 0) {
                    Toast.makeText(this, "اكتب مبلغ صحيح", Toast.LENGTH_SHORT).show()
                } else if (old == null) {
                    Budget.add(
                        this,
                        Entry(System.currentTimeMillis(), System.currentTimeMillis(), v, cat, n.text.toString().trim(), income)
                    )
                    refresh()
                } else {
                    Budget.replace(this, Entry(old.id, old.t, v, old.cat, n.text.toString().trim(), old.income))
                    refresh()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun askNewCat() {
        val et = input("مثال: 🚬 سكائر", false)
        AlertDialog.Builder(this)
            .setTitle("تصنيف جديد")
            .setView(box(et))
            .setPositiveButton("التالي") { _, _ ->
                val name = et.text.toString().trim()
                if (name.isNotEmpty()) {
                    Budget.addCat(this, name)
                    askAmount("مصروف", name, null, false)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun entryMenu(e: Entry) {
        AlertDialog.Builder(this)
            .setTitle(e.cat + " — " + Budget.fmt(e.amount))
            .setItems(arrayOf("✏️ تعديل", "🗑 حذف")) { _, w ->
                if (w == 0) {
                    askAmount(if (e.income) "دخل إضافي" else "مصروف", e.cat, e, e.income)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("حذف العملية؟")
                        .setPositiveButton("حذف") { _, _ ->
                            Budget.delete(this, e.id)
                            refresh()
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun askNewCycle(rem: Long) {
        val et = input("الراتب الجديد", true, Budget.salary(this).toString())
        val cb = CheckBox(this)
        cb.text = (if (rem >= 0) "أضف المتبقي لراتب الدورة الجديدة (" else "اخصم العجز من الدورة الجديدة (") +
            Budget.fmt(Math.abs(rem)) + ")"
        cb.layoutDirection = View.LAYOUT_DIRECTION_RTL
        AlertDialog.Builder(this)
            .setTitle("بدء دورة جديدة")
            .setMessage("تُحفظ الدورة الحالية بسجل الدورات السابقة وتبدأ عمليات جديدة.")
            .setView(box(et, cb))
            .setPositiveButton("ابدأ") { _, _ ->
                val v = Budget.parse(et.text.toString())
                if (v <= 0) {
                    Toast.makeText(this, "اكتب راتب صحيح", Toast.LENGTH_SHORT).show()
                } else {
                    Budget.newCycle(this, v, cb.isChecked)
                    showAll = false
                    refresh()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showHistory() {
        val h = Budget.history(this)
        if (h.length() == 0) {
            Toast.makeText(this, "ما كو دورات سابقة بعد", Toast.LENGTH_SHORT).show()
            return
        }
        val f = SimpleDateFormat("d/M/yyyy", Locale.US)
        val names = ArrayList<String>()
        val bodies = ArrayList<String>()
        for (i in h.length() - 1 downTo 0) {
            val o = h.getJSONObject(i)
            val head = f.format(Date(o.getLong("start"))) + " ← " + f.format(Date(o.getLong("end")))
            val sal = o.getLong("salary")
            val inc = o.getLong("income")
            val sp = o.getLong("spent")
            names.add(head + "\nصرفت " + Budget.fmt(sp))
            val sb = StringBuilder()
            sb.append("الراتب: ").append(Budget.fmt(sal)).append("\n")
            if (inc > 0) sb.append("دخل إضافي: ").append(Budget.fmt(inc)).append("\n")
            sb.append("المصروف: ").append(Budget.fmt(sp)).append("\n")
            sb.append("المتبقي: ").append(Budget.fmt(sal + inc - sp)).append("\n\n")
            val cats = o.optJSONObject("cats")
            if (cats != null) {
                val items = ArrayList<Pair<String, Long>>()
                val it = cats.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    items.add(k to cats.getLong(k))
                }
                for ((k, v) in items.sortedByDescending { it.second }) {
                    sb.append(k).append(": ").append(Budget.fmt(v)).append("\n")
                }
            }
            bodies.add(sb.toString())
        }
        AlertDialog.Builder(this)
            .setTitle("الدورات السابقة")
            .setItems(names.toTypedArray()) { _, w ->
                AlertDialog.Builder(this).setMessage(bodies[w]).setPositiveButton("تمام", null).show()
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun askDebt() {
        val rg = android.widget.RadioGroup(this)
        rg.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val r1 = android.widget.RadioButton(this)
        r1.text = "اقترضت من شخص (أنا مداين)"
        r1.id = 1001
        val r2 = android.widget.RadioButton(this)
        r2.text = "أقرضت شخص (مداينه)"
        r2.id = 1002
        rg.addView(r1)
        rg.addView(r2)
        rg.check(1001)
        val who = input("اسم الشخص", false)
        val amt = input("المبلغ", true)
        val note = input("ملاحظة (اختياري)", false)
        val cb = CheckBox(this)
        cb.text = "سجّل المبلغ بميزانيتي (يُضاف لرصيدي إذا اقترضت، ويُخصم إذا أقرضت)"
        cb.layoutDirection = View.LAYOUT_DIRECTION_RTL
        cb.isChecked = true
        AlertDialog.Builder(this)
            .setTitle("دين جديد")
            .setView(ScrollView(this).also { it.addView(box(rg, who, amt, note, cb)) })
            .setPositiveButton("حفظ") { _, _ ->
                val v = Budget.parse(amt.text.toString())
                val name = who.text.toString().trim()
                if (v <= 0 || name.isEmpty()) {
                    Toast.makeText(this, "اكتب الاسم والمبلغ", Toast.LENGTH_SHORT).show()
                } else {
                    val owe = rg.checkedRadioButtonId == 1001
                    val now = System.currentTimeMillis()
                    Budget.addDebt(this, Debt(now, name, v, 0L, owe, note.text.toString().trim(), now))
                    if (cb.isChecked) {
                        Budget.add(
                            this,
                            Entry(now + 1, now, v, if (owe) "💳 دين من " + name else "💳 سلفة لـ " + name, "", owe)
                        )
                    }
                    refresh()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun debtMenu(d: Debt) {
        AlertDialog.Builder(this)
            .setTitle(d.who + " — " + Budget.fmt(d.left))
            .setItems(arrayOf(if (d.owe) "💸 سددت دفعة" else "💵 استلمت دفعة", "✅ تم السداد كاملاً", "🗑 حذف")) { _, w ->
                when (w) {
                    0 -> askPayment(d, false)
                    1 -> askPayment(d, true)
                    else -> AlertDialog.Builder(this)
                        .setTitle("حذف هذا الدين؟")
                        .setPositiveButton("حذف") { _, _ ->
                            Budget.deleteDebt(this, d.id)
                            refresh()
                        }
                        .setNegativeButton("إلغاء", null)
                        .show()
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun askPayment(d: Debt, full: Boolean) {
        val amt = input("المبلغ", true, if (full) d.left.toString() else "")
        val cb = CheckBox(this)
        cb.text = if (d.owe) "سجّلها بمصاريفي (تُخصم من رصيدي)" else "سجّلها بميزانيتي (تُضاف لرصيدي)"
        cb.layoutDirection = View.LAYOUT_DIRECTION_RTL
        cb.isChecked = true
        AlertDialog.Builder(this)
            .setTitle(if (d.owe) "سداد لـ " + d.who else "استلام من " + d.who)
            .setView(box(amt, cb))
            .setPositiveButton("حفظ") { _, _ ->
                val v = Budget.parse(amt.text.toString()).coerceAtMost(d.left)
                if (v <= 0) {
                    Toast.makeText(this, "اكتب مبلغ صحيح", Toast.LENGTH_SHORT).show()
                } else {
                    Budget.replaceDebt(this, Debt(d.id, d.who, d.total, d.paid + v, d.owe, d.note, d.t))
                    if (cb.isChecked) {
                        val now = System.currentTimeMillis()
                        Budget.add(
                            this,
                            Entry(now, now, v, if (d.owe) "💳 سداد دين لـ " + d.who else "💳 تسديد من " + d.who, "", !d.owe)
                        )
                    }
                    refresh()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun share(
        list: List<Entry>, salary: Long, extra: Long, spent: Long, rem: Long, by: List<Pair<String, Long>>
    ) {
        val sb = StringBuilder()
        sb.append("💰 ملخص مصاريفي\n")
        sb.append("الراتب: ").append(Budget.fmt(salary)).append("\n")
        if (extra > 0) sb.append("دخل إضافي: ").append(Budget.fmt(extra)).append("\n")
        sb.append("المصروف: ").append(Budget.fmt(spent)).append("\n")
        sb.append("المتبقي: ").append(Budget.fmt(rem)).append("\n\n")
        for ((k, v) in by) sb.append(k).append(": ").append(Budget.fmt(v)).append("\n")
        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, sb.toString()),
                    "مشاركة الملخص"
                )
            )
        } catch (e: Exception) {
        }
    }
}
