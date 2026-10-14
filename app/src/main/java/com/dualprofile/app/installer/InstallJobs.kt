package com.dualprofile.app.installer
import android.content.*
import com.dualprofile.app.data.DPLog
import kotlinx.coroutines.*
import org.json.JSONObject
object InstallJobs {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val running=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
 fun isRunning(nonce: String)=nonce in running
 fun start(context: Context,nonce: String,body: JSONObject,clip: ClipData?) {
  if(!running.add(nonce)) return
  val c=context.applicationContext
  val prefs=c.getSharedPreferences("install",Context.MODE_PRIVATE)
  prefs.edit().putInt("request_$nonce",-3).commit()
  scope.launch {
   try { val id=WorkInstaller.install(c,body,clip,nonce); prefs.edit().putInt("request_$nonce",id).commit() }
   catch(e: Exception) { DPLog.failure("Install",e); prefs.edit().putInt("request_$nonce",-4).putString("request_error_$nonce",e.message).commit() }
   finally { running.remove(nonce) }
  }
 }
}
