package com.clipdock.app

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.Bundle
import android.os.CancellationSignal

class UnlockActivity:Activity() {
 companion object {
  var onUnlock:(()->Unit)?=null
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  if(android.os.Build.VERSION.SDK_INT<28) { finish(); return }
  BiometricPrompt.Builder(this)
   .setTitle("ClipDock gizli kayıt")
   .setSubtitle("Kaydı görüntülemek için doğrula")
   .setNegativeButton("Vazgeç",mainExecutor) { _,_ -> finish() }
   .build().authenticate(CancellationSignal(),mainExecutor,object:BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult) {
     onUnlock?.invoke()
     onUnlock=null
     finish()
    }
    override fun onAuthenticationError(code:Int,message:CharSequence) {
     onUnlock=null
     finish()
    }
   })
 }
 override fun onDestroy() {
  super.onDestroy()
  if(isFinishing) onUnlock=null
 }
}
