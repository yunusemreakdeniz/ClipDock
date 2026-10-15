package com.clipdock.app
import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.provider.Settings
import android.net.Uri
import android.view.*
import android.widget.*

class MainActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor=Color.rgb(13,17,29)
  window.navigationBarColor=Color.rgb(13,17,29)
  show()
 }
 private fun show() {
  val h=History(this)
  val root=LinearLayout(this).apply { orientation=1; setPadding(36,55,36,28); setBackgroundColor(Color.rgb(13,17,29)) }
  fun label(s:String,size:Float=16f):TextView = TextView(this).apply { text=s; textSize=size; setTextColor(Color.WHITE); setPadding(0,14,0,14) }
  root.addView(label("▣  ClipDock",30f))
  root.addView(label("Kopyalama geçmişin, bir dokunuş uzağında.",15f))
  root.addView(label("Android 10+ arka planda tüm kopyalamaları üçüncü taraf uygulamalara bildirmez. Bu uygulama yalnızca sistemin erişim verdiği kayıtları otomatik yakalar. Çentik açıldığında mevcut pano da kontrol edilir.",13f))
  val start=Button(this).apply { text="Çentiği etkinleştir"; setOnClickListener {
   if(!Settings.canDrawOverlays(this@MainActivity)) {
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
   } else {
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
      requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),12)
    try { if(Build.VERSION.SDK_INT>=26) startForegroundService(Intent(this@MainActivity,DockService::class.java)) else startService(Intent(this@MainActivity,DockService::class.java)) }
    catch(e:Exception) { Toast.makeText(this@MainActivity,e.message,Toast.LENGTH_LONG).show() }
   }
  }}
  root.addView(start)
  root.addView(label("Çentik rengi",19f))
  val colors=intArrayOf(0xFF8358F5.toInt(),0xFF559EFF.toInt(),0xFF31CC93.toInt(),0xFFFF873A.toInt(),0xFFF75D87.toInt(),0xFF66758C.toInt())
  val palette=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
  colors.forEach { color -> palette.addView(Button(this).apply {
   text="●"; textSize=20f; setTextColor(color); setOnClickListener { h.setColor(color); sendBroadcast(Intent("com.clipdock.app.COLOR").setPackage(packageName)) }
  },LinearLayout.LayoutParams(0,58,1f)) }
  root.addView(palette)
  root.addView(label("Çentik boyutu",19f))
  fun settingSlider(title:String, current:Int, min:Int, max:Int, suffix:String, save:(Int)->Unit) {
   val text=label("$title: $current$suffix",15f)
   root.addView(text)
   val slider=SeekBar(this).apply {
    this.max=max-min
    progress=current-min
    progressTintList=android.content.res.ColorStateList.valueOf(h.color())
    thumbTintList=android.content.res.ColorStateList.valueOf(h.color())
    setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
     override fun onProgressChanged(bar:SeekBar?, value:Int, fromUser:Boolean) {
      val newValue=value+min
      text.text="$title: $newValue$suffix"
      if(fromUser) {
       save(newValue)
       sendBroadcast(Intent("com.clipdock.app.COLOR").setPackage(packageName))
      }
     }
     override fun onStartTrackingTouch(bar:SeekBar?) {}
     override fun onStopTrackingTouch(bar:SeekBar?) {}
    })
   }
   root.addView(slider)
  }
  settingSlider("Yükseklik",h.tabHeight(),48,160," dp",h::setTabHeight)
  settingSlider("Genişlik",h.tabWidth(),22,65," dp",h::setTabWidth)
  root.addView(label("Çentik saydamlığı",19f))
  settingSlider("Görünürlük",h.tabOpacity(),20,100,"%",h::setTabOpacity)
  root.addView(label("Kopyalama algılama (deneysel)",19f))
  root.addView(label("Android 15, erişilebilirlik servisine de panoyu okuma izni garanti etmez. Bu özellik yalnızca bazı uygulamalardaki Kopyala eylemlerini algılamayı dener.",13f))
  root.addView(Button(this).apply {
   text="Erişilebilirlik ayarlarını aç"
   setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
  })
  val clear=Button(this).apply { text="Geçmişi temizle"; setOnClickListener { h.clear(); Toast.makeText(this@MainActivity,"Geçmiş temizlendi",Toast.LENGTH_SHORT).show() } }
  root.addView(clear)
  val stop=Button(this).apply { text="Çentiği kapat"; setOnClickListener { stopService(Intent(this@MainActivity,DockService::class.java)) } }
  root.addView(stop)
  setContentView(ScrollView(this).apply { addView(root) })
 }
}
