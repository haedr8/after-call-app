package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import android.widget.TextView

class ContactsActivity : Activity() {

    private class C(val name: String, val number: String, val norm: String, val id: Long, val lookup: String)
    private var pendingDelete: C? = null

    private val all = ArrayList<C>()
    private var shown: List<C> = emptyList()
    private lateinit var list: ListView
    private lateinit var info: TextView
    private lateinit var grantBtn: View
    private var asked = false

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

        grantBtn = T.tonal(this, "السماح بقراءة جهات الاتصال") {
            Grant.request(this, arrayOf(Manifest.permission.READ_CONTACTS), 51)
        }
        grantBtn.visibility = View.GONE
        root.addView(grantBtn, T.gap(this, 8))

        list = ListView(this)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, i, _ ->
            if (i < 0 || i >= shown.size) return@setOnItemClickListener
            val c = shown[i]
            startActivity(
                Intent(this, CallDialogActivity::class.java)
                    .putExtra("number", if (c.norm.isNotBlank()) c.norm else c.number)
                    .putExtra("name", c.name)
            )
        }
        list.setOnItemLongClickListener { _, _, i, _ ->
            if (i >= 0 && i < shown.size) menu(shown[i])
            true
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        load()
    }

    private fun load() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            info.text = "اسمح بقراءة جهات الاتصال لعرضها هنا"
            grantBtn.visibility = View.VISIBLE
            // نطلب الإذن مرة واحدة فقط تلقائياً، وبعدها بزر يضغطه المستخدم (لا نعيد النافذة بحلقة)
            if (!asked) {
                asked = true
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 51)
            }
            return
        }
        grantBtn.visibility = View.GONE
        info.text = ""
        all.clear()
        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
                ),
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
            )?.use { c ->
                val seen = HashSet<String>()
                while (c.moveToNext()) {
                    val n = c.getString(1) ?: continue
                    val key = (c.getString(0) ?: "") + n.filter { it.isDigit() }
                    if (seen.add(key)) all.add(C(c.getString(0) ?: n, n, c.getString(2) ?: "", c.getLong(3), c.getString(4) ?: ""))
                }
            }
        } catch (e: Exception) {
        }
        info.text = if (all.isEmpty()) "لا توجد جهات اتصال" else ""
        filter("")
    }

    private fun menu(c: C) {
        AlertDialog.Builder(this)
            .setTitle(c.name)
            .setItems(arrayOf("✏️ تعديل جهة الاتصال", "🗑 حذف جهة الاتصال", "📋 نسخ الرقم")) { _, w ->
                when (w) {
                    0 -> try {
                        val u = ContactsContract.Contacts.getLookupUri(c.id, c.lookup)
                        startActivity(
                            Intent(Intent.ACTION_EDIT).setDataAndType(u, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                        )
                    } catch (e: Exception) {
                        Toast.makeText(this, "تعذر فتح التعديل", Toast.LENGTH_SHORT).show()
                    }
                    1 -> askDelete(c)
                    else -> {
                        val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("number", c.number))
                        Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun askDelete(c: C) {
        AlertDialog.Builder(this)
            .setTitle("حذف «" + c.name + "»؟")
            .setMessage("تُحذف جهة الاتصال من هاتفك بكل أرقامها، ولا يمكن التراجع.")
            .setPositiveButton("حذف") { _, _ -> doDelete(c) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun doDelete(c: C) {
        if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            pendingDelete = c
            Grant.request(this, arrayOf(Manifest.permission.WRITE_CONTACTS), 52)
            return
        }
        try {
            val n = contentResolver.delete(ContactsContract.Contacts.getLookupUri(c.id, c.lookup), null, null)
            Toast.makeText(this, if (n > 0) "تم الحذف" else "تعذر الحذف", Toast.LENGTH_SHORT).show()
            if (n > 0) {
                all.removeAll { it.id == c.id }
                filter(lastQ)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر الحذف", Toast.LENGTH_SHORT).show()
        }
    }

    private var lastQ = ""
    private fun filter(q: String) {
        lastQ = q
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
        if (rc == 51) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) load()
            else {
                info.text = "اسمح بقراءة جهات الاتصال لعرضها هنا"
                grantBtn.visibility = View.VISIBLE
                Grant.afterResult(this, arrayOf(Manifest.permission.READ_CONTACTS))
            }
        }
        if (rc == 52) {
            val d = pendingDelete
            pendingDelete = null
            if (d != null && g.isNotEmpty() && g[0] == PackageManager.PERMISSION_GRANTED) doDelete(d)
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && all.isEmpty()) load()
    }
}
