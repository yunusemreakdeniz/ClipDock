package com.clipdock.app

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/** Briefly becomes the foreground app so Android can allow clipboard access. */
class ClipboardCaptureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDimAmount(0f)
    }
    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = manager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    clip.getItemAt(0).coerceToText(this)?.toString()?.let { History(this).add(it) }
                }
            } catch (_: Exception) { }
            sendBroadcast(Intent("com.clipdock.app.OPEN_PANEL").setPackage(packageName))
            finish()
            overridePendingTransition(0, 0)
        }, 160)
    }
}
