package com.clipdock.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper

/**
 * Best-effort detector. Android 15 may block clipboard reads even from accessibility services.
 * Never reads screen text or sends data over the network.
 */
class CopyAccessibilityService : AccessibilityService() {
 private val handler = Handler(Looper.getMainLooper())
 private var lastAttempt = 0L
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  val type = event?.eventType ?: return
  if(type != AccessibilityEvent.TYPE_VIEW_CLICKED &&
     type != AccessibilityEvent.TYPE_ANNOUNCEMENT &&
     type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
     type != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return
  val now = android.os.SystemClock.elapsedRealtime()
  if(now-lastAttempt < 1200) return
  val words = event.text?.joinToString(" ")?.lowercase(java.util.Locale.ROOT) ?: ""
  val description = event.contentDescription?.toString()?.lowercase(java.util.Locale.ROOT) ?: ""
  val copyHint=listOf("kopyala","copied","copy","panoya kopyalandı").any { words.contains(it) || description.contains(it) }
  if(!copyHint && type!=AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return
  lastAttempt = now
  handler.postDelayed({
   try {
    val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip=clipboard.primaryClip ?: return@postDelayed
    if(clip.itemCount==0) return@postDelayed
    val value=clip.getItemAt(0).coerceToText(this)?.toString() ?: return@postDelayed
    History(this).add(value)
   } catch (_:SecurityException) {} catch (_:Exception) {}
  },250)
 }
 override fun onInterrupt() {}
}
