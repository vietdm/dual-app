package com.dualprofile.app
import org.junit.Assert.*
import org.junit.Test
class DomainTest {
 private fun facts() = AppFacts("org.example.app", "Example", true, false, true, false, true)
 @Test fun safeLauncherIsEligible() { assertEquals(Eligibility.ELIGIBLE, evaluate(facts(), InstallCapability(true, false, false), false)) }
 @Test fun selfExcludedEvenWhenInstalledInWork() { assertEquals(Eligibility.SYSTEM_CRITICAL,evaluate(facts().copy(packageName="com.dualprofile.app"),InstallCapability(true,true,true),true)) }
 @Test fun policyBlockedWithoutValidStoreExcluded() { assertEquals(Eligibility.PROFILE_RESTRICTION,evaluate(facts(),InstallCapability(false,true,false,true),false)) }
 @Test fun noLauncherExcluded() { assertEquals(Eligibility.NO_LAUNCHER, evaluate(facts().copy(launcher=false), InstallCapability(true, false, false), false)) }
 @Test fun systemExcluded() { assertEquals(Eligibility.SYSTEM_CRITICAL, evaluate(facts().copy(critical=true), InstallCapability(true, false, false), false)) }
 @Test fun ownPackageExcluded() { assertEquals(Eligibility.SYSTEM_CRITICAL, evaluate(facts().copy(packageName="com.dualprofile.app"), InstallCapability(true, false, false), false)) }
 @Test fun unreadableApkWithoutStoreExcluded() { assertEquals(Eligibility.INSTALL_SOURCE_UNAVAILABLE, evaluate(facts().copy(readable=false), InstallCapability(true, false, false), false)) }
 @Test fun storeMakesUnreadableEligible() { assertEquals(Eligibility.ELIGIBLE, evaluate(facts().copy(readable=false,storeVerified=true), InstallCapability(false, true, false), false)) }
 @Test fun unpublishedStoreOnlyAppExcluded() { assertEquals(Eligibility.UNSUPPORTED, evaluate(facts(),InstallCapability(false,true,false),false)) }
 @Test fun installedConfirmedByWork() { assertEquals(Eligibility.ALREADY_DUAL, evaluate(facts(), InstallCapability(false, false, false), true)) }
 @Test fun noWorkCapabilityIsUnsupported() { assertEquals(Eligibility.UNSUPPORTED, evaluate(facts(), InstallCapability(false, false, false), false)) }
 @Test fun disabledIsExcluded() { assertEquals(Eligibility.UNSUPPORTED, evaluate(facts().copy(enabled=false), InstallCapability(true, false, false), false)) }
 @Test fun instantIsExcluded() { assertEquals(Eligibility.UNSUPPORTED, evaluate(facts().copy(instant=true), InstallCapability(true, false, false), false)) }
 @Test fun sortingAndSearchAreInsensitive() { val apps=listOf(facts().copy(label="zeta"),facts().copy(label="Alpha",packageName="com.a")); assertEquals("Alpha", sortAndSearch(apps, "").first().label); assertEquals(1, sortAndSearch(apps,"COM.A").size) }
 @Test fun installSuccessNeedsVerification() { assertEquals(InstallState.FAILED, mapInstallState(0,false)); assertEquals(InstallState.INSTALLED, mapInstallState(0,true)); assertEquals(InstallState.CONFIRMATION, mapInstallState(-1,false)) }
 @Test fun activityResultNotOwnerEvidence() { assertEquals(ProfilePhase.CONFIGURING, profilePhase(true,false,false)); assertEquals(ProfilePhase.ACTIVE, profilePhase(true,true,false)); assertEquals(ProfilePhase.PAUSED, profilePhase(true,true,true)) }
}
