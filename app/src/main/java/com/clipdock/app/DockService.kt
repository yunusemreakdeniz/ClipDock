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
 private var panelList:LinearLayout?=null
 private var panelCounter:TextView?=null
 private var ignoreOwnCopy=false
 private val handler=Handler(Looper.getMainLooper())
 private var tabParams:WindowManager.LayoutParams?=null
 private val listener=ClipboardManager.OnPrimaryClipChangedListener { handler.post { if(!ignoreOwnCopy) capture() } }
 private val openReceiver=object:BroadcastReceiver() {
  override fun onReceive(c:Context,i:Intent) { showPanel(false) }
 }
 private val colorReceiver=object:BroadcastReceiver() { override fun onReceive(c:Context,i:Intent) { updateTab() } }
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
  registerReceiver(openReceiver,IntentFilter("com.clipdock.app.OPEN_PANEL"),Context.RECEIVER_NOT_EXPORTED)
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
   text=""; gravity=Gravity.CENTER
   background=bg(history.color(),floatArrayOf(0f,0f,dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f))
   alpha=history.tabOpacity()/100f
  }
  val p=params(dp(history.tabWidth()),dp(history.tabHeight()))
  var startY=0f; var originalY=0; var moved=false
  view.setOnTouchListener { _,event ->
   when(event.action) {
    MotionEvent.ACTION_DOWN -> { startY=event.rawY; originalY=p.y; moved=false; true }
    MotionEvent.ACTION_MOVE -> {
     if(kotlin.math.abs(event.rawY-startY)>dp(8)) moved=true
     if(moved) { p.y=(originalY+(event.rawY-startY)).toInt().coerceIn(0,resources.displayMetrics.heightPixels-dp(history.tabHeight()+14)); wm.updateViewLayout(view,p) }
     true
    }
    MotionEvent.ACTION_UP -> { if(!moved) {
      val intent=Intent(this@DockService,ClipboardCaptureActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
      try { startActivity(intent) } catch (_:Exception) { showPanel(false) }
     }; true }
    else -> false
   }
  }
  wm.addView(view,p); tab=view; tabParams=p
 }
 private fun updateTab() {
  val view=tab ?: return
  val lp=tabParams ?: return
  lp.width=dp(history.tabWidth())
  lp.height=dp(history.tabHeight())
  view.background=bg(history.color(),floatArrayOf(0f,0f,dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f))
  view.alpha=history.tabOpacity()/100f
  wm.updateViewLayout(view,lp)
 }
 private fun hideTab() { tab?.let { wm.removeView(it) }; tab=null }
 private fun capture() {
  try {
   val item=clipboard.primaryClip?.takeIf { it.itemCount>0 }?.getItemAt(0) ?: return
   val value=item.coerceToText(this)?.toString() ?: return
   history.add(value)
   if(panel!=null) refreshList()
  } catch (_:Exception) { /* OS may deny clipboard access in background */ }
 }
 private fun showPanel(readClipboard:Boolean=true) {
  if(panel!=null) return
  // Do not read the clipboard just because the user opens the tab.
  // Only actual clipboard change callbacks may create history entries.
  val screenWidth=resources.displayMetrics.widthPixels
  val width=(screenWidth*0.82f).toInt()
  val outer=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(14),dp(14),dp(12))
   background=bg(0xFF151C2A.toInt(),floatArrayOf(0f,0f,dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),0f,0f))
   elevation=dp(14).toFloat()
  }
  val header=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
  val title=TextView(this).apply { text="▣  ClipDock"; textSize=22f; setTextColor(Color.WHITE); setTypeface(null,1) }
  header.addView(title,LinearLayout.LayoutParams(0,dp(54),1f))
  val close=TextView(this).apply { text="✕"; textSize=23f; gravity=Gravity.CENTER; setTextColor(0xFFB9C2D5.toInt()); setOnClickListener { hidePanel() } }
  header.addView(close,LinearLayout.LayoutParams(dp(44),dp(44)))
  outer.addView(header)
  val settingsToggle=TextView(this).apply {
   text="⚙  Çentik ayarları  ▾"
   textSize=14f; setTextColor(0xFFBCA8FF.toInt())
   setPadding(dp(10),dp(9),dp(10),dp(9))
  }
  outer.addView(settingsToggle)
  val settings=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   visibility=View.GONE
   setPadding(dp(10),dp(8),dp(10),dp(10))
   background=bg(0xFF242E40.toInt(),FloatArray(8){dp(12).toFloat()})
  }
  settingsToggle.setOnClickListener {
   settings.visibility=if(settings.visibility==View.VISIBLE) View.GONE else View.VISIBLE
   settingsToggle.text=if(settings.visibility==View.VISIBLE) "⚙  Çentik ayarları  ▴" else "⚙  Çentik ayarları  ▾"
  }
  val colors=intArrayOf(0xFF8358F5.toInt(),0xFF559EFF.toInt(),0xFF31CC93.toInt(),0xFFFF873A.toInt(),0xFFF75D87.toInt(),0xFF66758C.toInt())
  settings.addView(itemText("Çentik rengi",13f))
  val palette=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
  colors.forEach { color ->
   val swatch=TextView(this).apply {
    text="●"; textSize=26f; gravity=Gravity.CENTER; setTextColor(color)
    setOnClickListener { history.setColor(color); updateTab() }
   }
   palette.addView(swatch,LinearLayout.LayoutParams(0,dp(38),1f))
  }
  settings.addView(palette)
  fun slider(label:String,min:Int,max:Int,current:Int,onValue:(Int)->Unit) {
   val caption=itemText("$label: $current",12f)
   settings.addView(caption)
   val seek=SeekBar(this).apply {
    this.max=max-min
    progress=current-min
    progressTintList=android.content.res.ColorStateList.valueOf(0xFFA68CFF.toInt())
    thumbTintList=android.content.res.ColorStateList.valueOf(0xFFA68CFF.toInt())
    setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
     override fun onProgressChanged(bar:SeekBar?,value:Int,fromUser:Boolean) {
      val v=value+min
      caption.text="$label: $v"
      if(fromUser) { onValue(v); updateTab() }
     }
     override fun onStartTrackingTouch(bar:SeekBar?) {}
     override fun onStopTrackingTouch(bar:SeekBar?) {}
    })
   }
   settings.addView(seek)
  }
  slider("Genişlik (dp)",8,65,history.tabWidth(),history::setTabWidth)
  slider("Yükseklik (dp)",20,160,history.tabHeight(),history::setTabHeight)
  slider("Görünürlük (%)",20,100,history.tabOpacity(),history::setTabOpacity)
  outer.addView(settings)
  val sub=TextView(this).apply { text="KAYITLAR  •  ${history.all().size} (sabitlenenler korunur)"; textSize=12f; setTextColor(0xFF9BAAC3.toInt()); setPadding(0,dp(4),0,dp(12)) }
  outer.addView(sub)
  val scroll=ScrollView(this)
  val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  panelList=list
  panelCounter=sub
  refreshList()
  scroll.addView(list); outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val footer=itemText("Temizle",14f).apply { gravity=Gravity.END; setTextColor(0xFFFF879B.toInt()); setOnClickListener { history.clear(); refreshList() } }
  outer.addView(footer)
  // A left swipe closes the drawer, including when the gesture starts on a list item.
  // Ordinary taps and vertical scrolling continue to work.
  val backdrop=object:FrameLayout(this) {
   private var downX=0f
   private var downY=0f
   private var swipeDismissed=false
   override fun dispatchTouchEvent(event:MotionEvent):Boolean {
    when(event.actionMasked) {
     MotionEvent.ACTION_DOWN -> {
      downX=event.x; downY=event.y; swipeDismissed=false
     }
     MotionEvent.ACTION_UP -> {
      val dx=event.x-downX
      val dy=event.y-downY
      if(dx < -dp(75) && kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.3f) {
       swipeDismissed=true
       hidePanel()
       return true
      }
     }
    }
    return super.dispatchTouchEvent(event)
   }
  }.apply {
   setBackgroundColor(0x55000000)
   setOnClickListener { hidePanel() }
  }
  outer.isClickable=true
  outer.setOnClickListener { }
  backdrop.addView(outer,FrameLayout.LayoutParams(width,FrameLayout.LayoutParams.MATCH_PARENT,Gravity.START))
  val p=params(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT).apply {
   x=0; y=0
  }
  wm.addView(backdrop,p); panel=backdrop
 }
 private fun itemText(value:String,size:Float)=TextView(this).apply { text=value; textSize=size; setTextColor(Color.WHITE); setPadding(dp(8),dp(10),dp(8),dp(10)) }
 private fun copy(value:String) {
  ignoreOwnCopy=true
  try {
   clipboard.setPrimaryClip(ClipData.newPlainText("ClipDock",value))
   history.add(value)
   refreshList()
   Toast.makeText(this,"Panoya kopyalandı",Toast.LENGTH_SHORT).show()
  } finally {
   handler.postDelayed({ ignoreOwnCopy=false },400)
  }
 }
 private fun editNote(entry:ClipEntry) {
  val editor=EditText(this).apply {
   setText(entry.note)
   hint="Bu kopya için açıklama yaz..."
   setSingleLine(false)
   minLines=3
   maxLines=6
   setPadding(dp(18),dp(12),dp(18),dp(12))
  }
  val dialog=AlertDialog.Builder(this)
   .setTitle("Açıklama")
   .setView(editor)
   .setNegativeButton("Vazgeç",null)
   .setPositiveButton("Kaydet") { _,_ ->
    history.setNote(entry.text,editor.text.toString().trim())
    refreshList()
   }.create()
  dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
  dialog.show()
  dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
 }
 private fun refreshList() {
  val list=panelList ?: return
  list.removeAllViews()
  val entries=history.entries()
  val pinnedCount=entries.count { it.pinned }
  panelCounter?.text="KAYITLAR • ${entries.size}  |  SABİT ${pinnedCount}"
  if(entries.isEmpty()) list.addView(itemText("Henüz kayıt yok.",14f))
  entries.forEach { entry ->
   val value=entry.text
   val row=LinearLayout(this).apply {
    orientation=LinearLayout.VERTICAL
    setPadding(dp(10),dp(8),dp(8),dp(8))
    background=bg(0xFF242E40.toInt(),FloatArray(8){dp(12).toFloat()})
   }
   val top=LinearLayout(this).apply {
    orientation=LinearLayout.HORIZONTAL
    gravity=Gravity.CENTER_VERTICAL
   }
   val preview=TextView(this).apply {
    text=value
    textSize=14f
    setTextColor(Color.WHITE)
    maxLines=2
    ellipsize=android.text.TextUtils.TruncateAt.END
    setOnClickListener { copy(value) }
   }
   top.addView(preview,LinearLayout.LayoutParams(0,dp(52),1f))
   val pinButton=TextView(this).apply {
    text=if(entry.pinned) "★" else "☆"
    textSize=22f
    gravity=Gravity.CENTER
    setTextColor(if(entry.pinned) 0xFFFFC66D.toInt() else 0xFFB9C2D5.toInt())
    contentDescription=if(entry.pinned) "Sabitlemeyi kaldır" else "Sabitle"
    setOnClickListener { history.setPinned(value,!entry.pinned); refreshList() }
   }
   top.addView(pinButton,LinearLayout.LayoutParams(dp(32),dp(42)))
   val copyButton=TextView(this).apply {
    text="⧉"; textSize=18f; gravity=Gravity.CENTER
    setTextColor(0xFFA68CFF.toInt())
    setOnClickListener { copy(value) }
   }
   top.addView(copyButton,LinearLayout.LayoutParams(dp(30),dp(42)))
   if(entry.pinned) {
    // Pinned clips hide destructive actions in an overflow menu.
    val moreButton=TextView(this).apply {
     text="⋮"; textSize=24f; gravity=Gravity.CENTER
     setTextColor(0xFFB9C2D5.toInt())
     contentDescription="Diğer seçenekler"
     setOnClickListener { anchor ->
      PopupMenu(this@DockService,anchor).apply {
       menu.add("Sil").setOnMenuItemClickListener {
        history.remove(value)
        refreshList()
        true
       }
       show()
      }
     }
    }
    top.addView(moreButton,LinearLayout.LayoutParams(dp(28),dp(42)))
   } else {
    val deleteButton=TextView(this).apply {
     text="×"; textSize=24f; gravity=Gravity.CENTER
     setTextColor(0xFFFF879B.toInt())
     contentDescription="Sil"
     setOnClickListener { history.remove(value); refreshList() }
    }
    top.addView(deleteButton,LinearLayout.LayoutParams(dp(28),dp(42)))
   }
   row.addView(top)
   val noteButton=TextView(this).apply {
    text=if(entry.note.isBlank()) "✎  Açıklama ekle" else "✎  ${entry.note}"
    textSize=12f
    maxLines=2
    ellipsize=android.text.TextUtils.TruncateAt.END
    setTextColor(0xFFBCA8FF.toInt())
    setPadding(dp(4),dp(3),dp(4),dp(4))
    setOnClickListener { editNote(entry) }
   }
   row.addView(noteButton)
   list.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
  }
 }
 private fun hidePanel() {
  panel?.let { wm.removeView(it) }
  panel=null
  panelList=null
  panelCounter=null
 }
 override fun onDestroy() {
  clipboard.removePrimaryClipChangedListener(listener); unregisterReceiver(colorReceiver); unregisterReceiver(openReceiver)
  hidePanel(); hideTab(); super.onDestroy()
 }
}
