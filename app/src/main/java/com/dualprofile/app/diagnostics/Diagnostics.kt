package com.dualprofile.app.diagnostics
import android.content.Context
import android.os.Build
import com.dualprofile.app.profile.ProfileSnapshot
import com.dualprofile.app.installer.WorkInstaller
object Diagnostics {
 fun describe(c: Context,s: ProfileSnapshot,ownerVerified: Boolean,bridge: Boolean): String = buildString {
  appendLine("Manufacturer: ${Build.MANUFACTURER}"); appendLine("Brand: ${Build.BRAND}"); appendLine("Model: ${Build.MODEL}"); appendLine("Device: ${Build.DEVICE}")
  appendLine("Android version: ${Build.VERSION.RELEASE}"); appendLine("SDK: ${Build.VERSION.SDK_INT}"); appendLine("Security patch: ${Build.VERSION.SECURITY_PATCH}"); appendLine("Build fingerprint: ${Build.FINGERPRINT}")
  appendLine("Samsung detected: ${Build.MANUFACTURER.equals("samsung",true)}")
  appendLine("One UI version: unavailable through supported public Android APIs")
  val knoxPackage=runCatching { c.packageManager.getPackageInfo("com.samsung.android.knox.containercore",0) }.getOrNull()
  appendLine("Knox package presence: ${knoxPackage!=null}; framework version: unavailable (no Knox SDK/private API)")
  appendLine("Work Profile supported: ${s.supported}"); appendLine("Provisioning allowed: ${s.provisioningAllowed}")
  appendLine("Work Profile exists: ${s.work!=null}"); appendLine("Managed profile UserHandle: ${s.work ?: "none"}"); appendLine("Quiet Mode: ${s.quiet}")
  appendLine("Current profile: ${if(s.inWork) "Work" else "Personal"}"); appendLine("Local Profile Owner: ${s.owner}"); appendLine("Work owner verified by companion: $ownerVerified")
  appendLine("Personal installed: ${!s.inWork}"); appendLine("Work installed/paired: ${s.inWork || ownerVerified}"); appendLine("Cross-profile communication: $bridge")
  appendLine("installExistingPackage allowed here: ${WorkInstaller.existingAllowed(c)}")
  appendLine("PackageInstaller available here: ${WorkInstaller.installerAvailable(c)}")
  appendLine("Security policy restriction here: ${WorkInstaller.restrictions(c)}")
  appendLine("Installed status comes from live Work PackageManager queries; no notification access.")
 }
}
