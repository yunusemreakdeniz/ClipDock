package com.clipdock.app

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Reads clipboard only while this Activity is actually foreground/resumed. */
class ClipboardCaptureActivity : Activity() {
 private var started = false
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.setDimAmount(0f)
 }
 override fun onResume() {
  super.onResume()
  if(started) return
  started=true
  Handler(Looper.getMainLooper()).postDelayed({
   try {
    val clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip=clipboard.primaryClip
    if(clip!=null && clip.itemCount>0) {
     val value=clip.getItemAt(0).coerceToText(this)?.toString()
     if(!value.isNullOrBlank()) History(this).add(value)
    }
   } catch(e:Exception) {
    Log.e("ClipDock","Foreground clipboard capture failed",e)
   } finally {
    // Even if clipboard read fails, always return to the overlay panel.
    sendBroadcast(Intent("com.clipdock.app.OPEN_PANEL").setPackage(packageName))
    finish()
    overridePendingTransition(0,0)
   }
  },300)
 }
}
