package com.aftercall.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateInfo(val code: Long, val tag: String, val notes: String, val url: String)

object Updater {

    private const val API = "https://api.github.com/repos/haedr8/after-call-app/releases/latest"
    private val main = Handler(Looper.getMainLooper())

    fun currentCode(a: Activity): Long {
        return try {
            val pi = a.packageManager.getPackageInfo(a.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) {
                pi.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun check(a: Activity, onResult: (UpdateInfo?, Boolean) -> Unit) {
        Thread {
            var info: UpdateInfo? = null
            var error = false
            try {
                val c = URL(API).openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "Haulni-App")
                if (c.responseCode == 200) {
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    val j = JSONObject(text)
                    val tag = j.optString("tag_name")
                    val code = tag.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    var url = ""
                    val assets = j.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val o = assets.getJSONObject(i)
                            if (o.optString("name").endsWith(".apk")) {
                                url = o.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    if (code > currentCode(a) && url.isNotEmpty()) {
                        info = UpdateInfo(code, tag, j.optString("body"), url)
                    }
                } else {
                    error = true
                }
                c.disconnect()
            } catch (e: Exception) {
                error = true
            }
            val result = info
            val failed = error
            main.post { onResult(result, failed) }
        }.start()
    }

    fun canInstall(a: Activity): Boolean {
        return Build.VERSION.SDK_INT < 26 || a.packageManager.canRequestPackageInstalls()
    }

    fun openInstallSettings(a: Activity) {
        try {
            a.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName))
            )
        } catch (e: Exception) {
        }
    }

    fun download(a: Activity, info: UpdateInfo, onProgress: (Int) -> Unit, onDone: (File?) -> Unit) {
        Thread {
            var out: File? = null
            try {
                val dir = File(a.cacheDir, "updates")
                dir.mkdirs()
                dir.listFiles()?.forEach { it.delete() }
                val f = File(dir, "haulni-" + info.code + ".apk")
                val c = URL(info.url).openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                c.setRequestProperty("User-Agent", "Haulni-App")
                val total = c.contentLength
                c.inputStream.use { ins ->
                    f.outputStream().use { os ->
                        val buf = ByteArray(16384)
                        var sum = 0L
                        var last = -1
                        while (true) {
                            val read = ins.read(buf)
                            if (read < 0) break
                            os.write(buf, 0, read)
                            sum += read
                            if (total > 0) {
                                val p = (sum * 100 / total).toInt()
                                if (p != last) {
                                    last = p
                                    main.post { onProgress(p) }
                                }
                            }
                        }
                    }
                }
                c.disconnect()
                if (f.length() > 0) out = f
            } catch (e: Exception) {
                out = null
            }
            val result = out
            main.post { onDone(result) }
        }.start()
    }

    fun install(a: Activity, f: File) {
        try {
            val uri = FileProvider.getUriForFile(a, a.packageName + ".fileprovider", f)
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, "application/vnd.android.package-archive")
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            a.startActivity(i)
        } catch (e: Exception) {
        }
    }
}
