package com.aftercall.app

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.widget.Toast

object Phone {
    val perms: Array<String>
        get() {
            val l = mutableListOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
            if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
            return l.toTypedArray()
        }

    fun isDefault(c: Context): Boolean = try {
        (c.getSystemService(Context.TELECOM_SERVICE) as TelecomManager)
            .defaultDialerPackage == c.packageName
    } catch (e: Exception) {
        false
    }

    fun roleIntent(c: Context): Intent {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = c.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, c.packageName)
    }

    fun requestDefault(a: Activity) {
        try {
            a.startActivityForResult(roleIntent(a), 77)
        } catch (e: Exception) {
            try {
                a.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(a, "تعذر فتح الإعداد، غيّر تطبيق الهاتف من إعدادات التطبيقات", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun granted(c: Context, p: String): Boolean =
        c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    fun name(c: Context, number: String): String {
        if (number.isBlank() || !granted(c, Manifest.permission.READ_CONTACTS)) return ""
        return try {
            val u = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
            )
            c.contentResolver.query(
                u, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { if (it.moveToFirst()) it.getString(0) ?: "" else "" } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // يرجع true إذا بدأ الاتصال
    fun place(a: Activity, number: String): Boolean {
        if (number.isBlank()) return false
        if (!granted(a, Manifest.permission.CALL_PHONE)) {
            a.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 41)
            return false
        }
        return try {
            val tm = a.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.placeCall(Uri.fromParts("tel", number, null), Bundle())
            true
        } catch (e: Exception) {
            try {
                a.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
                true
            } catch (e2: Exception) {
                Toast.makeText(a, "تعذر الاتصال", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
