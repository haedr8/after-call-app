package com.aftercall.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// فحص ملف حالة ثابت (status.json) من صفحة المشروع. لا يُرسل أي بيانات، فقط قراءة الملف.
object Remote {
    private const val STATUS = "https://haedr8.github.io/after-call-app/status.json"
    private val main = Handler(Looper.getMainLooper())

    private fun p(c: Context) = c.getSharedPreferences("remote", Context.MODE_PRIVATE)

    fun versionNum(c: Context): Int = try {
        (c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "")
            .filter { it.isDigit() }.toIntOrNull() ?: 0
    } catch (e: Exception) {
        0
    }

    fun needUpdate(c: Context): Boolean {
        val q = p(c)
        val min = q.getInt("min", 0)
        return q.getBoolean("enabled", true) && min > 0 && versionNum(c) < min
    }

    // نص الحظر إذا التطبيق موقوف أو يحتاج تحديث إجباري، وإلا null
    private const val CLOSED = "توقّف هذا التطبيق عن العمل.\nللحصول على نسخة Google Play، أرسل بريدك الإلكتروني (Gmail) بدون كلمة المرور إلى رقم الواتساب 07712384075، وسيتم إرسال النسخة إليك."

    // التطبيق مغلق نهائيًا: الرسالة ثابتة داخل الكود ولا تعتمد على الإنترنت
    fun blocked(c: Context): String? {
        if (true) return CLOSED
        val q = p(c)
        val msg = q.getString("msg", "") ?: ""
        if (!q.getBoolean("enabled", true)) {
            return if (msg.isBlank()) "تم إيقاف التطبيق من قبل المطوّر." else msg
        }
        if (needUpdate(c)) {
            return if (msg.isBlank()) "يلزم تحديث التطبيق للاستمرار." else msg
        }
        return null
    }

    fun refresh(c: Context, force: Boolean, onChanged: (() -> Unit)? = null) {
        val app = c.applicationContext
        val q = p(app)
        val now = System.currentTimeMillis()
        val every = 6L * 3600 * 1000
        if (!force && now - q.getLong("last", 0L) < every) return
        q.edit().putLong("last", now - every + 3600L * 1000).apply() // إعادة المحاولة بعد ساعة عند الفشل
        Thread {
            try {
                val before = blocked(app)
                val u = URL(STATUS + "?t=" + now)
                val cn = u.openConnection() as HttpURLConnection
                cn.connectTimeout = 10000
                cn.readTimeout = 10000
                cn.setRequestProperty("User-Agent", "Haulni-App")
                if (cn.responseCode == 200) {
                    val j = JSONObject(cn.inputStream.bufferedReader().use { it.readText() })
                    if (j.has("enabled") && j.get("enabled") is Boolean) {
                        q.edit()
                            .putBoolean("enabled", j.getBoolean("enabled"))
                            .putInt("min", j.optInt("min_version", 0))
                            .putString("msg", j.optString("message", ""))
                            .putLong("last", System.currentTimeMillis())
                            .apply()
                    }
                }
                cn.disconnect()
                if (before != blocked(app) && onChanged != null) main.post { onChanged() }
            } catch (e: Exception) {
            }
        }.start()
    }
}
