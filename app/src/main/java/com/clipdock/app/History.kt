package com.clipdock.app
import android.content.Context
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArrayList

class History(private val context: Context) {
 private val prefs = context.getSharedPreferences("clipdock", Context.MODE_PRIVATE)
 fun all(): List<String> {
  val array = try { JSONArray(prefs.getString("history", "[]")) } catch (_: Exception) { JSONArray() }
  return (0 until array.length()).mapNotNull { array.optString(it).takeIf { s -> s.isNotBlank() } }
 }
 fun add(value: String) {
  val s = value.trim().take(10000)
  if(s.isBlank()) return
  val list = (listOf(s) + all().filterNot { it == s }).take(10)
  val array = JSONArray(); list.forEach { array.put(it) }
  prefs.edit().putString("history", array.toString()).apply()
 }
 fun remove(value: String) { val a=JSONArray(); all().filterNot { it==value }.forEach { a.put(it) }; prefs.edit().putString("history",a.toString()).apply() }
 fun clear() = prefs.edit().remove("history").apply()
 fun color() = prefs.getInt("color",0xFF8358F5.toInt())
 fun setColor(c:Int) = prefs.edit().putInt("color",c).apply()
}
