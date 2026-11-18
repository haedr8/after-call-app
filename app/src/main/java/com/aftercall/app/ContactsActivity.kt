package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class ContactsActivity : Activity() {

    private class C(val name: String, val number: String)

    private val all = ArrayList<C>()
    private var shown: List<C> = emptyList()
    private lateinit var list: ListView
    private lateinit var info: TextView

    private fun dp(v: Int) = T.dp(this, v)

    private val adapter = object : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(i: Int) = shown[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, v: View?, p: ViewGroup?): View {
            val row: LinearLayout
            if (v is LinearLayout) {
                row = v
            } else {
                row = LinearLayout(this@ContactsActivity)
                row.orientation = LinearLayout.VERTICAL
                row.layoutDirection = View.LAYOUT_DIRECTION_RTL
                row.setPadding(dp(14), dp(10), dp(14), dp(10))
                row.addView(T.label(this@ContactsActivity, "", 16f, T.INK, true))
                val n = T.label(this@ContactsActivity, "", 13f, T.MUTED)
                n.textDirection = View.TEXT_DIRECTION_LTR
                n.gravity = Gravity.START
                row.addView(n)
            }
            val c = shown[i]
            (row.getChildAt(0) as TextView).text = c.name
            (row.getChildAt(1) as TextView).text = c.number
            return row
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(T.BG)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setPadding(dp(12), dp(12), dp(12), dp(12))
        Ui.fit(root, dp(12), dp(12))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val title = T.label(this, "👤 جهات الاتصال", 22f, T.INK, true)
        top.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(T.tonal(this, "➕ جديد") {
            try {
                startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))
            } catch (e: Exception) {
            }
        }, LinearLayout.LayoutParams(dp(110), dp(44)))
        root.addView(top)

        val et = EditText(this)
        et.hint = "ابحث بالاسم أو الرقم"
        et.layoutDirection = View.LAYOUT_DIRECTION_RTL
        et.setSingleLine()
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        root.addView(et, T.gap(this, 8))

        info = T.label(this, "", 14f, T.MUTED)
        info.gravity = Gravity.CENTER
        root.addView(info, T.gap(this, 8))

        list = ListView(this)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, i, _ ->
            val c = shown[i]
            startActivity(
                Intent(this, CallDialogActivity::class.java)
                    .putExtra("number", c.number).putExtra("name", c.name)
            )
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        load()
    }

    private fun load() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            info.text = "اسمح بقراءة جهات الاتصال لعرضها هنا"
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 51)
            return
        }
        all.clear()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                val seen = HashSet<String>()
                while (c.moveToNext()) {
                    val n = c.getString(1) ?: continue
                    val key = (c.getString(0) ?: "") + n.filter { it.isDigit() }
                    if (seen.add(key)) all.add(C(c.getString(0) ?: n, n))
                }
            }
        } catch (e: Exception) {
        }
        info.text = if (all.isEmpty()) "لا توجد جهات اتصال" else ""
        filter("")
    }

    private fun filter(q: String) {
        val t = q.trim().lowercase()
        shown = if (t.isEmpty()) all.toList() else {
            val d = t.filter { it.isDigit() }
            all.filter {
                it.name.lowercase().contains(t) || (d.isNotEmpty() && it.number.filter { x -> x.isDigit() }.contains(d))
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        if (rc == 51) load()
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && all.isEmpty()) load()
    }
}
