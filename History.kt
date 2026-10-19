package com.clipdock.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ClipEntry(val text:String, val pinned:Boolean=false, val note:String="", val hidden:Boolean=false)

class History(private val context:Context) {
 private val prefs=context.getSharedPreferences("clipdock",Context.MODE_PRIVATE)
 private fun loadHistory():String {
  val encrypted=prefs.getString("history_encrypted",null)
  if(encrypted!=null) return Crypto.decryptLocal(encrypted)
  val legacy=prefs.getString("history","[]") ?: "[]"
  // Migrate legacy plaintext immediately; do not delete it unless encrypted commit succeeds.
  check(prefs.edit().putString("history_encrypted",Crypto.encryptLocal(legacy))
   .remove("history").commit()) { "Eski kayıtlar şifrelenemedi" }
  return legacy
 }
 private fun read():MutableList<ClipEntry> {
  val array=JSONArray(loadHistory())
  return (0 until array.length()).mapNotNull { i ->
   val obj=array.optJSONObject(i)
   if(obj!=null) {
    val text=obj.optString("text")
    if(text.isBlank()) null else ClipEntry(text,obj.optBoolean("pinned"),obj.optString("note"),obj.optBoolean("hidden"))
   } else array.optString(i).takeIf { it.isNotBlank() }?.let { ClipEntry(it) }
  }.toMutableList()
 }
 private fun save(items:List<ClipEntry>) {
  val array=JSONArray()
  items.forEach { array.put(JSONObject().put("text",it.text).put("pinned",it.pinned).put("note",it.note).put("hidden",it.hidden)) }
  check(prefs.edit().putString("history_encrypted",Crypto.encryptLocal(array.toString()))
   .remove("history").commit()) { "Şifreli kayıt kaydedilemedi" }
 }
 fun exportBackup():String {
  val data=JSONObject()
  data.put("format","clipdock-backup")
  data.put("version",1)
  data.put("history",JSONArray(loadHistory()))
  data.put("color",color())
  data.put("tab_height",tabHeight())
  data.put("tab_width",tabWidth())
  data.put("tab_opacity",tabOpacity())
  return data.toString(2)
 }
 fun importBackup(json:String):Int {
  require(json.length<=5_000_000) { "Yedek dosyası çok büyük" }
  val data=JSONObject(json)
  require(data.optString("format")=="clipdock-backup" && data.optInt("version")==1) { "Geçersiz ClipDock yedeği" }
  val array=data.getJSONArray("history")
  require(array.length()<=10000) { "Çok fazla kayıt" }
  val items=mutableListOf<ClipEntry>()
  for(i in 0 until array.length()) {
   val obj=array.optJSONObject(i) ?: throw IllegalArgumentException("Bozuk kayıt")
   val value=obj.getString("text")
   require(value.isNotBlank() && value.length<=10000) { "Geçersiz kayıt metni" }
   items.add(ClipEntry(value,obj.optBoolean("pinned"),obj.optString("note").take(1000),obj.optBoolean("hidden")))
  }
  // Validate the entire backup before changing stored data.
  val newColor=data.optInt("color",color())
  val newHeight=data.optInt("tab_height",tabHeight()).coerceIn(20,160)
  val newWidth=data.optInt("tab_width",tabWidth()).coerceIn(8,65)
  val newOpacity=data.optInt("tab_opacity",tabOpacity()).coerceIn(20,100)
  val serialized=JSONArray()
  items.distinctBy { it.text }.forEach { serialized.put(JSONObject().put("text",it.text).put("pinned",it.pinned).put("note",it.note).put("hidden",it.hidden)) }
  check(prefs.edit().putString("history_encrypted",Crypto.encryptLocal(serialized.toString())).remove("history")
   .putInt("color",newColor).putInt("tab_height",newHeight)
   .putInt("tab_width",newWidth).putInt("tab_opacity",newOpacity).commit()) { "Yedek kaydedilemedi" }
  return serialized.length()
 }
 fun entries():List<ClipEntry> = read().sortedWith(compareByDescending<ClipEntry> { it.pinned })
 fun all():List<String> = entries().map { it.text }
 fun add(value:String) {
  val text=value.trim().take(10000)
  if(text.isBlank()) return
  val current=read()
  val previous=current.firstOrNull { it.text==text }
  current.removeAll { it.text==text }
  current.add(0,ClipEntry(text,previous?.pinned?:false,previous?.note?:"",previous?.hidden?:false))
  val pinned=current.filter { it.pinned }
  val recent=current.filterNot { it.pinned }.take(10)
  // Keep the existing pinned order, even when a pinned clip is copied again.
  val originalPinned=read().filter { it.pinned }.map { it.text }
  save(pinned.sortedBy { originalPinned.indexOf(it.text).let { n -> if(n<0) Int.MAX_VALUE else n } }+recent)
 }
 fun setPinned(text:String,pinned:Boolean) {
  save(read().map { if(it.text==text) it.copy(pinned=pinned) else it })
 }
 fun movePinned(text:String,direction:Int) {
  val current=read()
  val pinned=current.filter { it.pinned }.toMutableList()
  val from=pinned.indexOfFirst { it.text==text }
  val to=from+direction
  if(from<0 || to !in pinned.indices) return
  val item=pinned.removeAt(from)
  pinned.add(to,item)
  save(pinned+current.filterNot { it.pinned })
 }
 fun setHidden(text:String,hidden:Boolean) {
  save(read().map { if(it.text==text) it.copy(hidden=hidden) else it })
 }
 fun setNote(text:String,note:String) {
  save(read().map { if(it.text==text) it.copy(note=note.take(1000)) else it })
 }
 fun remove(value:String) = save(read().filterNot { it.text==value })
 // Clear only unpinned clips, protecting pinned entries.
 fun clear() = save(read().filter { it.pinned || it.hidden })
 fun color()=prefs.getInt("color",0xFF8358F5.toInt())
 fun setColor(c:Int)=prefs.edit().putInt("color",c).apply()
 fun tabHeight()=prefs.getInt("tab_height",86)
 fun setTabHeight(v:Int)=prefs.edit().putInt("tab_height",v).apply()
 fun tabWidth()=prefs.getInt("tab_width",33)
 fun setTabWidth(v:Int)=prefs.edit().putInt("tab_width",v).apply()
 fun tabOpacity()=prefs.getInt("tab_opacity",90)
 fun setTabOpacity(v:Int)=prefs.edit().putInt("tab_opacity",v).apply()
}
