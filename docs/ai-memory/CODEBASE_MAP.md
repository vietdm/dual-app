# Codebase map

| Path | Responsibility / when to read |
|---|---|
| app/build.gradle.kts, root Gradle files | SDK, Compose, dependencies, release signing |
| build.sh | Java/SDK validation, tests/lint/build, manifest/signature verify, dist |
| app/src/main/AndroidManifest.xml | permissions, exposed/protected components, FileProvider |
| Domain.kt | pure eligibility, install errors/states, sorting/search, profile abstraction |
| admin/ProfileAdminReceiver.kt | completion callback, protected profile broadcasts |
| admin/ProvisioningActivity.kt | modern mode/compliance/success activities |
| profile/WorkInstallPolicyActivity.kt, PolicyConsent.kt | user-confirmed own Work-local policy change + manual Android source permission; private owner-gated Activity |
| installer/SessionPolicy.kt | allows only debug lawful Probe to reach OS for diagnostic; no security permission granted |
| profile/ProfileManager.kt | support, provisioning, owner initialization, quiet/resume |
| packages/AppRepository.kt | package metadata/source/critical filters, icon LRU application context |
| crossprofile/Protocol.kt, Authentication.kt, ReplayWindow.kt | pairing/HMAC/expiry/replay, authenticated results |
| crossprofile/WorkBridgeActivity.kt | owner-gated Work commands, install confirmation/retry/recovery, Work stores/open |
| installer/ApkTransfer.kt | immutable base/split snapshots, SHA256, FileProvider grants |
| installer/WorkInstaller.kt | affiliation, restrictions, Work package/store queries, session commit/result receiver |
| installer/InstallJobs.kt | application-scoped preparation, durable nonce→session tracking; no Activity retained |
| ui/MainViewModel.kt | discovery/state/capabilities bound to Work profile; live invalidation |
| MainActivity.kt | Compose home/search/themes/diagnostics, public OS forwarding, profile event refresh |
| diagnostics/Diagnostics.kt | Build/public profile/API capabilities; unavailable OEM versions honestly reported |
| data/Log.kt | technical Logcat wrapper; no credentials/payloads |
| app/src/main/res | original XML/vector adaptive/round/monochrome icon, admin/file-path XML |
| app/src/test | pure domain/authentication/replay tests |
| app/src/androidTest | actual-device discovery/launch/Work owner-policy smoke tests |
| probe | lawful isolated-value/local-alarm notification fixture, optional native ABI payload |
| scripts/prepare-probe-native.sh, make-probe-splits.sh | optional NDK fixture + official Bundletool split creation |
| docs/MANUAL_TEST.md, SAMSUNG_VALIDATION.md | acceptance cases and actual evidence |

Generated directories: app/build, probe/build, .gradle, dist. Never confuse fixture generation/build pass with physical split installation pass. No backend/database/network service exists.
