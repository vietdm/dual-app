package com.dualprofile.app.ui
import android.app.Application
import androidx.lifecycle.*
import com.dualprofile.app.*
import com.dualprofile.app.packages.*
import com.dualprofile.app.profile.*
import com.dualprofile.app.data.DPLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class HomeState(val profile: ProfileSnapshot?=null,val apps: List<DualAppCandidate> = emptyList(),val ownerVerified: Boolean=false,val scanning: Boolean=false,val message: String?=null,val workDiagnostics: String?=null,val operations: Map<String,InstallState> = emptyMap())
class MainViewModel(app: Application): AndroidViewModel(app) {
 val repository=AppRepository(app)
 val profiles=ProfileManager(app)
 private val _state=MutableStateFlow(HomeState())
 val state=_state.asStateFlow()
 private var capabilities: JSONObject?=null
 private var refreshGeneration=0
 private var capabilityProfile: android.os.UserHandle?=null
 init { refresh() }
 fun refresh() {
  val generation=++refreshGeneration
  viewModelScope.launch {
   _state.update { it.copy(scanning=true,profile=profiles.snapshot()) }
   try {
    val apps=withContext(Dispatchers.IO) { repository.scan() }
    if(generation!=refreshGeneration) return@launch
    val p=profiles.snapshot()
    if(p.work==null || p.work!=capabilityProfile) capabilities=null
    val evaluated=apps.map { app ->
     val c=capabilities?.optJSONObject(app.facts.packageName)
     val stores=c?.optJSONArray("stores")
     val sameStore=stores!=null && (0 until stores.length()).any { stores.optString(it)==app.facts.storeSource }
     val cap=InstallCapability(c?.optBoolean("installer")==true,c?.optBoolean("store")==true && sameStore,c?.optBoolean("existing")==true,c?.optBoolean("restricted")==true)
     val installed=c?.optBoolean("installed")==true
     app.copy(workInstalled=installed,eligibility=evaluate(app.facts,cap,installed),installMethod=when { cap.existing -> InstallMethod.EXISTING; cap.installer && app.facts.readable -> InstallMethod.PACKAGE_INSTALLER; cap.store && app.facts.storeVerified -> InstallMethod.STORE; else -> InstallMethod.NONE })
    }
    _state.update { it.copy(profile=p,apps=evaluated,scanning=false,ownerVerified=capabilities!=null && p.work!=null) }
   } catch(e: Exception) { DPLog.failure("Packages",e); if(generation!=refreshGeneration) return@launch; _state.update { it.copy(scanning=false,message="Application discovery failed. Refresh to try again.") } }
  }
 }
 fun workResponse(body: JSONObject) { if(body.optString("error").isNotEmpty() && body.optString("package").isNotEmpty()) operation(body.getString("package"),InstallState.FAILED); body.optString("diagnostics").takeIf { it.isNotEmpty() }?.let { details -> _state.update { it.copy(workDiagnostics=details) } }; body.optJSONObject("packages")?.let { capabilities=it; capabilityProfile=profiles.snapshot().work }; message(body.optString("message","Work state refreshed.")); refresh() }
 fun operation(pkg: String,status: InstallState) { _state.update { it.copy(operations=it.operations+(pkg to status)) } }
 fun message(text: String?) { _state.update { it.copy(message=text) } }
 fun invalidateWork() { capabilities=null; _state.update { it.copy(ownerVerified=false,apps=emptyList(),workDiagnostics=null) }; refresh() }
}
