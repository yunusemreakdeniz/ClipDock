package com.clipdock.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ClipEntry(val text:String, val pinned:Boolean=false, val note:String="", val hidden:Boolean=false)

class History(private val context:Context) {
 private val prefs=context.getSharedPreferences("clipdock",Context.MODE_PRIVATE)
 private fun read():MutableList<ClipEntry> {
  val array=try { JSONArray(prefs.getString("history","[]")) } catch (_:Exception) { JSONArray() }
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
  prefs.edit().putString("history",array.toString()).apply()
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
