package com.dualprofile.app.packages
import android.content.*
import android.content.pm.*
import android.view.inputmethod.InputMethodManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.dualprofile.app.*
import java.io.File

data class DualAppCandidate(val facts: AppFacts, val versionName: String?, val versionCode: Long, val apkPaths: List<String>, val workInstalled: Boolean=false, val eligibility: Eligibility=Eligibility.UNSUPPORTED, val installMethod: InstallMethod=InstallMethod.NONE)
class AppRepository(context: Context) {
 private val context=context.applicationContext
 private val pm=this.context.packageManager
 private val icons=LruCache<String,Drawable>(64)
 fun icon(pkg: String): Drawable? = icons.get(pkg) ?: runCatching { pm.getApplicationIcon(pkg).also { icons.put(pkg,it) } }.getOrNull()
 @Suppress("DEPRECATION")
 fun scan(): List<DualAppCandidate> {
  val home=pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
  val keyboards=context.getSystemService(InputMethodManager::class.java).inputMethodList.map { it.packageName }.toSet()
  val admins=pm.queryBroadcastReceivers(Intent("android.app.action.DEVICE_ADMIN_ENABLED"),PackageManager.GET_META_DATA).map { it.activityInfo.packageName }.toSet()
  return pm.getInstalledPackages(0).mapNotNull { p ->
   val a=p.applicationInfo ?: return@mapNotNull null
   val paths=listOf(a.sourceDir)+a.splitSourceDirs.orEmpty().toList()
   val critical=a.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0 || a.uid<10000 || p.packageName==home || p.packageName in keyboards || p.packageName in admins
   val installer=runCatching { if(android.os.Build.VERSION.SDK_INT>=30) pm.getInstallSourceInfo(p.packageName).installingPackageName else pm.getInstallerPackageName(p.packageName) }.getOrNull()
   val storeVerified=installer in setOf("com.android.vending","com.sec.android.app.samsungapps")
   val f=AppFacts(p.packageName,pm.getApplicationLabel(a).toString(),pm.getLaunchIntentForPackage(p.packageName)!=null,critical,a.enabled,pm.isInstantApp(p.packageName),paths.isNotEmpty() && paths.all { File(it).isFile && File(it).canRead() },storeVerified,installer)
   DualAppCandidate(f,p.versionName,p.longVersionCode,paths)
  }.sortedWith(compareBy { it.facts.label.lowercase(java.util.Locale.ROOT) })
 }
}
