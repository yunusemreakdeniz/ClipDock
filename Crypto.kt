package com.clipdock.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
 private const val ALIAS="clipdock-local-v1"
 private val random=SecureRandom()
 private fun localKey():SecretKey {
  val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (store.getKey(ALIAS,null) as? SecretKey)?.let { return it }
  val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
  generator.init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
   .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
   .setKeySize(256).build())
  return generator.generateKey()
 }
 private fun b64(data:ByteArray)=Base64.encodeToString(data,Base64.NO_WRAP)
 private fun decode(value:String)=Base64.decode(value,Base64.NO_WRAP)
 private fun encrypt(key:SecretKey,plain:String):Pair<String,String> {
  val nonce=ByteArray(12).also(random::nextBytes)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,key,GCMParameterSpec(128,nonce))
  return b64(nonce) to b64(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
 }
 private fun decrypt(key:SecretKey,nonce:String,data:String):String {
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,decode(nonce)))
  return String(cipher.doFinal(decode(data)),Charsets.UTF_8)
 }
 fun encryptLocal(plain:String):String {
  val (nonce,data)=encrypt(localKey(),plain)
  return JSONObject().put("v",1).put("nonce",nonce).put("data",data).toString()
 }
 fun decryptLocal(blob:String):String {
  val json=JSONObject(blob)
  require(json.getInt("v")==1)
  return decrypt(localKey(),json.getString("nonce"),json.getString("data"))
 }
 private fun derive(password:CharArray,salt:ByteArray,iterations:Int):SecretKey {
  val spec=PBEKeySpec(password,salt,iterations,256)
  return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") }
  finally { spec.clearPassword() }
 }
 fun encryptBackup(plain:String,password:CharArray):String {
  require(password.size>=10) { "Parola en az 10 karakter olmalı" }
  val salt=ByteArray(16).also(random::nextBytes)
  val iterations=310000
  val (nonce,data)=encrypt(derive(password,salt,iterations),plain)
  return JSONObject().put("format","clipdock-encrypted").put("version",1)
   .put("kdf","PBKDF2-HMAC-SHA256").put("iterations",iterations)
   .put("salt",b64(salt)).put("nonce",nonce).put("data",data).toString()
 }
 fun decryptBackup(blob:String,password:CharArray):String {
  require(blob.length<=8_000_000) { "Dosya çok büyük" }
  val json=JSONObject(blob)
  require(json.getString("format")=="clipdock-encrypted" && json.getInt("version")==1) { "Şifreli ClipDock yedeği değil" }
  val iterations=json.getInt("iterations")
  require(iterations in 200000..1000000) { "Geçersiz anahtar türetme parametresi" }
  val salt=decode(json.getString("salt"))
  require(salt.size==16) { "Geçersiz salt" }
  return decrypt(derive(password,salt,iterations),json.getString("nonce"),json.getString("data"))
 }
}
