package com.aftercall.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

// يستقبل أي رقم يُضغط من تطبيق آخر ويفتح نافذة الخيارات فوقه مباشرة بدون شاشة وسيطة
class TelActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = intent?.data
        if (!Phone.isDefault(this) && Phone.forwardDial(this, d)) {
            finish()
            overridePendingTransition(0, 0)
            return
        }
        val n = if (d != null && d.scheme == "tel") Uri.decode(d.schemeSpecificPart ?: "").trim() else ""
        if (n.isNotBlank() && Store.getBool(this, "intercept", true)) {
            startActivity(
                Intent(this, CallDialogActivity::class.java)
                    .putExtra("number", n).putExtra("name", Phone.name(this, n))
            )
        } else {
            startActivity(
                Intent(this, DialerActivity::class.java).setData(d).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
