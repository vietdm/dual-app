package com.dualprofile.app.crossprofile
import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dualprofile.app.installer.*
import com.dualprofile.app.profile.ProfileManager
import com.dualprofile.app.data.DPLog
import kotlinx.coroutines.*
import org.json.*

class WorkBridgeActivity: ComponentActivity() {
 private lateinit var request: JSONObject
 private var message by mutableStateOf("Checking Work Profile…")
 private var sessionId by mutableIntStateOf(-2)
 private var observing=false
 private var lastError: String?=null
 private var installBody: JSONObject?=null
 private var storePackage: String?=null
 private var storeSource: String?=null
 private var busy by mutableStateOf(false)
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Text("Dual Profile · Work",style=MaterialTheme.typography.headlineSmall)
   Text(message)
   if(installBody!=null && sessionId==-2) Button(onClick={ beginInstall() },enabled=!busy) { Text("Install unchanged application") }
   if(installBody!=null && !busy && sessionId!=-2) OutlinedButton(onClick={ if(resetInstall()) beginInstall() }) { Text("Retry installation") }
   if(busy && installBody!=null) OutlinedButton(onClick={ if(resetInstall()) message="Session canceled. You may retry or return." }) { Text("Cancel / recover session") }
   if(storePackage!=null) OutlinedButton(onClick={ openStore(storePackage!!) }) { Text("Open available Work app store") }
   TextButton(onClick={ respond(JSONObject().put("message",if(lastError!=null) message else "Operation closed. Refresh Work state.").put("error",lastError ?: "").put("package",if(::request.isInitialized) request.optJSONObject("body")?.optString("package") ?: "" else "")) }) { Text("Return to Personal") }
  } } } }
  try {
   check(ProfileManager(this).snapshot().run { inWork && owner }) { "Companion must be the Work Profile owner" }
   request=if(savedInstanceState?.getString("request")!=null) JSONObject(savedInstanceState.getString("request")!!) else Protocol.authenticate(this,intent)
   sessionId=getSharedPreferences("install",MODE_PRIVATE).getInt("request_${request.getString("nonce")}",savedInstanceState?.getInt("session",-2) ?: -2)
   val body=request.getJSONObject("body")
   storeSource=body.optString("storeSource").takeIf { it in setOf("com.android.vending","com.sec.android.app.samsungapps") }
   when(request.getString("command")) {
    "sync" -> {
     val packages=body.getJSONArray("packages"); require(packages.length()<=1000)
     val status=JSONObject()
     for(i in 0 until packages.length()) { val pkg=packages.getString(i); require(Protocol.validPackage(pkg)); status.put(pkg,WorkInstaller.capability(this,pkg)) }
     DPLog.info("CrossProfile","Work owner verified; packages=${packages.length()} installer=${WorkInstaller.installerAvailable(this)} affiliated=${WorkInstaller.existingAllowed(this)} policyBlocked=${WorkInstaller.restrictions(this)}")
     respond(JSONObject().put("owner",true).put("packages",status).put("diagnostics",com.dualprofile.app.diagnostics.Diagnostics.describe(this,ProfileManager(this).snapshot(),true,true)).put("message","Work Profile owner verified. App capabilities refreshed."))
    }
    "open" -> { val pkg=body.getString("package"); require(Protocol.validPackage(pkg)); startActivity(packageManager.getLaunchIntentForPackage(pkg) ?: error("No Work launcher for this application. Use its Work badge in the launcher.")); respond(JSONObject().put("message","Work application opened.")) }
    "store" -> { val pkg=body.getString("package"); require(Protocol.validPackage(pkg) && storeSource!=null); storePackage=pkg; openStore(pkg) }
    "install" -> {
     val pkg=body.getString("package"); require(Protocol.validPackage(pkg)); installBody=body; storePackage=if(body.optBoolean("storeVerified") && WorkInstaller.stores(this,pkg).isNotEmpty()) pkg else null
     message="Install $pkg in Work Profile? The original base APK and all installed splits keep their package, signature and version. Android may require confirmation."
     if(sessionId!=-2) { busy=true; observeSession() }
    }
    else -> error("Unsupported command")
   }
  } catch(e: Exception) { fail(e) }
 }
 override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); if(::request.isInitialized) outState.putString("request",request.toString()); outState.putInt("session",sessionId) }
 private fun beginInstall() {
  busy=true; lastError=null; message="Preparing verified APKs…"; sessionId=-3
  InstallJobs.start(applicationContext,request.getString("nonce"),installBody!!,intent.clipData)
  observeSession()
 }
 private fun resetInstall(): Boolean {
  if(InstallJobs.isRunning(request.getString("nonce"))) { message="Preparation is still running. Wait for Android before retrying."; return false }
  if(sessionId>=0) runCatching { packageManager.packageInstaller.abandonSession(sessionId) }
  getSharedPreferences("install",MODE_PRIVATE).edit().remove("request_${request.getString("nonce")}").remove("confirm_$sessionId").commit()
  sessionId=-2; busy=false; return true
 }
 private fun observeSession() {
  if(observing) return
  observing=true
  lifecycleScope.launch {
   try {
    val prefs=getSharedPreferences("install",MODE_PRIVATE)
    val start=System.currentTimeMillis()
    while(!isFinishing && busy) {
     sessionId=prefs.getInt("request_${request.getString("nonce")}",sessionId)
     if(sessionId==-1) { respond(JSONObject().put("message","Work instance verified.").put("installed",true)); break }
     if(sessionId==-4 || (sessionId==-3 && !InstallJobs.isRunning(request.getString("nonce")))) { fail(IllegalStateException(prefs.getString("request_error_${request.getString("nonce")}","Preparation was interrupted"))); busy=false; break }
     if(sessionId>=0 && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
      PendingConfirmations.take(this@WorkBridgeActivity,sessionId)?.let { confirm -> message="Installation requires confirmation."; try { startActivity(confirm) } catch(e: RuntimeException) { fail(e); busy=false } }
      when(prefs.getString("state_$sessionId","")) {
       "INSTALLED" -> { respond(JSONObject().put("message","Installed and verified in Work Profile.").put("installed",true)); break }
       "FAILED" -> { fail(IllegalStateException(prefs.getString("error_$sessionId",""))); busy=false; break }
      }
      if(packageManager.packageInstaller.getSessionInfo(sessionId)==null && prefs.getString("state_$sessionId","")!="INSTALLED") { message="Android no longer has this installation session. Retry or use an available Work store."; busy=false; break }
     }
     if(System.currentTimeMillis()-start>600000) { message="Installation has not completed. Use Cancel / recover session or an available Work store."; busy=false; break }
     delay(400)
    }
   } finally { observing=false }
  }
 }
 private fun openStore(pkg: String) {
  for(store in WorkInstaller.stores(this,pkg).sortedBy { if(it.`package`==storeSource) 0 else 1 }) try { startActivity(store); message="Store opened inside Work Profile. Return after installation and refresh."; return } catch(e: RuntimeException) { DPLog.failure("Install",e) }
  message="No available app store can be opened in Work Profile. This is not a fatal profile error."
 }
 private fun respond(body: JSONObject) { if(::request.isInitialized) runCatching { setResult(RESULT_OK,Protocol.response(this,request,body)) }; finish() }
 private fun fail(e: Exception) { lastError=e.message ?: "FAILED"; DPLog.failure("CrossProfile",e); message=when(e.message) { "SECURITY_POLICY_BLOCKED" -> "Installation was blocked by the device security policy. Dual Profile cannot change this security setting automatically. Use an available Work app store."; "UNKNOWN_SOURCES_NOT_ALLOWED" -> "Android does not permit this installation source in Work. Dual Profile cannot change this security setting automatically. Use an available Work app store."; "VERSION_OR_SIGNATURE_CONFLICT" -> "Android rejected a version or signature conflict. Refresh the Personal application and retry; the original APK will not be modified."; "INSTALLATION_CANCELED" -> "Installation was canceled. You can retry when ready."; "INCOMPATIBLE_APK_SET" -> "This APK/split set is incompatible with the device ABI or Android version."; "INSUFFICIENT_STORAGE" -> "Android needs more free storage to install this application."; "INVALID_APK" -> "Android rejected the APK set. Refresh the source application and retry."; "PACKAGE_INSTALLER_UNAVAILABLE" -> "PackageInstaller is unavailable. Use an available Work app store."; else -> "This operation could not be completed. Check Work Profile policy, pairing and application availability." }; if(!::request.isInitialized) { setResult(RESULT_CANCELED) } }
}
