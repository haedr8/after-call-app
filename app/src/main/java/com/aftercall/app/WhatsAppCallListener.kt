package com.aftercall.app

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppCallListener : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())

    private fun prefs(): SharedPreferences =
        getSharedPreferences("aftercall", Context.MODE_PRIVATE)

    private fun isWaCall(s: StatusBarNotification): Boolean =
        s.packageName == "com.whatsapp" && s.notification.category == Notification.CATEGORY_CALL

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isWaCall(sbn)) return
        val title = sbn.notification.extras
            ?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val e = prefs().edit().putBoolean("wa_active", true)
        if (title.isNotBlank()) e.putString("wa_title", title)
        e.apply()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!isWaCall(sbn)) return
        handler.postDelayed({ checkEnded() }, 1200)
    }

    private fun checkEnded() {
        val still = try {
            activeNotifications?.any { isWaCall(it) } ?: false
        } catch (e: Exception) {
            false
        }
        if (still) return
        val p = prefs()
        if (!p.getBoolean("wa_active", false)) return
        p.edit().putBoolean("wa_active", false).apply()

        val title = (p.getString("wa_title", "") ?: "").trim()
        val digits = title.filter { it.isDigit() }
        val number: String
        val name: String
        if (digits.length >= 7) {
            number = (if (title.startsWith("+")) "+" else "") + digits
            name = ""
        } else {
            name = title
            number = lookupNumber(title)
        }
        Notifier.show(applicationContext, 2, "انتهت مكالمة واتساب", number, name)
    }

    private fun lookupNumber(name: String): String {
        if (name.isBlank()) return ""
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return ""
        return try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + "=?",
                arrayOf(name),
                null
            )?.use { c -> if (c.moveToFirst()) (c.getString(0) ?: "") else "" } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
