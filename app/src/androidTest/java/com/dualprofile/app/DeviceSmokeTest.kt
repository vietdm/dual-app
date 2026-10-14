package com.dualprofile.app
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.dualprofile.app.packages.AppRepository
import com.dualprofile.app.profile.ProfileManager
import com.dualprofile.app.installer.WorkInstaller
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {
 @Test fun publicApiDiscoveryAndProfileState() {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val apps=AppRepository(c).scan(); val profile=ProfileManager(c).snapshot()
  assertTrue(Build.VERSION.SDK_INT>=28)
  assertTrue(apps.isNotEmpty())
  assertTrue(apps.any { it.facts.packageName==c.packageName })
  assertFalse(apps.filter { it.facts.critical }.any { evaluate(it.facts,InstallCapability(true,true,true),true)==Eligibility.ELIGIBLE })
  Log.i("DualProfile-Test","manufacturer=${Build.MANUFACTURER} model=${Build.MODEL} sdk=${Build.VERSION.SDK_INT} packages=${apps.size} supported=${profile.supported} provisioningAllowed=${profile.provisioningAllowed} inWork=${profile.inWork} owner=${profile.owner} quiet=${profile.quiet}")
 }
 @Test fun ownerAndInstallPolicyInWork() {
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val profile=ProfileManager(c).snapshot()
  org.junit.Assume.assumeTrue("Work-profile instrumentation required",profile.inWork)
  if(profile.inWork) {
   assertTrue(profile.owner)
   assertFalse(WorkInstaller.existingAllowed(c))
   Log.i("DualProfile-Test","Work owner verified, affiliated=${WorkInstaller.existingAllowed(c)}, installationPolicyBlocked=${WorkInstaller.restrictions(c)}")
   if(WorkInstaller.restrictions(c)) {
    try { WorkInstaller.install(c,JSONObject().put("package","com.dualprofile.probe"),null,"policy-smoke"); fail("Blocked policy must not be bypassed") }
    catch(e: IllegalStateException) { assertTrue(e.message in listOf("SECURITY_POLICY_BLOCKED","UNKNOWN_SOURCES_NOT_ALLOWED")) }
   }
  }
 }
 @Test fun rejectsFullyManagedModeOnly() {
  org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT>=31)
  val c=InstrumentationRegistry.getInstrumentation().targetContext
  val intent=android.content.Intent(c,com.dualprofile.app.admin.ProvisioningActivity::class.java).setAction(android.app.admin.DevicePolicyManager.ACTION_GET_PROVISIONING_MODE).putIntegerArrayListExtra(android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES,arrayListOf(android.app.admin.DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE))
  ActivityScenario.launchActivityForResult<com.dualprofile.app.admin.ProvisioningActivity>(intent).use { scenario -> assertEquals(android.app.Activity.RESULT_CANCELED,scenario.result.resultCode) }
 }
 @Test fun mainActivityLaunches() { ActivityScenario.launch(MainActivity::class.java).use { scenario -> scenario.onActivity { assertFalse(it.isFinishing) } } }
}
