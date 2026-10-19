package com.clipdock.app
import android.content.*
import android.os.Build
import android.provider.Settings
class BootReceiver:BroadcastReceiver() {
 override fun onReceive(context:Context,intent:Intent) {
  // Android 15+ foreground service boot restrictions vary by type/device.
  // Let the user explicitly restart the overlay from the launcher.
 }
}
