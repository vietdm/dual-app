package com.dualprofile.app.installer
import android.app.*
import android.content.*
import android.content.pm.*
import android.net.Uri
import android.os.*
import com.dualprofile.app.*
import com.dualprofile.app.profile.ProfileManager
import com.dualprofile.app.data.DPLog
import org.json.JSONObject
import java.io.File
import java.util.UUID
object WorkInstaller {
 const val RESULT_ACTION="com.dualprofile.app.INSTALL_RESULT"
 fun installed(c: Context,pkg: String): Boolean = runCatching { c.packageManager.getPackageInfo(pkg,0).applicationInfo?.enabled==true }.getOrDefault(false)
 fun existingAllowed(c: Context): Boolean = ProfileManager(c).run { dpm.isProfileOwnerApp(c.packageName) && runCatching { dpm.isAffiliatedUser }.getOrDefault(false) }
 fun restrictions(c: Context): Boolean = c.getSystemService(UserManager::class.java).run { hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS) || hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES) || (Build.VERSION.SDK_INT>=29 && hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)) }
 fun installerAvailable(c: Context): Boolean = !restrictions(c) && c.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://${c.packageName}.files/test.apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),0)!=null
 fun stores(c: Context,pkg: String): List<Intent> = listOf("com.android.vending","com.sec.android.app.samsungapps").mapNotNull { store ->
  val intent=Intent(Intent.ACTION_VIEW,Uri.parse(if(store=="com.android.vending") "market://details?id=$pkg" else "samsungapps://ProductDetail/$pkg")).setPackage(store)
  val info=c.packageManager.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
  if(!c.getSystemService(UserManager::class.java).hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS) && info!=null && info.exported && info.enabled && info.applicationInfo.enabled && (info.permission==null || c.checkSelfPermission(info.permission)==PackageManager.PERMISSION_GRANTED)) intent else null
 }
 fun capability(c: Context,pkg: String): JSONObject = JSONObject().put("installed",installed(c,pkg)).put("installer",installerAvailable(c)).put("existing",existingAllowed(c)).put("store",stores(c,pkg).isNotEmpty()).put("restricted",restrictions(c)).put("stores",org.json.JSONArray(stores(c,pkg).map { it.`package` }))
 fun install(c: Context,body: JSONObject,clip: ClipData?,requestNonce: String): Int {
  val pkg=body.getString("package"); require(com.dualprofile.app.crossprofile.Protocol.validPackage(pkg))
  val profile=ProfileManager(c)
  if(existingAllowed(c)) try { if(profile.dpm.installExistingPackage(profile.admin,pkg) && installed(c,pkg)) return -1 } catch(e: SecurityException) { DPLog.failure("Install",e) }
  val users=c.getSystemService(UserManager::class.java)
  check(!users.hasUserRestriction(UserManager.DISALLOW_INSTALL_APPS)) { InstallError.SECURITY_POLICY_BLOCKED.name }
  check(!users.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES) && (Build.VERSION.SDK_INT<29 || !users.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY))) { InstallError.UNKNOWN_SOURCES_NOT_ALLOWED.name }
  check(installerAvailable(c)) { InstallError.PACKAGE_INSTALLER_UNAVAILABLE.name }
  // A profile owner is an enterprise installation source; Android still owns confirmation/policy decisions.
  val files=body.getJSONArray("files"); require(files.length() in 1..128 && clip?.itemCount==files.length())
  val stage=File(c.cacheDir,"verified_${UUID.randomUUID()}").apply { mkdirs() }
  var sessionId=-1
  try {
   var total=0L
   val apks=(0 until files.length()).map { i ->
    val entry=files.getJSONObject(i); val uri=clip!!.getItemAt(i).uri
    require(uri.scheme=="content" && uri.authority?.substringAfter('@')=="${c.packageName}.files")
    val length=entry.getLong("size"); total+=length; require(length>0 && total<=2_147_483_648L)
    val file=File(stage,if(i==0) "base.apk" else "split_$i.apk")
    c.contentResolver.openInputStream(uri).use { input -> requireNotNull(input); file.outputStream().use { out -> val buffer=ByteArray(65536); var count=0L; while(true) { val n=input.read(buffer); if(n<0) break; count+=n; require(count<=length); out.write(buffer,0,n) }; require(count==length) } }
    require(ApkTransfer.sha(file)==entry.getString("sha")) { "APK integrity check failed" }; file
   }
   val archive=c.packageManager.getPackageArchiveInfo(apks.first().path,PackageManager.GET_SIGNING_CERTIFICATES) ?: error("Invalid base APK")
   require(archive.packageName==pkg && archive.longVersionCode==body.getLong("version")) { "Application version changed; refresh and retry" }
   val pi=c.packageManager.packageInstaller
   val params=PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(pkg); setSize(total); if(Build.VERSION.SDK_INT>=31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED) }
   sessionId=pi.createSession(params)
   check(c.getSharedPreferences("install",Context.MODE_PRIVATE).edit().putInt("request_$requestNonce",sessionId).commit())
   pi.openSession(sessionId).use { session ->
    apks.forEach { file -> session.openWrite(file.name,0,file.length()).use { output -> file.inputStream().use { it.copyTo(output) }; session.fsync(output) } }
    c.getSharedPreferences("install",Context.MODE_PRIVATE).edit().putString("package_$sessionId",pkg).putString("state_$sessionId",InstallState.CREATING.name).commit()
    val callback=Intent(c,InstallResultReceiver::class.java).setAction(RESULT_ACTION).putExtra("session",sessionId)
    val flags=PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT>=31) PendingIntent.FLAG_MUTABLE else 0)
    session.commit(PendingIntent.getBroadcast(c,sessionId,callback,flags).intentSender)
   }
   return sessionId
  } catch(e: Exception) { if(sessionId>=0) runCatching { c.packageManager.packageInstaller.abandonSession(sessionId) }; throw e } finally { stage.deleteRecursively() }
 }
}
class InstallResultReceiver: BroadcastReceiver() {
 override fun onReceive(context: Context,intent: Intent) {
  if(intent.action!=WorkInstaller.RESULT_ACTION) return
  val status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE)
  val id=intent.getIntExtra("session",-1); val prefs=context.getSharedPreferences("install",Context.MODE_PRIVATE)
  val pkg=prefs.getString("package_$id",null) ?: return
  val error=when(status) { PackageInstaller.STATUS_SUCCESS -> InstallError.INSTALL_ALLOWED; PackageInstaller.STATUS_PENDING_USER_ACTION -> InstallError.USER_CONFIRMATION_REQUIRED; PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallError.SECURITY_POLICY_BLOCKED; PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallError.VERSION_OR_SIGNATURE_CONFLICT; PackageInstaller.STATUS_FAILURE_ABORTED -> InstallError.INSTALLATION_CANCELED; PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallError.INCOMPATIBLE_APK_SET; PackageInstaller.STATUS_FAILURE_INVALID -> InstallError.INVALID_APK; PackageInstaller.STATUS_FAILURE_STORAGE -> InstallError.INSUFFICIENT_STORAGE; else -> InstallError.NO_VALID_INSTALL_METHOD }
  prefs.edit().putString("state_$id",mapInstallState(status,WorkInstaller.installed(context,pkg)).name).putString("error_$id",error.name).commit()
  if(status==PackageInstaller.STATUS_PENDING_USER_ACTION) {
   val confirm=androidx.core.content.IntentCompat.getParcelableExtra(intent,Intent.EXTRA_INTENT,Intent::class.java)
   if(confirm!=null) {
    // Foreground bridge starts this UI; receiver only records it to avoid Android background activity launch restrictions.
    PendingConfirmations.put(context,id,confirm)
   }
  }
  context.sendBroadcast(Intent("com.dualprofile.app.INSTALL_CHANGED").setPackage(context.packageName))
  DPLog.info("Install","Session $id status=$status verified=${WorkInstaller.installed(context,pkg)}")
 }
}
object PendingConfirmations {
 fun put(c: Context,id: Int,intent: Intent) { c.getSharedPreferences("install",Context.MODE_PRIVATE).edit().putString("confirm_$id",intent.toUri(Intent.URI_INTENT_SCHEME)).commit() }
 fun take(c: Context,id: Int): Intent? {
  val p=c.getSharedPreferences("install",Context.MODE_PRIVATE)
  val raw=p.getString("confirm_$id",null) ?: return null
  val result=runCatching { Intent.parseUri(raw,Intent.URI_INTENT_SCHEME) }.getOrNull()
  p.edit().remove("confirm_$id").commit()
  return result
 }
}
