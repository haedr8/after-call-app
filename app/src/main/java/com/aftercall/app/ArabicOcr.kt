package com.aftercall.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

// قراءة الأرقام العربية (٠١٢٣٤٥٦٧٨٩) على الجهاز. النموذج ara.traineddata مضمّن داخل التطبيق.
// الكلاس غير آمن للخيوط: يُستخدم من خيط واحد فقط.
class ArabicOcr(private val ctx: Context) {
    private var api: TessBaseAPI? = null
    @Volatile var ready = false
        private set

    fun open(): Boolean {
        try {
            val root = File(ctx.filesDir, "tesseract")
            val dir = File(root, "tessdata")
            dir.mkdirs()
            val out = File(dir, "ara.traineddata")
            val am = ctx.assets
            val size = try {
                am.openFd("tessdata/ara.traineddata").use { it.length }
            } catch (e: Exception) {
                -1L
            }
            if (!out.exists() || (size > 0 && out.length() != size)) {
                am.open("tessdata/ara.traineddata").use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            }
            val t = TessBaseAPI()
            if (!t.init(root.absolutePath, "ara")) {
                t.recycle()
                return false
            }
            t.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT)
            t.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "٠١٢٣٤٥٦٧٨٩0123456789+.Oo ")
            api = t
            ready = true
            return true
        } catch (e: Throwable) {
            ready = false
            return false
        }
    }

    fun read(b: Bitmap): String {
        val t = api ?: return ""
        return try {
            t.setImage(enhance(b))
            t.getUTF8Text() ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    // رمادي + رفع التباين، يساعد مع الإضاءة الضعيفة وانعكاس الشاشات
    private fun enhance(b: Bitmap): Bitmap {
        return try {
            val o = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
            val c = ColorMatrix()
            c.setSaturation(0f)
            val k = 2.2f
            val t = (-0.5f * k + 0.5f) * 255f
            c.postConcat(ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, t, 0f, k, 0f, 0f, t, 0f, 0f, k, 0f, t, 0f, 0f, 0f, 1f, 0f)))
            val p = Paint()
            p.colorFilter = ColorMatrixColorFilter(c)
            Canvas(o).drawBitmap(b, 0f, 0f, p)
            o
        } catch (e: Throwable) {
            b
        }
    }

    fun close() {
        try {
            api?.recycle()
        } catch (e: Throwable) {
        }
        api = null
        ready = false
    }
}
