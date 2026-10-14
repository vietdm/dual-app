# SAMSUNG GALAXY A57 VALIDATION

Date: 2026-10-14 (Asia/Bangkok). Real USB-connected device, not emulator. Provisioning operated manually by the user; no provisioning clicks automated.

```text
Device:
Samsung SM-A576B

Android:
16 / API 36

One UI:
8.5 (development adb property 80500; no runtime private property access)

Work Profile provisioning:
PASS

Profile Owner:
PASS

Reboot persistence:
NOT TESTED - awaiting manual reboot/unlock verification

App discovery:
PASS

Create Work instance:
FAIL - lawful Probe blocked by Work installation-source policy

PackageInstaller:
PARTIAL - actual base transfer/verification/session commit succeeds; Android policy refuses installation

Open Personal instance:
NOT TESTED - target Open A button acceptance not yet executed

Open Work instance:
NOT TESTED - no Probe Work installation; companion itself launches successfully

Independent application data:
NOT TESTED - no Probe Work installation

Notifications Personal:
NOT TESTED

Notifications Work:
NOT TESTED

Quiet Mode detection:
NOT TESTED - active-state query verified; paused/resumed transition pending user test

Auto Blocker impact:
NOT DETERMINED - no evidence isolating Auto Blocker; observed restriction is no_install_unknown_sources

Known Samsung limitations:
Fresh managed profile has no_install_unknown_sources restriction.
Profile Owner is unaffiliated; installExistingPackage is not permitted.
Shell lacks permission to access Work user 10; Work instrumentation install denied.
No Samsung security control or Work restriction was disabled.
```

## Evidence

- Device identified by ADB as SM_A576B / a57x; SDK36 and development OneUI property80500 read.
- Before setup: only Personal user0. After user completed setup: user10 `Dual Profile`, managed profile, running.
- `dumpsys device_policy`: Profile Owner User10 = `com.dualprofile.app/.admin.ProfileAdminReceiver`; affiliation IDs empty; installation-source restriction present.
- DPC completion/success callbacks log `Profile owner verified; companion initialized` twice, showing idempotent owner initialization.
- Authenticated Work roundtrip logged `owner verified; packages=31 installer=false affiliated=false policyBlocked=true`.
- Final Personal device tests: discovery, activity launch and rejecting fully-managed-only mode passed; one Work-only test skipped (not a Work PASS).
- Personal instrumentation discovered503 packages, support=true, provisioningAllowed=false after existing profile, no local Personal owner.
- Probe installed legally in Personal via development ADB. User invoked Diagnostics → Test Probe installation → Work Install unchanged application, then Retry. Both returned `UNKNOWN_SOURCES_NOT_ALLOWED`.
- [Actual screenshot](evidence/samsung-install-policy-block.png) shows Work-badged companion and source-policy error. [Condensed diagnostic evidence](evidence/samsung-spike.txt).

This is **not a successful dual-instance PoC**: provisioning and companion work, but the required local Probe cannot currently be installed in Work while preserving the observed policy. Initial attempt stopped at local preflight. Follow-up debug Probe-only diagnostic actually transferred/verified the base APK and committed a public PackageInstaller session. OS returned pending-user-action then aborted and displayed a policy refusal. No PackageInstaller STATUS_SUCCESS or independent Work data/notification evidence exists. A store-resolvable URI alone does not make unpublished Probe eligible. Public installation policy handling follows [UserManager restrictions](https://developer.android.com/reference/android/os/UserManager); affiliation follows [DevicePolicyManager.installExistingPackage](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#installExistingPackage(android.content.ComponentName,%20java.lang.String)).

## Split test status

| Case | Artifact verification | Physical Work installation |
|---|---|---|
| Base/universal Probe APK | Built, same lawful debug certificate | FAIL: source-policy preflight |
| Base + ABI config split | Generated using official Bundletool1.18.3 | NOT TESTED: policy blocks |
| Base + ABI + language splits | Generated, device-selected Samsung set | NOT TESTED: policy blocks |
| arm64-v8a | Native fixture generated with16KB ELF alignment | NOT TESTED in Work |
| x86_64 | Alternative fixture generated | NOT TESTED; incompatible with primary target |
| Same package/signature/version | Fixture manifests/certificates verifiable | NOT TESTED after Work installation |
| Signature/version conflicts | Error handling implemented | NOT TESTED physically |

Do not use ADB install --user10 to claim Dual Profile installation success. No third-party application testing was advanced past the failed Probe spike. Remaining acceptance checklist: docs/MANUAL_TEST.md.

User deferred reboot verification until after delivery (2026-10-14). Quiet-mode toggle was explained; no paused/resumed transition performed in this session. Both remain NOT TESTED.

## Follow-up actual PackageInstaller spike

See [real OS policy dialog](evidence/samsung-packageinstaller-real-policy-block.png) and [session log](evidence/samsung-packageinstaller-real.txt). APK bytes/hash verified inside Work; session210660213 committed; OS denied installation under unchanged policy. No Google Play used in this test. User has requested a way to grant permissions; explicit own-Work-policy consent screen and manual Android source-permission flow now implemented, but its mutation/installation result has not been validated yet because user consent is still required.

Permission review screen opened successfully via authenticated bridge on SM-A576B. Actual public API state: Dual Profile owns Work; its own local source restriction is active; effective install policy blocked; Android source permission not granted. [Screenshot](evidence/samsung-work-policy-review.png). No Allow/consent/source-setting button pressed by agent. This confirms an owner-managed policy setup gap, not a blanket impossibility of custom APKs on Samsung.
