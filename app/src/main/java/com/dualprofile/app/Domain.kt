package com.dualprofile.app

enum class Eligibility { ELIGIBLE, ALREADY_DUAL, NO_LAUNCHER, SYSTEM_CRITICAL, INSTALL_SOURCE_UNAVAILABLE, PROFILE_RESTRICTION, UNSUPPORTED }
enum class InstallMethod { EXISTING, PACKAGE_INSTALLER, STORE, NONE }
enum class InstallState { AVAILABLE, CREATING, INSTALLED, CONFIRMATION, FAILED, UNSUPPORTED, PAUSED }
enum class InstallError { INSTALL_ALLOWED, USER_CONFIRMATION_REQUIRED, UNKNOWN_SOURCES_NOT_ALLOWED, SECURITY_POLICY_BLOCKED, PACKAGE_INSTALLER_UNAVAILABLE, STORE_INSTALL_AVAILABLE, NO_VALID_INSTALL_METHOD, VERSION_OR_SIGNATURE_CONFLICT, INSTALLATION_CANCELED, INCOMPATIBLE_APK_SET, INSUFFICIENT_STORAGE, INVALID_APK }
enum class ProfilePhase { ABSENT, CONFIGURING, ACTIVE, PAUSED }
data class AppFacts(val packageName: String, val label: String, val launcher: Boolean, val critical: Boolean, val enabled: Boolean, val instant: Boolean, val readable: Boolean, val storeVerified: Boolean=false, val storeSource: String?=null)
data class InstallCapability(val installer: Boolean, val store: Boolean, val existing: Boolean, val restricted: Boolean=false)
fun evaluate(f: AppFacts, c: InstallCapability, installed: Boolean): Eligibility = when {
 f.packageName == "com.dualprofile.app" || f.critical -> Eligibility.SYSTEM_CRITICAL
 !f.launcher -> Eligibility.NO_LAUNCHER
 !f.enabled || f.instant -> Eligibility.UNSUPPORTED
 installed -> Eligibility.ALREADY_DUAL
 c.existing || (c.store && f.storeVerified) || (c.installer && f.readable) -> Eligibility.ELIGIBLE
 c.restricted -> Eligibility.PROFILE_RESTRICTION
 !f.readable -> Eligibility.INSTALL_SOURCE_UNAVAILABLE
 else -> Eligibility.UNSUPPORTED
}
fun sortAndSearch(apps: List<AppFacts>, query: String) = apps.filter { it.label.contains(query,true) || it.packageName.contains(query,true) }.sortedWith(compareBy<AppFacts> { it.label.lowercase(java.util.Locale.ROOT) }.thenBy { it.packageName })
fun mapInstallState(status: Int, verified: Boolean) = when { status == 0 && verified -> InstallState.INSTALLED; status == -1 -> InstallState.CONFIRMATION; else -> InstallState.FAILED }
fun profilePhase(exists: Boolean, ownerVerified: Boolean, quiet: Boolean) = when { !exists -> ProfilePhase.ABSENT; quiet -> ProfilePhase.PAUSED; ownerVerified -> ProfilePhase.ACTIVE; else -> ProfilePhase.CONFIGURING }
