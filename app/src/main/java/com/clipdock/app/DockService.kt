package com.clipdock.app

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*

class DockService: Service() {
 private lateinit var wm:WindowManager
 private lateinit var history:History
 private lateinit var clipboard:ClipboardManager
 private var tab:View?=null
 private var panel:View?=null
 private val handler=Handler(Looper.getMainLooper())
 private var tabParams:WindowManager.LayoutParams?=null
 private val listener=ClipboardManager.OnPrimaryClipChangedListener { handler.post { capture() } }
 private val colorReceiver=object:BroadcastReceiver() { override fun onReceive(c:Context,i:Intent) { hideTab(); showTab() } }
 override fun onBind(i:Intent?):IBinder?=null
 override fun onCreate() {
  super.onCreate()
  history=History(this); wm=getSystemService(WINDOW_SERVICE) as WindowManager
  clipboard=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
  val channel=NotificationChannel("dock","ClipDock çentiği",NotificationManager.IMPORTANCE_LOW)
  getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  val notification=Notification.Builder(this,"dock").setSmallIcon(android.R.drawable.ic_menu_edit)
   .setContentTitle("ClipDock çalışıyor").setContentText("Sol kenardaki çentikle geçmişi aç")
   .setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)).build()
  startForeground(11,notification)
  clipboard.addPrimaryClipChangedListener(listener)
  registerReceiver(colorReceiver,IntentFilter("com.clipdock.app.COLOR"),Context.RECEIVER_NOT_EXPORTED)
  showTab()
 }
 private fun bg(color:Int,radii:FloatArray):GradientDrawable =
  GradientDrawable().apply { setColor(color); cornerRadii=radii }
 private fun dp(v:Int):Int=(resources.displayMetrics.density*v).toInt()
 private fun params(width:Int,height:Int):WindowManager.LayoutParams =
  WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
   PixelFormat.TRANSLUCENT).apply { gravity=Gravity.START or Gravity.TOP; x=0; y=resources.displayMetrics.heightPixels/3 }
 private fun showTab() {
  if(tab!=null) return
  val view=TextView(this).apply {
   text="▤"; textSize=22f; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
   background=bg(history.color(),floatArrayOf(0f,0f,dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f))
  }
  val p=params(dp(33),dp(86))
  var startY=0f; var originalY=0; var moved=false
  view.setOnTouchListener { _,event ->
   when(event.action) {
    MotionEvent.ACTION_DOWN -> { startY=event.rawY; originalY=p.y; moved=false; true }
    MotionEvent.ACTION_MOVE -> {
     if(kotlin.math.abs(event.rawY-startY)>dp(8)) moved=true
     if(moved) { p.y=(originalY+(event.rawY-startY)).toInt().coerceIn(0,resources.displayMetrics.heightPixels-dp(100)); wm.updateViewLayout(view,p) }
     true
    }
    MotionEvent.ACTION_UP -> { if(!moved) showPanel(); true }
    else -> false
   }
  }
  wm.addView(view,p); tab=view; tabParams=p
 }
 private fun hideTab() { tab?.let { wm.removeView(it) }; tab=null }
 private fun capture() {
  try {
   val item=clipboard.primaryClip?.takeIf { it.itemCount>0 }?.getItemAt(0) ?: return
   val value=item.coerceToText(this)?.toString() ?: return
   history.add(value)
   if(panel!=null) { hidePanel(); showPanel(false) }
  } catch (_:Exception) { /* OS may deny clipboard access in background */ }
 }
 private fun showPanel(readClipboard:Boolean=true) {
  if(panel!=null) return
  if(readClipboard) capture()
  val screenWidth=resources.displayMetrics.widthPixels
  val width=(screenWidth*0.88f).toInt()
  val outer=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(20),dp(16),dp(16))
   background=bg(0xFF151C2A.toInt(),floatArrayOf(0f,0f,dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),0f,0f))
   elevation=dp(14).toFloat()
  }
  val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
  val title=TextView(this).apply { text="▣  ClipDock"; textSize=22f; setTextColor(Color.WHITE); setTypeface(null,1) }
  header.addView(title,LinearLayout.LayoutParams(0,dp(54),1f))
  val close=TextView(this).apply { text="✕"; textSize=23f; gravity=Gravity.CENTER; setTextColor(0xFFB9C2D5.toInt()); setOnClickListener { hidePanel() } }
  header.addView(close,LinearLayout.LayoutParams(dp(44),dp(44)))
  outer.addView(header)
  val sub=TextView(this).apply { text="SON KOPYALANANLAR  •  ${history.all().size}/10"; textSize=12f; setTextColor(0xFF9BAAC3.toInt()); setPadding(0,dp(4),0,dp(12)) }
  outer.addView(sub)
  val scroll=ScrollView(this)
  val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  val entries=history.all()
  if(entries.isEmpty()) list.addView(itemText("Henüz kayıt yok. Android izin verdiğinde kopyalanan metinler burada görünür.",14f))
  entries.forEachIndexed { index,value ->
   val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(8),dp(8),dp(8)); background=bg(0xFF242E40.toInt(),FloatArray(8){dp(12).toFloat()}) }
   val preview=TextView(this).apply { text=value; textSize=14f; setTextColor(Color.WHITE); maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END; setOnClickListener { copy(value) } }
   row.addView(preview,LinearLayout.LayoutParams(0,dp(52),1f))
   val copy=TextView(this).apply { text="⧉"; textSize=25f; gravity=Gravity.CENTER; setTextColor(0xFFA68CFF.toInt()); setOnClickListener { copy(value) } }
   row.addView(copy,LinearLayout.LayoutParams(dp(42),dp(50)))
   val delete=TextView(this).apply { text="×"; textSize=25f; gravity=Gravity.CENTER; setTextColor(0xFFFF879B.toInt()); setOnClickListener { history.remove(value); hidePanel(); showPanel(false) } }
   row.addView(delete,LinearLayout.LayoutParams(dp(30),dp(50)))
   list.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
  }
  scroll.addView(list); outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val footer=itemText("Temizle",14f).apply { gravity=Gravity.END; setTextColor(0xFFFF879B.toInt()); setOnClickListener { history.clear(); hidePanel(); showPanel(false) } }
  outer.addView(footer)
  val p=params(width,(resources.displayMetrics.heightPixels*0.74f).toInt()).apply { y=(resources.displayMetrics.heightPixels*0.13f).toInt() }
  wm.addView(outer,p); panel=outer
 }
 private fun itemText(value:String,size:Float)=TextView(this).apply { text=value; textSize=size; setTextColor(Color.WHITE); setPadding(dp(8),dp(10),dp(8),dp(10)) }
 private fun copy(value:String) {
  clipboard.setPrimaryClip(ClipData.newPlainText("ClipDock",value))
  history.add(value); hidePanel()
  Toast.makeText(this,"Panoya kopyalandı",Toast.LENGTH_SHORT).show()
 }
 private fun hidePanel() { panel?.let { wm.removeView(it) }; panel=null }
 override fun onDestroy() {
  clipboard.removePrimaryClipChangedListener(listener); unregisterReceiver(colorReceiver)
  hidePanel(); hideTab(); super.onDestroy()
 }
}
