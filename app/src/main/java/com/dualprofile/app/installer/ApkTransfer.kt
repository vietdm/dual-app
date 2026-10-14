package com.dualprofile.app.installer
import android.content.*
import androidx.core.content.FileProvider
import com.dualprofile.app.packages.DualAppCandidate
import org.json.*
import java.io.File
import java.security.MessageDigest
import java.util.UUID
object ApkTransfer {
 fun sha(file: File): String { val digest=MessageDigest.getInstance("SHA-256"); file.inputStream().use { input -> val b=ByteArray(64*1024); while(true) { val n=input.read(b); if(n<0) break; digest.update(b,0,n) } }; return digest.digest().joinToString("") { "%02x".format(it) } }
 fun snapshot(context: Context,app: DualAppCandidate): Pair<JSONObject,ClipData> {
  val root=File(context.cacheDir,"transfers").apply { mkdirs() }
  root.listFiles()?.filter { System.currentTimeMillis()-it.lastModified()>86_400_000 }?.forEach { it.deleteRecursively() }
  val dir=File(root,UUID.randomUUID().toString()).apply { mkdirs() }
  try {
   require(app.apkPaths.size in 1..128)
   val entries=JSONArray(); var clip: ClipData?=null; var total=0L
   app.apkPaths.forEachIndexed { i,path ->
    val src=File(path); total+=src.length(); require(total in 1..2_147_483_648L) { "APK set too large" }
    val dst=File(dir,if(i==0) "base.apk" else "split_$i.apk")
    src.copyTo(dst); require(sha(src)==sha(dst)) { "Application changed during APK transfer. Refresh and retry." }
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",dst)
    entries.put(JSONObject().put("name",dst.name).put("sha",sha(dst)).put("size",dst.length()))
    if(clip==null) clip=ClipData.newUri(context.contentResolver,"Unmodified application APK",uri) else clip!!.addItem(ClipData.Item(uri))
   }
   return JSONObject().put("package",app.facts.packageName).put("version",app.versionCode).put("storeVerified",app.facts.storeVerified).put("storeSource",app.facts.storeSource ?: "").put("files",entries) to clip!!
  } catch(e: Exception) { dir.deleteRecursively(); throw e }
 }
}
