package com.dualprofile.app.crossprofile
import android.content.*
import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
object Protocol {
 const val ACTION="com.dualprofile.app.WORK_REQUEST"
 const val MIME="application/vnd.dualprofile.request"
 const val KEY="pairing_key"
 private fun prefs(c: Context)=c.getSharedPreferences("pairing",Context.MODE_PRIVATE)
 fun saveKey(c: Context,key: String) { prefs(c).edit().putString(KEY,key).commit() }
 fun newPairingKey(c: Context): String = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) },Base64.NO_WRAP).also { saveKey(c,it) }
 private fun key(c: Context): ByteArray = prefs(c).getString(KEY,null)?.let { Base64.decode(it,Base64.NO_WRAP) } ?: error("Profiles are not paired. Open Dual Profile in Work to inspect setup.")
 fun request(c: Context,command: String,body: JSONObject = JSONObject()): Intent {
  val payload=JSONObject().put("command",command).put("body",body).put("time",System.currentTimeMillis()).put("nonce",UUID.randomUUID().toString()).toString()
  return Intent(ACTION).setType(MIME).putExtra("payload",payload).putExtra("mac",Authentication.sign(key(c),payload))
 }
 fun authenticate(c: Context,intent: Intent): JSONObject {
  require(intent.action==ACTION && intent.type==MIME)
  val raw=intent.getStringExtra("payload") ?: error("Missing request")
  require(raw.length<=200_000 && Authentication.verify(key(c),raw,intent.getStringExtra("mac") ?: "")) { "Unauthenticated request" }
  val p=JSONObject(raw)
  require(Authentication.fresh(p.getLong("time"),System.currentTimeMillis())) { "Expired request" }
  val nonce=p.getString("nonce"); require(nonce.length==36)
  val prefs=prefs(c)
  synchronized(this) {
   val seenJson=JSONObject(prefs.getString("seen_v2","{}")!!)
   val previous=seenJson.keys().asSequence().associateWith { seenJson.getLong(it) }
   val accepted=ReplayWindow.accept(nonce,System.currentTimeMillis(),previous)
   check(prefs.edit().putString("seen_v2",JSONObject(accepted).toString()).commit())

  }
  return p
 }
 fun response(c: Context,request: JSONObject,body: JSONObject): Intent {
  val raw=JSONObject().put("nonce",request.getString("nonce")).put("body",body).toString()
  return Intent().putExtra("payload",raw).putExtra("mac",Authentication.sign(key(c),raw))
 }
 fun readResponse(c: Context,intent: Intent,nonce: String): JSONObject {
  val raw=intent.getStringExtra("payload") ?: error("No Work response")
  require(Authentication.verify(key(c),raw,intent.getStringExtra("mac") ?: ""))
  return JSONObject(raw).also { require(it.getString("nonce")==nonce) }.getJSONObject("body")
 }
 fun validPackage(name: String)=name.length<=200 && Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+").matches(name) && name!="com.dualprofile.app"
}
