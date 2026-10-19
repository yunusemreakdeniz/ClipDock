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
 private val backupCreateCode=701
 private val backupRestoreCode=702
 private var backupPassword:CharArray?=null
 private fun askPassword(title:String,onReady:(CharArray)->Unit) {
  val input=EditText(this).apply {
   hint="En az 10 karakter"
   inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
  }
  val wrap=LinearLayout(this).apply { setPadding(32,8,32,0); addView(input) }
  AlertDialog.Builder(this).setTitle(title).setView(wrap)
   .setNegativeButton("Vazgeç",null)
   .setPositiveButton("Devam") { _,_ ->
    val pass=input.text.toString().toCharArray()
    if(pass.size<10) Toast.makeText(this,"Parola en az 10 karakter olmalı",Toast.LENGTH_LONG).show()
    else onReady(pass)
   }.show()
 }

 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(resultCode!=RESULT_OK) {
   backupPassword?.fill('\u0000'); backupPassword=null
   return
  }
  val uri=data?.data ?: return
  val history=History(this)
  try {
   when(requestCode) {
    backupCreateCode -> {
     val pass=backupPassword ?: error("Parola eksik")
     try {
      val encrypted=Crypto.encryptBackup(history.exportBackup(),pass)
      contentResolver.openOutputStream(uri,"wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(encrypted) }
       ?: error("Dosya açılamadı")
     } finally { pass.fill('\u0000'); backupPassword=null }
     Toast.makeText(this,"Yedek dosyası kaydedildi",Toast.LENGTH_LONG).show()
    }
    backupRestoreCode -> {
     val backup=contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
      val content=reader.readText()
      require(content.length<=8_000_000) { "Dosya çok büyük" }
      content
     } ?: error("Dosya açılamadı")
     val pass=backupPassword ?: error("Parola eksik")
     val decrypted=try { Crypto.decryptBackup(backup,pass) }
      finally { pass.fill('\u0000'); backupPassword=null }
     val count=history.importBackup(decrypted)
     sendBroadcast(Intent("com.clipdock.app.COLOR").setPackage(packageName))
     Toast.makeText(this,"$count kayıt geri yüklendi",Toast.LENGTH_LONG).show()
     show()
    }
   }
  } catch(e:Exception) {
   backupPassword?.fill('\u0000'); backupPassword=null
   Toast.makeText(this,"Yedek işlemi başarısız: ${e.message}",Toast.LENGTH_LONG).show()
  }
 }

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
  settingSlider("Yükseklik",h.tabHeight(),20,160," dp",h::setTabHeight)
  settingSlider("Genişlik",h.tabWidth(),8,65," dp",h::setTabWidth)
  root.addView(label("Çentik saydamlığı",19f))
  settingSlider("Görünürlük",h.tabOpacity(),20,100,"%",h::setTabOpacity)
  root.addView(label("Kopyalama algılama (deneysel)",19f))
  root.addView(label("Android 15, erişilebilirlik servisine de panoyu okuma izni garanti etmez. Bu özellik yalnızca bazı uygulamalardaki Kopyala eylemlerini algılamayı dener.",13f))
  root.addView(Button(this).apply {
   text="Erişilebilirlik ayarlarını aç"
   setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
  })
  root.addView(label("Gizli kayıt koruması",19f))
  val bioPrefs=getSharedPreferences("clipdock_security",MODE_PRIVATE)
  root.addView(CheckBox(this).apply {
   text="Göz simgesiyle açarken biyometrik doğrulama iste"
   setTextColor(Color.WHITE)
   isChecked=bioPrefs.getBoolean("biometric",false)
   isEnabled=Build.VERSION.SDK_INT>=28
   setOnCheckedChangeListener { _,checked ->
    if(checked && Build.VERSION.SDK_INT>=29) {
     val manager=getSystemService(android.hardware.biometrics.BiometricManager::class.java)
     if(manager.canAuthenticate()!=android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
      isChecked=false
      Toast.makeText(this@MainActivity,"Önce telefon ayarlarından biyometri tanımla",Toast.LENGTH_LONG).show()
      return@setOnCheckedChangeListener
     }
    }
    bioPrefs.edit().putBoolean("biometric",checked).apply()
   }
  })
  root.addView(label("Yedekleme ve geri yükleme",19f))
  root.addView(label("Yedek AES-256-GCM ile şifrelenir. Parolanı unutursan geri yükleyemezsin. Yedekleme uygulama kaldırılmadan önce yapılmalıdır.",13f))
  root.addView(Button(this).apply {
   text="Şifreli yedek oluştur"
   setOnClickListener {
    askPassword("Yedek parolası belirle") { pass ->
     backupPassword=pass
     val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type="application/octet-stream"
      putExtra(Intent.EXTRA_TITLE,"ClipDock-yedek.cdb")
     }
     startActivityForResult(intent,backupCreateCode)
    }
   }
  })
  root.addView(Button(this).apply {
   text="Yedeği geri yükle"
   setOnClickListener {
    AlertDialog.Builder(this@MainActivity)
     .setTitle("Yedeği geri yükle")
     .setMessage("Mevcut kayıtlar ve çentik ayarları yedekteki verilerle değiştirilecek. Devam edilsin mi?")
     .setNegativeButton("Vazgeç",null)
     .setPositiveButton("Devam") { _,_ ->
      askPassword("Yedek parolasını gir") { pass ->
       backupPassword=pass
       val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type="*/*"
       }
       startActivityForResult(intent,backupRestoreCode)
      }
     }.show()
   }
  })
  val clear=Button(this).apply { text="Geçmişi temizle"; setOnClickListener { h.clear(); Toast.makeText(this@MainActivity,"Geçmiş temizlendi",Toast.LENGTH_SHORT).show() } }
  root.addView(clear)
  val stop=Button(this).apply { text="Çentiği kapat"; setOnClickListener { stopService(Intent(this@MainActivity,DockService::class.java)) } }
  root.addView(stop)
  setContentView(ScrollView(this).apply { addView(root) })
 }
}
