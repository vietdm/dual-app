package com.dualprofile.app.profile
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.*
import android.content.pm.PackageManager
import com.dualprofile.app.admin.ProfileAdminReceiver
import com.dualprofile.app.crossprofile.Protocol
import com.dualprofile.app.data.DPLog

data class ProfileSnapshot(val supported: Boolean, val provisioningAllowed: Boolean, val work: UserHandle?, val inWork: Boolean, val owner: Boolean, val quiet: Boolean)
class ProfileManager(private val context: Context) {
 val dpm = context.getSystemService(DevicePolicyManager::class.java)
 val users = context.getSystemService(UserManager::class.java)
 val admin = ComponentName(context,ProfileAdminReceiver::class.java)
 fun snapshot(): ProfileSnapshot {
  val inWork=if(Build.VERSION.SDK_INT>=30) users.isManagedProfile else dpm.isProfileOwnerApp(context.packageName) && runCatching { dpm.isManagedProfile(admin) }.getOrDefault(false)
  val work=if(inWork) Process.myUserHandle() else users.userProfiles.firstOrNull { handle ->
   handle != Process.myUserHandle() && (Build.VERSION.SDK_INT<35 || runCatching {
    context.getSystemService(android.content.pm.LauncherApps::class.java).getLauncherUserInfo(handle)?.userType == "android.os.usertype.profile.MANAGED"
   }.getOrDefault(false))
  }
  return ProfileSnapshot(context.packageManager.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS),runCatching { dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE) }.getOrDefault(false),work,inWork,dpm.isProfileOwnerApp(context.packageName),work?.let { runCatching { users.isQuietModeEnabled(it) }.getOrDefault(false) } ?: false)
 }
 fun provisioningIntent(): Intent {
  val s=snapshot(); check(s.supported && s.provisioningAllowed && s.work==null) { "Work Profile provisioning is unavailable. Another profile or device policy may prevent setup." }
  val secret=Protocol.newPairingKey(context)
  return Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,admin).putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,PersistableBundle().apply { putString(Protocol.KEY,secret) })
 }
 fun initialize(intent: Intent? = null): Boolean {
  if(!dpm.isProfileOwnerApp(context.packageName) || !snapshot().inWork) return false
  intent?.let { androidx.core.content.IntentCompat.getParcelableExtra(it,DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,PersistableBundle::class.java) }?.getString(Protocol.KEY)?.let { Protocol.saveKey(context,it) }
  try {
   context.packageManager.setComponentEnabledSetting(ComponentName(context,"com.dualprofile.app.crossprofile.WorkBridgeActivity"),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP)
   dpm.clearCrossProfileIntentFilters(admin)
   dpm.addCrossProfileIntentFilter(admin,IntentFilter(Protocol.ACTION).apply { addCategory(Intent.CATEGORY_DEFAULT); addDataType(Protocol.MIME) },DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT)
   dpm.setProfileName(admin,"Dual Profile")
   dpm.setProfileEnabled(admin)
   DPLog.info("DPC","Profile owner verified; companion initialized")
   return true
  } catch(e: RuntimeException) { DPLog.failure("DPC",e); return false }
 }
 fun resume(): Boolean = snapshot().work?.let { runCatching { users.requestQuietModeEnabled(false,it) }.getOrElse { DPLog.failure("DPC",it); false } } ?: false
}
