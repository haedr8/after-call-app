package com.aftercall.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// ماسح أرقام الهواتف بالكاميرا:
// - ML Kit للأرقام اللاتينية (كل إطار) + Tesseract للأرقام العربية (٠٧٧…) كل نحو 0.7 ثانية
// - القراءة داخل الإطار الإرشادي فقط، والصور لا تُحفظ ولا تُرسل
// - بعد التأكد (قراءتان متطابقتان) تظهر شاشة مراجعة: اتصال / واتساب / نسخ / خيارات
class ScanActivity : ComponentActivity() {

    private val mlk = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    @Volatile private var arabic: ArabicOcr? = null
    private val camExec = Executors.newSingleThreadExecutor()
    private val tessExec = Executors.newSingleThreadExecutor()
    private val tessBusy = AtomicBoolean(false)
    private val ui = Handler(Looper.getMainLooper())

    @Volatile private var lastTess = 0L
    @Volatile private var done = false
    @Volatile private var manual = false
    private val hits = LinkedHashMap<String, Int>() // للخيط الرئيسي فقط
    private var selected: String? = null

    private var camera: Camera? = null
    private var torch = false
    private lateinit var preview: PreviewView
    private lateinit var panel: LinearLayout
    private lateinit var hint: TextView

    private fun dp(v: Int) = T.dp(this, v)

