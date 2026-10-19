package com.clipdock.app

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Rect
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.net.Uri
import android.util.Patterns
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
 private val revealedClips=mutableSetOf<String>()
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
  try { showTab() } catch(e:Exception) {
   android.util.Log.e("ClipDock","Notch olusturulamadi",e)
   Toast.makeText(this,"ClipDock çentiği başlatılamadı: ${e.message}",Toast.LENGTH_LONG).show()
  }
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
  // A transparent 56dp touch target surrounds a narrower visible notch.
  // The overlay remains flush with the physical left edge.
  val visible=TextView(this).apply {
   text=""; gravity=Gravity.CENTER
   background=bg(history.color(),floatArrayOf(0f,0f,dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f))
   alpha=history.tabOpacity()/100f
  }
  val view=FrameLayout(this).apply {
   addView(visible,FrameLayout.LayoutParams(dp(history.tabWidth()),FrameLayout.LayoutParams.MATCH_PARENT,Gravity.START))
   isClickable=true
  }
  val p=params(dp(maxOf(history.tabWidth(),56)),dp(history.tabHeight())).apply {
   val maxY=(resources.displayMetrics.heightPixels-height-dp(24)).coerceAtLeast(dp(8))
   y=getSharedPreferences("clipdock_ui",MODE_PRIVATE).getInt("notch_y",y).coerceIn(dp(8),maxY)
   x=0
  }
  // Avoid gesture-exclusion callbacks on an application overlay: OEM-specific
  // WindowManager implementations can reject them.
  // The overlay owns only its narrow notch. Avoid requiring a gesture to travel
  // beyond the overlay's bounds: Android may hand that motion to system navigation.
  val longPressMs=ViewConfiguration.getLongPressTimeout().toLong()
  var downX=0f
  var downY=0f
  var lastY=0f
  var dragging=false
  var moved=false
  var opened=false
  var longPress:Runnable?=null
  fun cancelHold() {
   longPress?.let { handler.removeCallbacks(it) }
   longPress=null
  }
  fun openDrawer() {
   if(opened) return
   opened=true
   cancelHold()
   try {
    showPanel(false)
    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
   } catch(e:Exception) {
    android.util.Log.e("ClipDock","Panel acilamadi",e)
    Toast.makeText(this@DockService,"Panel acilamadi: ${e.message}",Toast.LENGTH_LONG).show()
   }
  }
  view.setOnTouchListener { _,event ->
   when(event.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
     downX=event.rawX
     downY=event.rawY
     lastY=event.rawY
     dragging=false
     moved=false
     opened=false
     cancelHold()
     val hold=Runnable {
      if(!opened && !moved) {
       dragging=true
       view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      }
     }
     longPress=hold
     handler.postDelayed(hold,longPressMs)
     true
    }
    MotionEvent.ACTION_MOVE -> {
     val dx=event.rawX-downX
     val dy=event.rawY-downY
     if(dragging) {
      val maxY=(resources.displayMetrics.heightPixels-p.height-dp(24)).coerceAtLeast(dp(8))
      p.y=(p.y+event.rawY-lastY).toInt().coerceIn(dp(8),maxY)
      lastY=event.rawY
      try { wm.updateViewLayout(view,p) } catch(e:Exception) {
       android.util.Log.e("ClipDock","Notch move failed",e)
      }
     } else if(dx>dp(14) && dx>kotlin.math.abs(dy)*1.1f) {
      openDrawer()
     } else if(kotlin.math.abs(dx)>dp(18) || kotlin.math.abs(dy)>dp(18)) {
      moved=true
      cancelHold()
     }
     true
    }
    MotionEvent.ACTION_UP -> {
     if(dragging) {
      getSharedPreferences("clipdock_ui",MODE_PRIVATE).edit().putInt("notch_y",p.y).apply()
     } else if(!opened) {
      val dx=event.rawX-downX
      val dy=event.rawY-downY
      if(dx>dp(14) && dx>kotlin.math.abs(dy)*1.1f) openDrawer()
      else if(!moved && kotlin.math.abs(dx)<dp(12) && kotlin.math.abs(dy)<dp(12)) openDrawer()
     }
     cancelHold()
     dragging=false
     true
    }
    MotionEvent.ACTION_CANCEL -> {
     cancelHold()
     dragging=false
     true
    }
    else -> true
   }
  }
  wm.addView(view,p); tab=view; tabParams=p
 }
 private fun updateTab() {
  val view=tab ?: return
  val lp=tabParams ?: return
  lp.width=dp(maxOf(history.tabWidth(),56))
  lp.height=dp(history.tabHeight())
  val visible=(view as FrameLayout).getChildAt(0)
  visible.layoutParams=FrameLayout.LayoutParams(dp(history.tabWidth()),FrameLayout.LayoutParams.MATCH_PARENT,Gravity.START)
  visible.background=bg(history.color(),floatArrayOf(0f,0f,dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),dp(18).toFloat(),0f,0f))
  visible.alpha=history.tabOpacity()/100f
  wm.updateViewLayout(view,lp)
 }
 private fun hideTab() { tab?.let { try { wm.removeView(it) } catch(_:Exception) {} }; tab=null }
 private fun capture() {
  try {
   val item=clipboard.primaryClip?.takeIf { it.itemCount>0 }?.getItemAt(0) ?: return
   val value=item.coerceToText(this)?.toString() ?: return
   history.add(value)
   if(panel!=null) refreshList()
  } catch (_:Exception) { /* OS may deny clipboard access in background */ }
 }
 private fun showPanel(readClipboard:Boolean=true) {
  if(panel!=null) {
   if(panel?.isAttachedToWindow==true) return
   panel=null
  }
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
  // Swipe left on the header to dismiss; list gestures remain independent.
  var headerDownX=0f
  var headerDownY=0f
  header.setOnTouchListener { _,event ->
   when(event.actionMasked) {
    MotionEvent.ACTION_DOWN -> { headerDownX=event.rawX; headerDownY=event.rawY; true }
    MotionEvent.ACTION_UP -> {
     val dx=event.rawX-headerDownX
     val dy=event.rawY-headerDownY
     if(dx < -dp(60) && kotlin.math.abs(dx)>kotlin.math.abs(dy)*1.3f) hidePanel()
     true
    }
    else -> true
   }
  }
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
  val sub=TextView(this).apply { text="KAYITLAR  •  Yükleniyor"; textSize=12f; setTextColor(0xFF9BAAC3.toInt()); setPadding(0,dp(4),0,dp(12)) }
  outer.addView(sub)
  // Horizontal right swipe deletes unpinned entries; vertical gestures scroll normally.
  // A pinned entry is never deleted by a gesture.
  val scroll=object:ScrollView(this) {
   private var downX=0f
   private var downY=0f
   private var target:String?=null
   private var horizontal=false
   override fun onInterceptTouchEvent(event:MotionEvent):Boolean {
    when(event.actionMasked) {
     MotionEvent.ACTION_DOWN -> {
      downX=event.x; downY=event.y; horizontal=false
      val listView=panelList
      val contentY=(event.y+scrollY).toInt()
      target=null
      if(listView!=null) {
       val i=(0 until listView.childCount).firstOrNull { n ->
        val child=listView.getChildAt(n)
        contentY>=child.top && contentY<child.bottom
       }
       target=i?.let { history.entries().getOrNull(it) }?.takeUnless { it.pinned || it.hidden }?.text
      }
     }
     MotionEvent.ACTION_MOVE -> {
      val dx=event.x-downX
      val dy=event.y-downY
      if(target!=null && dx>dp(20) && dx>kotlin.math.abs(dy)*1.5f) {
       horizontal=true
       return true
      }
     }
    }
    return super.onInterceptTouchEvent(event)
   }
   override fun onTouchEvent(event:MotionEvent):Boolean {
    if(horizontal) {
     if(event.actionMasked==MotionEvent.ACTION_UP) {
      val dx=event.x-downX
      val dy=event.y-downY
      val value=target
      if(value!=null && dx>dp(100) && dx>kotlin.math.abs(dy)*1.5f &&
       history.entries().any { it.text==value && !it.pinned && !it.hidden }) {
       history.remove(value)
       refreshList()
       Toast.makeText(this@DockService,"Kayıt silindi",Toast.LENGTH_SHORT).show()
      }
      horizontal=false; target=null
     } else if(event.actionMasked==MotionEvent.ACTION_CANCEL) {
      horizontal=false; target=null
     }
     return true
    }
    return super.onTouchEvent(event)
   }
  }
  val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
  panelList=list
  panelCounter=sub
  refreshList()
  scroll.addView(list); outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val footer=itemText("Temizle",14f).apply { gravity=Gravity.END; setTextColor(0xFFFF879B.toInt()); setOnClickListener { history.clear(); refreshList() } }
  outer.addView(footer)
  // Tapping outside closes the drawer; gestures inside the list are reserved for items.
  val backdrop=FrameLayout(this).apply {
   setBackgroundColor(0x55000000)
   setOnClickListener { hidePanel() }
  }
  outer.isClickable=true
  outer.setOnClickListener { }
  backdrop.addView(outer,FrameLayout.LayoutParams(width,FrameLayout.LayoutParams.MATCH_PARENT,Gravity.START))
  val p=params(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT).apply {
   x=0; y=0
  }
  try {
   wm.addView(backdrop,p)
   panel=backdrop
  } catch(e:Exception) {
   panelList=null
   panelCounter=null
   throw e
  }
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
 private fun smartAction(value:String):Pair<String,Intent>? {
  val trimmed=value.trim()
  if(Patterns.WEB_URL.matcher(trimmed).matches() &&
   (trimmed.startsWith("https://",true) || trimmed.startsWith("http://",true) || trimmed.startsWith("www.",true))) {
   val url=if(trimmed.startsWith("www.",true)) "https://$trimmed" else trimmed
   return "Bağlantıyı aç" to Intent(Intent.ACTION_VIEW,Uri.parse(url))
  }
  val digits=trimmed.replace(Regex("[\\s()+-]"),"")
  if(digits.length in 10..15 && digits.all { it.isDigit() }) {
   return "Numarayı ara" to Intent(Intent.ACTION_DIAL,Uri.parse("tel:$digits"))
  }
  return null
 }
 private fun normalizedIban(value:String):String? {
  val iban=value.uppercase(java.util.Locale.ROOT).replace(Regex("\\s"),"")
  return if(iban.matches(Regex("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}")) &&
   (iban.take(2)!="TR" || iban.length==26)) iban else null
 }
 private fun launchSafe(intent:Intent) {
  try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent) }
  catch (_:Exception) { Toast.makeText(this,"Bu işlem için uygun uygulama bulunamadı",Toast.LENGTH_SHORT).show() }
 }
 private fun refreshList() {
  val list=panelList ?: return
  list.removeAllViews()
  val entries=try { history.entries() } catch(e:Exception) {
   android.util.Log.e("ClipDock","Şifreli geçmiş okunamadı",e)
   list.addView(itemText("Kayıtlar okunamadı. Uygulamadan şifreli yedeği geri yükleyin.",14f))
   return
  }
  val pinnedCount=entries.count { it.pinned }
  panelCounter?.text="KAYITLAR • ${entries.size}  |  SABİT ${pinnedCount}"
  if(entries.isEmpty()) list.addView(itemText("Henüz kayıt yok.",14f))
  entries.forEach { entry ->
   val value=entry.text
   val concealed=entry.hidden && !revealedClips.contains(value)
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
    text=if(concealed) "••••••••  Gizli kayıt" else value
    textSize=14f
    setTextColor(Color.WHITE)
    maxLines=2
    ellipsize=android.text.TextUtils.TruncateAt.END
    setOnClickListener {
     if(concealed) Toast.makeText(this@DockService,"Önce göz simgesiyle göster",Toast.LENGTH_SHORT).show()
     else copy(value)
    }
   }
   top.addView(preview,LinearLayout.LayoutParams(0,dp(52),1f))
   if(!concealed) smartAction(value)?.let { (label,intent) ->
    val action=TextView(this).apply {
     text=if(label=="Bağlantıyı aç") "↗" else "☎"
     textSize=19f
     gravity=Gravity.CENTER
     setTextColor(0xFF77DCC1.toInt())
     contentDescription=label
     setOnClickListener { launchSafe(intent) }
    }
    top.addView(action,LinearLayout.LayoutParams(dp(28),dp(42)))
   }
   if(!concealed) normalizedIban(value)?.let { iban ->
    val action=TextView(this).apply {
     text="₺"; textSize=17f; gravity=Gravity.CENTER
     setTextColor(0xFF77DCC1.toInt())
     contentDescription="IBAN boşluksuz kopyala"
     setOnClickListener { copy(iban) }
    }
    top.addView(action,LinearLayout.LayoutParams(dp(28),dp(42)))
   }
   if(entry.hidden) {
    val eye=TextView(this).apply {
     text=if(concealed) "◉" else "◌"
     textSize=21f
     gravity=Gravity.CENTER
     setTextColor(0xFF77DCC1.toInt())
     contentDescription=if(concealed) "Gizli kaydı göster" else "Gizli kaydı gizle"
     setOnClickListener {
      if(concealed) {
       val requireBio=getSharedPreferences("clipdock_security",MODE_PRIVATE).getBoolean("biometric",false)
       if(requireBio && Build.VERSION.SDK_INT>=28) {
        UnlockActivity.onUnlock={
         revealedClips.add(value)
         refreshList()
        }
        try {
         startActivity(Intent(this@DockService,UnlockActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch(e:Exception) {
         UnlockActivity.onUnlock=null
         Toast.makeText(this@DockService,"Doğrulama açılamadı",Toast.LENGTH_SHORT).show()
        }
       } else {
        revealedClips.add(value)
        refreshList()
       }
      } else {
       revealedClips.remove(value)
       refreshList()
      }
     }
    }
    top.addView(eye,LinearLayout.LayoutParams(dp(30),dp(42)))
   }
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
    setOnClickListener {
     if(concealed) Toast.makeText(this@DockService,"Önce göz simgesiyle göster",Toast.LENGTH_SHORT).show()
     else copy(value)
    }
   }
   top.addView(copyButton,LinearLayout.LayoutParams(dp(30),dp(42)))
   if(entry.pinned || entry.hidden) {
    // Pinned and hidden clips hide destructive actions in an overflow menu.
    val moreButton=TextView(this).apply {
     text="⋮"; textSize=24f; gravity=Gravity.CENTER
     setTextColor(0xFFB9C2D5.toInt())
     contentDescription="Diğer seçenekler"
     setOnClickListener { anchor ->
      PopupMenu(this@DockService,anchor).apply {
       menu.add(if(entry.hidden) "Gizlilikten çıkar" else "Gizli kayıt yap").setOnMenuItemClickListener {
        history.setHidden(value,!entry.hidden)
        revealedClips.remove(value)
        refreshList()
        true
       }
       if(entry.pinned) {
        menu.add("Yukarı taşı").setOnMenuItemClickListener { history.movePinned(value,-1); refreshList(); true }
        menu.add("Aşağı taşı").setOnMenuItemClickListener { history.movePinned(value,1); refreshList(); true }
       }
       if(!concealed) smartAction(value)?.let { (label,intent) ->
        menu.add(label).setOnMenuItemClickListener { launchSafe(intent); true }
       }
       if(!concealed) normalizedIban(value)?.let { iban ->
        menu.add("IBAN (boşluksuz) kopyala").setOnMenuItemClickListener { copy(iban); true }
       }
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
    val hideButton=TextView(this).apply {
     text="◉"; textSize=19f; gravity=Gravity.CENTER
     setTextColor(0xFFB9C2D5.toInt())
     contentDescription="Gizli kayıt yap"
     setOnClickListener { history.setHidden(value,true); revealedClips.remove(value); refreshList() }
    }
    top.addView(hideButton,LinearLayout.LayoutParams(dp(28),dp(42)))
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
    text=if(concealed) "Açıklama gizlendi" else if(entry.note.isBlank()) "✎  Açıklama ekle" else "✎  ${entry.note}"
    textSize=12f
    maxLines=2
    ellipsize=android.text.TextUtils.TruncateAt.END
    setTextColor(0xFFBCA8FF.toInt())
    setPadding(dp(4),dp(3),dp(4),dp(4))
    setOnClickListener {
     if(concealed) Toast.makeText(this@DockService,"Önce göz simgesiyle göster",Toast.LENGTH_SHORT).show()
     else editNote(entry)
    }
   }
   row.addView(noteButton)
   list.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
  }
 }
 private fun hidePanel() {
  panel?.let { try { wm.removeView(it) } catch(_:Exception) {} }
  panel=null
  revealedClips.clear()
  panelList=null
  panelCounter=null
 }
 override fun onDestroy() {
  try { clipboard.removePrimaryClipChangedListener(listener) } catch(_:Exception) {}
  try { unregisterReceiver(colorReceiver) } catch(_:Exception) {}
  try { unregisterReceiver(openReceiver) } catch(_:Exception) {}
  hidePanel(); hideTab(); super.onDestroy()
 }
}
