package com.aftercall.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

// طلب الصلاحيات بشكل موحّد: إذا النظام ما عاد يعرض نافذة السماح، نفتح صفحة الصلاحيات مباشرة
object Grant {
    private fun prefs(c: Context) = c.getSharedPreferences("grant", Context.MODE_PRIVATE)

    fun missing(a: Activity, perms: Array<String>): List<String> =
        perms.filter { a.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

    fun request(a: Activity, perms: Array<String>, code: Int = 1) {
        val miss = missing(a, perms)
        if (miss.isEmpty()) return
        val p = prefs(a)
        val blocked = miss.any { p.getBoolean(it, false) && !a.shouldShowRequestPermissionRationale(it) }
        if (blocked) {
            Toast.makeText(a, "فعّل «الأذونات» ثم اسمح بكل الصلاحيات", Toast.LENGTH_LONG).show()
            appSettings(a)
            return
        }
        val e = p.edit()
        for (x in miss) e.putBoolean(x, true)
        e.apply()
        try {
            a.requestPermissions(miss.toTypedArray(), code)
        } catch (ex: Exception) {
            appSettings(a)
        }
    }

    // بعد رجوع النتيجة: إذا ما زال شي ناقص والنظام يمنع النافذة، افتح الإعدادات
    fun afterResult(a: Activity, perms: Array<String>) {
        val miss = missing(a, perms)
        if (miss.isNotEmpty() && miss.any { !a.shouldShowRequestPermissionRationale(it) }) {
            Toast.makeText(a, "اسمح بالصلاحيات من هذه الصفحة: الأذونات", Toast.LENGTH_LONG).show()
            appSettings(a)
        }
    }

    fun openFirst(a: Activity, vararg intents: Intent): Boolean {
        for (i in intents) {
            try {
                a.startActivity(i)
                return true
            } catch (e: Exception) {
            }
        }
        return false
    }

    fun appSettings(a: Activity): Boolean = openFirst(
        a,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName)),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun fullScreen(a: Activity): Boolean {
        val list = ArrayList<Intent>()
        if (Build.VERSION.SDK_INT >= 34) {
            list.add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + a.packageName)))
        }
        list.add(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, a.packageName)
        )
        list.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName)))
        list.add(Intent(Settings.ACTION_SETTINGS))
        return openFirst(a, *list.toTypedArray())
    }
}