    // ---------- شاشة ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())
        preview = PreviewView(this)
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))
        root.addView(GuideView(this), FrameLayout.LayoutParams(-1, -1))

        hint = T.label(this, "وجّه الكاميرا نحو رقم الهاتف", 16f, 0xFFFFFFFF.toInt(), true)
        hint.gravity = Gravity.CENTER
        hint.setBackgroundColor(0x99000000.toInt())
        hint.setPadding(dp(12), dp(10), dp(12), dp(10))
        val hp = FrameLayout.LayoutParams(-1, -2)
        hp.gravity = Gravity.TOP
        root.addView(hint, hp)
        Ui.fit(hint, dp(12), dp(10))

        panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.gravity = Gravity.CENTER
        panel.layoutDirection = View.LAYOUT_DIRECTION_RTL
        panel.setBackgroundColor(0xDD000000.toInt())
        panel.setPadding(dp(16), dp(12), dp(16), dp(16))
        Ui.fit(panel, dp(16), dp(16))
        val pp = FrameLayout.LayoutParams(-1, -2)
        pp.gravity = Gravity.BOTTOM
        root.addView(panel, pp)
        setContentView(root)
        renderScan()
        setupGestures()

        // تحميل محرك الأرقام العربية بالخلفية
        tessExec.execute {
            val a = ArabicOcr(applicationContext)
            a.open()
            arabic = a
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
        else Grant.request(this, arrayOf(Manifest.permission.CAMERA), 61)
    }

    private fun row(vararg v: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.layoutDirection = View.LAYOUT_DIRECTION_RTL
        for (x in v) {
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.setMargins(dp(4), dp(8), dp(4), 0)
            r.addView(x, lp)
        }
        return r
    }

    private fun renderScan() {
        panel.removeAllViews()
        hint.text = "وجّه الكاميرا نحو رقم الهاتف داخل الإطار"
        val zoom = SeekBar(this)
        zoom.max = 100
        zoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) camera?.cameraControl?.setLinearZoom(p / 100f)
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        panel.addView(T.label(this, "تكبير", 12f, 0xFFCCCCCC.toInt()))
        panel.addView(zoom, LinearLayout.LayoutParams(-1, -2))
        panel.addView(
            row(
                T.button(this, "📸 التقاط الآن", true) { captureNow() },
                T.button(this, "🖼 من الصور", false) { pickImage() }
            )
        )
        panel.addView(
            row(
                T.button(this, "🔦 إضاءة", false) { toggleTorch() },
                T.button(this, "إغلاق", false) { finish() }
            )
        )
    }

    // رقم واحد: نفتح نافذة الخيارات مباشرة (نفس نافذة بقية التطبيق).
    // عدة أرقام: نعرضها للاختيار، والضغط على أي رقم يفتح نافذة الخيارات.
    private fun renderResult() {
        val list = hits.entries.sortedByDescending { it.value }.map { it.key }
        if (list.size == 1) {
            options(list[0])
            return
        }
        panel.removeAllViews()
        hint.text = "تم العثور على ${list.size} أرقام، اختر الرقم"
        for (n in list) {
            val b = T.button(this, "📞  $n", true) { options(n) }
            b.textDirection = View.TEXT_DIRECTION_LTR
            b.textSize = 18f
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, dp(6), 0, 0)
            panel.addView(b, lp)
        }
        panel.addView(
            row(
                T.button(this, "🔄 مسح جديد", false) { rescan() },
                T.button(this, "إغلاق", false) { finish() }
            )
        )
    }

    private fun act(f: (String) -> Unit) {
        val n = selected ?: return
        f(n)
    }

    private fun copy(n: String) {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("رقم", n))
            Toast.makeText(this, "تم نسخ الرقم", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
        }
    }

    private fun openWa(n: String) {
        try {
            startActivity(Store.waIntent(this, n, "", Store.waPackage(this)))
        } catch (e: Exception) {
            try {
                startActivity(Store.waIntent(this, n, "", null))
            } catch (e2: Exception) {
                Toast.makeText(this, "واتساب غير موجود", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun options(n: String) {
        startActivity(Intent(this, CallDialogActivity::class.java).putExtra("number", n).putExtra("name", ""))
        finish()
    }

    private fun rescan() {
        hits.clear()
        selected = null
        manual = false
        done = false
        renderScan()
    }

    // ---------- أذونات ----------
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 61) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) start()
            else {
                Toast.makeText(this, "اسمح للكاميرا حتى يعمل المسح", Toast.LENGTH_LONG).show()
                Grant.afterResult(this, arrayOf(Manifest.permission.CAMERA))
                finish()
            }
        }
        if (requestCode == 41 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            selected?.let { if (Phone.place(this, it)) finish() }
        }
    }

    // ---------- الكاميرا ----------
    private fun start() {
        val f = ProcessCameraProvider.getInstance(this)
        f.addListener({
            try {
                val provider = f.get()
                val pv = Preview.Builder().build()
                pv.setSurfaceProvider(preview.surfaceProvider)
                val rs = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    ).build()
                val ia = ImageAnalysis.Builder()
                    .setResolutionSelector(rs)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                ia.setAnalyzer(camExec) { proxy -> analyze(proxy) }
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, pv, ia)
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر تشغيل الكاميرا", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val scale = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val c = camera ?: return true
                val z = c.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                c.cameraControl.setZoomRatio(z * d.scaleFactor)
                return true
            }
        })
        val tap = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val c = camera ?: return true
                try {
                    val pt = preview.meteringPointFactory.createPoint(e.x, e.y)
                    c.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(pt).build())
                } catch (ex: Exception) {
                }
                return true
            }
        })
        preview.setOnTouchListener { _, ev ->
            scale.onTouchEvent(ev)
            tap.onTouchEvent(ev)
            true
        }
    }

    private fun toggleTorch() {
        torch = !torch
        camera?.cameraControl?.enableTorch(torch)
    }

    // ---------- التحليل ----------
    // الإطار: شريط بوسط الصورة بارتفاع 42%
    private fun frameBitmap(proxy: ImageProxy): Bitmap {
        var b = proxy.toBitmap()
        val rot = proxy.imageInfo.rotationDegrees
        if (rot != 0) {
            val m = Matrix()
            m.postRotate(rot.toFloat())
            b = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        }
        val h = (b.height * 0.42f).toInt().coerceAtLeast(64)
        val y = ((b.height - h) / 2).coerceAtLeast(0)
        b = Bitmap.createBitmap(b, 0, y, b.width, minOf(h, b.height - y))
        if (b.width > 1280) {
            val s = 1280f / b.width
            b = Bitmap.createScaledBitmap(b, 1280, (b.height * s).toInt().coerceAtLeast(1), true)
        }
        return b
    }

    private fun analyze(proxy: ImageProxy) {
        if (done) {
            proxy.close()
            return
        }
        val bmp = try {
            frameBitmap(proxy)
        } catch (e: Throwable) {
            proxy.close()
            return
        }
        proxy.close()
        process(bmp, true)
    }

    // يشغّل المحركين على الصورة؛ throttle=true أثناء البث المباشر
    private fun process(bmp: Bitmap, throttle: Boolean) {
        mlk.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { t -> onNumbers(PhoneScan.findAll(this, t.text)) }
        val ar = arabic ?: return
        if (!ar.ready) return
        val now = SystemClock.elapsedRealtime()
        if (throttle && !manual && now - lastTess < 650) return
        if (!tessBusy.compareAndSet(false, true)) return
        lastTess = now
        tessExec.execute {
            try {
                val txt = ar.read(bmp)
                val nums = PhoneScan.findAll(applicationContext, txt)
                ui.post { onNumbers(nums) }
            } finally {
                tessBusy.set(false)
            }
        }
    }

    private fun onNumbers(nums: List<String>) {
        if (done || nums.isEmpty()) return
        for (n in nums) hits[n] = (hits[n] ?: 0) + 1
        val best = hits.entries.maxByOrNull { it.value } ?: return
        hint.text = "جارٍ التأكد: ${best.key}"
        if (best.value >= 2 || manual) {
            done = true
            manual = false
            try {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
            }
            renderResult()
        }
    }

    private fun captureNow() {
        manual = true
        hint.text = "جارٍ التحليل…"
        ui.postDelayed({
            if (manual && !done) {
                manual = false
                Toast.makeText(this, "لم أجد رقم هاتف، قرّب الكاميرا وحاول مرة ثانية", Toast.LENGTH_LONG).show()
                hint.text = "وجّه الكاميرا نحو رقم الهاتف داخل الإطار"
            }
        }, 2500)
    }

    // ---------- من الصور ----------
    private fun pickImage() {
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "اختر صورة"), 71
            )
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الصور", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode == 71 && uri != null) {
            hint.text = "جارٍ قراءة الصورة…"
            done = true // نوقف البث أثناء قراءة الصورة
            tessExec.execute { readPicked(uri) }
        }
    }

    private fun decode(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { d, info, _ ->
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val m = maxOf(info.size.width, info.size.height)
                    if (m > 2400) {
                        val s = 2400f / m
                        d.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt())
                    }
                }
            } else {
                val o = BitmapFactory.Options()
                o.inSampleSize = 2
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun readPicked(uri: Uri) {
        val bmp = decode(uri)
        if (bmp == null) {
            ui.post {
                done = false
                Toast.makeText(this, "تعذر قراءة الصورة", Toast.LENGTH_SHORT).show()
                renderScan()
            }
            return
        }
        val found = LinkedHashSet<String>()
        // اللاتيني (ML Kit) ثم العربي (Tesseract) على الصورة كاملة
        try {
            val t = com.google.android.gms.tasks.Tasks.await(mlk.process(InputImage.fromBitmap(bmp, 0)))
            found.addAll(PhoneScan.findAll(applicationContext, t.text))
        } catch (e: Throwable) {
        }
        val ar = arabic
        if (ar != null && ar.ready) {
            try {
                found.addAll(PhoneScan.findAll(applicationContext, ar.read(bmp)))
            } catch (e: Throwable) {
            }
        }
        ui.post {
            if (found.isEmpty()) {
                done = false
                Toast.makeText(this, "لم أجد رقم هاتف بالصورة", Toast.LENGTH_LONG).show()
                renderScan()
            } else {
                hits.clear()
                for (n in found) hits[n] = 2
                done = true
                renderResult()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        done = true
        ui.removeCallbacksAndMessages(null)
        try {
            mlk.close()
        } catch (e: Exception) {
        }
        camExec.shutdown()
        tessExec.execute { arabic?.close() }
        tessExec.shutdown()
    }

    // إطار إرشادي: تعتيم أعلى وأسفل الشريط، وإطار بزوايا
    private class GuideView(c: Context) : View(c) {
        private val dim = Paint().apply { color = 0x88000000.toInt() }
        private val line = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f * c.resources.displayMetrics.density
            isAntiAlias = true
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val bh = h * 0.42f
            val top = (h - bh) / 2
            val bottom = top + bh
            canvas.drawRect(0f, 0f, w, top, dim)
            canvas.drawRect(0f, bottom, w, h, dim)
            val m = 16f * resources.displayMetrics.density
            canvas.drawRoundRect(RectF(m, top + m, w - m, bottom - m), m, m, line)
        }
    }
}
