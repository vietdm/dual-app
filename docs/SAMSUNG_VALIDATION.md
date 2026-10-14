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
PARTIAL - implementation/build verified, source-policy preflight blocks before session commit

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

This is **not a successful dual-instance PoC**: provisioning and companion work, but the required local Probe cannot currently be installed in Work while preserving the observed policy. The installer intentionally refuses before creating/committing a session; no PackageInstaller STATUS_SUCCESS or independent Work data/notification evidence exists. A store-resolvable URI alone does not make unpublished Probe eligible. Public installation policy handling follows [UserManager restrictions](https://developer.android.com/reference/android/os/UserManager); affiliation follows [DevicePolicyManager.installExistingPackage](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#installExistingPackage(android.content.ComponentName,%20java.lang.String)).

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
