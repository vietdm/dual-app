package com.dualprofile.app
import android.content.*
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dualprofile.app.crossprofile.*
import com.dualprofile.app.diagnostics.Diagnostics
import com.dualprofile.app.installer.ApkTransfer
import com.dualprofile.app.ui.MainViewModel
import com.dualprofile.app.data.DPLog
import kotlinx.coroutines.*
import org.json.*

class MainActivity: ComponentActivity() {
 private val vm: MainViewModel by viewModels()
 private var nonce by mutableStateOf<String?>(null)
 private var skipResumeInvalidation=false
 private var pendingCommand: String?=null
 private var preparing by mutableStateOf(false)
 private var attemptedSync=false
 private val profileEvents=object: BroadcastReceiver() {
  override fun onReceive(context: Context,intent: Intent) { attemptedSync=false; vm.invalidateWork() }
 }

 private val provisioning=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  vm.message(if(result.resultCode==RESULT_CANCELED) "Setup was canceled or did not finish. Refresh to check Android's actual profile state." else "Android setup returned. Verifying Work Profile owner…")
  attemptedSync=false; vm.invalidateWork()
 }
 private val work=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  skipResumeInvalidation=true
  try {
   if(result.resultCode==RESULT_OK && result.data!=null && nonce!=null) {
    vm.workResponse(Protocol.readResponse(this,result.data!!,nonce!!))
    if(pendingCommand=="install" || pendingCommand=="store") { attemptedSync=false; vm.invalidateWork() }
   } else vm.message("Work operation was canceled or could not be verified. Open Dual Profile using its Work badge and retry.")
  } catch(e: Exception) { DPLog.failure("CrossProfile",e); vm.message("Could not authenticate Work response.") }
  nonce=null; pendingCommand=null
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  nonce=savedInstanceState?.getString("nonce"); pendingCommand=savedInstanceState?.getString("command")
  androidx.core.content.ContextCompat.registerReceiver(this,profileEvents,IntentFilter().apply {
   addAction(Intent.ACTION_MANAGED_PROFILE_ADDED); addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
   addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE); addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
   addAction("android.app.action.MANAGED_PROFILE_PROVISIONED"); addAction("android.app.action.PROFILE_OWNER_CHANGED")
  },androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
  setContent {
   val state by vm.state.collectAsStateWithLifecycle()
   val prefs=remember { getSharedPreferences("ui",MODE_PRIVATE) }
   var theme by remember { mutableStateOf(prefs.getString("theme","System")!!) }
   var diagnostics by remember { mutableStateOf(false) }
   var query by remember { mutableStateOf("") }
   val dark=when(theme) { "Dark" -> true; "Light" -> false; else -> isSystemInDarkTheme() }
   MaterialTheme(colorScheme=if(dark) darkColorScheme() else lightColorScheme()) {
    Surface(Modifier.fillMaxSize()) {
     val p=state.profile
     LaunchedEffect(p?.work,state.scanning,state.apps.size,state.ownerVerified,nonce) {
      if(p?.work!=null && !p.inWork && !p.quiet && !state.scanning && !state.ownerVerified && !attemptedSync && nonce==null) { attemptedSync=true; sync() }
     }
     LazyColumn(Modifier.safeDrawingPadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(vertical=20.dp)) {
      item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Dual Profile",style=MaterialTheme.typography.headlineMedium); TextButton(onClick={diagnostics=!diagnostics}) { Text(if(diagnostics) "Home" else "Diagnostics") } } }
      item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("System","Light","Dark").forEach { t -> FilterChip(selected=theme==t,onClick={theme=t; prefs.edit().putString("theme",t).apply()},label={Text(t)}) } } }
      if(diagnostics) {
       item { Text(Diagnostics.describe(this@MainActivity,p ?: vm.profiles.snapshot(),state.ownerVerified,nonce==null && state.ownerVerified),style=MaterialTheme.typography.bodySmall) }
       if(state.workDiagnostics!=null) item { Text("Live Work companion diagnostics\n${state.workDiagnostics}",style=MaterialTheme.typography.bodySmall) }
       if(BuildConfig.DEBUG && p?.inWork==false) item {
        OutlinedButton(enabled=!preparing && nonce==null,onClick={ lifecycleScope.launch {
         preparing=true
         try {
          val probe=vm.state.value.apps.firstOrNull { it.facts.packageName=="com.dualprofile.probe" } ?: error("Install the supplied Probe APK in Personal first.")
          val transfer=withContext(Dispatchers.IO) { ApkTransfer.snapshot(this@MainActivity,probe) }
          send("install",transfer.first,transfer.second)
         } catch(e: Exception) { vm.message(e.message ?: "Probe preparation failed.") } finally { preparing=false }
        } }) { Text("Test Probe installation (development)") }
        Text("This technical spike may be refused by Work installation policy. It does not bypass policy or establish eligibility.",style=MaterialTheme.typography.bodySmall)
       }
       item { Text("Application eligibility",style=MaterialTheme.typography.titleLarge) }
       items(state.apps,key={it.facts.packageName}) { app -> Text("${app.facts.label}\n${app.facts.packageName}\n${app.eligibility} · ${app.installMethod}",style=MaterialTheme.typography.bodySmall) }
      } else {
       item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Work Profile",style=MaterialTheme.typography.titleLarge)
        when {
         p==null -> Text("Checking Android profile state…")
         p.inWork -> { Text(if(p.owner) "● Profile Owner verified" else "This Work Profile is managed by another application."); Text("Use Dual Profile in Personal to create a second instance. Applications here hold Account B independently.") }
         !p.supported -> Text("Work Profile is not supported on this device.")
         p.work==null -> { Text("Second profile is not configured."); Text("Dual Profile uses Android Work Profile to create an isolated second application instance."); Button(enabled=p.provisioningAllowed,onClick={runCatching { provisioning.launch(vm.profiles.provisioningIntent()) }.onFailure { vm.message(it.message ?: "Provisioning unavailable.") }}) { Text("Create Work Profile") }; if(!p.provisioningAllowed) Text("Android policy does not allow provisioning. An existing managed profile or device restriction may prevent setup.") }
         p.quiet -> { Text("○ Paused"); Text("Apps in the second profile cannot receive notifications while the profile is paused."); Button(onClick={ if(!vm.profiles.resume()) settings(); vm.refresh() }) { Text("Resume") } }
         state.ownerVerified -> { Text("● Active · Owner verified"); Text("Personal = Account A · Work = Account B"); OutlinedButton(onClick={sync()}) { Text("Refresh Work apps") } }
         else -> { Text("Work Profile detected · Management unverified"); Text("If another application manages this profile, Dual Profile cannot create or take over another Work Profile. Open the Work companion to inspect setup."); OutlinedButton(onClick={attemptedSync=true; sync()}) { Text("Verify Work companion") } }
        }
       } } }
       if(state.message!=null) item { Card { Column(Modifier.padding(14.dp)) { Text(state.message!!); TextButton(onClick={vm.message(null)}) { Text("Dismiss") } } } }
       item { OutlinedTextField(value=query,onValueChange={query=it},label={Text("Search applications…")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
       item { Text("Apps available for Dual Profile",style=MaterialTheme.typography.titleLarge); if(state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth()) }
       val apps=state.apps.filter { it.facts.packageName!=packageName && (it.eligibility==Eligibility.ELIGIBLE || it.eligibility==Eligibility.ALREADY_DUAL) && (it.facts.label.contains(query,true) || it.facts.packageName.contains(query,true)) }
       if(apps.isEmpty()) item { Text(if(p?.work==null) "Create Work Profile to check compatible applications." else "No verified compatible apps yet. Refresh Work apps after setup. Diagnostics lists excluded apps.") }
       items(apps,key={it.facts.packageName}) { app ->
        val icon=remember(app.facts.packageName,app.versionCode) { vm.repository.icon(app.facts.packageName)?.toBitmap(96,96)?.asImageBitmap() }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
         Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { if(icon!=null) Image(icon,contentDescription=null,modifier=Modifier.size(44.dp)); Column { Text(app.facts.label,style=MaterialTheme.typography.titleMedium); Text(app.facts.packageName,style=MaterialTheme.typography.bodySmall) } }
         if(state.operations[app.facts.packageName]==InstallState.FAILED && !app.workInstalled) Text("Failed · check installation policy or available Work store",color=MaterialTheme.colorScheme.error)
         Text("Personal: Installed     Work: ${if(p?.quiet==true) "Paused" else if(app.workInstalled) "Installed" else "Not setup"}")
         Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
          OutlinedButton(onClick={runCatching { startActivity(packageManager.getLaunchIntentForPackage(app.facts.packageName) ?: error("No launcher")) }.onFailure { vm.message("Personal application cannot be opened.") }}) { Text("Open A") }
          if(app.workInstalled) Button(enabled=p?.quiet!=true,onClick={send("open",JSONObject().put("package",app.facts.packageName).put("storeSource",app.facts.storeSource ?: ""))}) { Text("Open B") }
          else Button(enabled=p?.quiet!=true && nonce==null && !preparing,onClick={
           if(app.installMethod==InstallMethod.STORE) send("store",JSONObject().put("package",app.facts.packageName).put("storeSource",app.facts.storeSource ?: "")) else lifecycleScope.launch { vm.operation(app.facts.packageName,InstallState.CREATING); preparing=true; vm.message("Preparing base APK and splits…"); try { val transfer=withContext(Dispatchers.IO) { ApkTransfer.snapshot(this@MainActivity,app) }; send("install",transfer.first,transfer.second) } catch(e: Exception) { DPLog.failure("Install",e); vm.message("APK files could not be read. Refresh or use an available Work app store."); if(app.facts.storeVerified) send("store",JSONObject().put("package",app.facts.packageName).put("storeSource",app.facts.storeSource ?: "")) } finally { preparing=false } }
          }) { Text("Create dual app") }
         }
        } }
       }
       item { Text("Android isolates application data and notifications. Pausing Work stops Account B. Dual Profile does not read your messages or credentials.",style=MaterialTheme.typography.bodySmall); TextButton(onClick={settings()}) { Text("Open Android profile settings") } }
      }
     }
    }
   }
  }
 }
 override fun onDestroy() { unregisterReceiver(profileEvents); super.onDestroy() }
 override fun onResume() { super.onResume(); if(skipResumeInvalidation) { skipResumeInvalidation=false; vm.refresh() } else { attemptedSync=false; vm.invalidateWork() } }
 override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("nonce",nonce); outState.putString("command",pendingCommand) }
 private fun sync() {
  val packages=JSONArray(vm.state.value.apps.filter { it.facts.launcher && !it.facts.critical && it.facts.packageName!=packageName }.map { it.facts.packageName })
  send("sync",JSONObject().put("packages",packages))
 }
 private fun send(command: String,body: JSONObject,clip: ClipData?=null) {
  try {
   val intent=Protocol.request(this,command,body)
   if(clip!=null) { intent.clipData=clip; intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
   // Select only the OS forwarder discovered through PackageManager, avoiding a Personal-profile receiver.
   val targets=packageManager.queryIntentActivities(intent,0)
   val forwarder=targets.firstOrNull { it.activityInfo.packageName=="android" && it.activityInfo.exported } ?: error("Work companion unavailable")
   intent.component=ComponentName(forwarder.activityInfo.packageName,forwarder.activityInfo.name)
   nonce=JSONObject(intent.getStringExtra("payload")!!).getString("nonce"); pendingCommand=command
   work.launch(intent)
  } catch(e: Exception) { nonce=null; pendingCommand=null; DPLog.failure("CrossProfile",e); vm.message("Could not reach the Work companion. Check whether another DPC manages this profile or the Work Profile is paused. You can open apps using their Work badge.") }
 }
 private fun settings() { runCatching { startActivity(Intent(Settings.ACTION_SYNC_SETTINGS)) }.onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } } }
}
