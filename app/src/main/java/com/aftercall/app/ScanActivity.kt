package com.aftercall.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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

// مسح رقم هاتف من ورقة/أوردر بالكاميرا. القراءة تتم على الجهاز ولا تُحفظ الصور ولا تُرسل.
class ScanActivity : ComponentActivity() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val exec = Executors.newSingleThreadExecutor()
    private val seen = HashMap<String, Int>()
    @Volatile private var done = false
    private var camera: Camera? = null
    private var torch = false
    private lateinit var preview: PreviewView
    private lateinit var hint: TextView

    private fun dp(v: Int) = T.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())
        preview = PreviewView(this)
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))

        // إطار توجيه
        val frame = FrameLayout(this)
        frame.background = T.shape(this, 0x00000000, 16, 0xFFFFFFFF.toInt())
        val fp = FrameLayout.LayoutParams(-1, dp(120))
        fp.gravity = Gravity.CENTER
        fp.setMargins(dp(24), 0, dp(24), 0)
        root.addView(frame, fp)

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.VERTICAL
        bottom.gravity = Gravity.CENTER
        bottom.layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
        bottom.setBackgroundColor(0xCC000000.toInt())
        bottom.setPadding(dp(16), dp(12), dp(16), dp(16))
        Ui.fit(bottom, dp(16), dp(16))
        hint = T.label(this, "وجّه الكاميرا نحو رقم الهاتف", 16f, 0xFFFFFFFF.toInt(), true)
        hint.gravity = Gravity.CENTER
        bottom.addView(hint)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
        val b1 = T.button(this, "🔦 إضاءة", false) { toggleTorch() }
        val b2 = T.button(this, "إغلاق", false) { finish() }
        val lp = LinearLayout.LayoutParams(0, -2, 1f)
        lp.setMargins(dp(4), dp(10), dp(4), 0)
        row.addView(b1, lp)
        row.addView(b2, LinearLayout.LayoutParams(lp))
        bottom.addView(row)
        val bp = FrameLayout.LayoutParams(-1, -2)
        bp.gravity = Gravity.BOTTOM
        root.addView(bottom, bp)
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start()
        else Grant.request(this, arrayOf(Manifest.permission.CAMERA), 61)
    }

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
    }

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
                ia.setAnalyzer(exec) { proxy -> analyze(proxy) }
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, pv, ia)
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر تشغيل الكاميرا", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val img = proxy.image
        if (img == null || done) {
            proxy.close()
            return
        }
        val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
        recognizer.process(input)
            .addOnSuccessListener { t -> onText(t.text) }
            .addOnCompleteListener { proxy.close() }
    }

    private fun onText(text: String) {
        if (done) return
        val n = PhoneScan.find(text) ?: return
        val c = (seen[n] ?: 0) + 1
        seen[n] = c
        hint.text = "تم العثور على: $n"
        if (c < 2) return // نتأكد بقراءتين متطابقتين لتجنب الخطأ
        done = true
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("رقم", n))
        } catch (e: Exception) {
        }
        try {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
        }
        startActivity(Intent(this, CallDialogActivity::class.java).putExtra("number", n).putExtra("name", ""))
        finish()
    }

    private fun toggleTorch() {
        torch = !torch
        camera?.cameraControl?.enableTorch(torch)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            recognizer.close()
        } catch (e: Exception) {
        }
        exec.shutdown()
    }
}
