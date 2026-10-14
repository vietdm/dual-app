# Manual acceptance checklist

Record device/model/build/security patch/date, APK hash and test outcomes. Never label unexecuted steps PASS. Samsung SM-A576B / Android16/API36/OneUI8.5 is the primary acceptance gate. ADB is development-only. Do not automate provisioning clicks or disable security controls.

## Provisioning

- [ ] Fresh device: supported=true, provisioningAllowed=true, Create Work Profile launches standard OS flow.
- [ ] Unsupported managed-users device: clear unsupported message, no launch/crash.
- [ ] Existing external DPC profile: no takeover/reprovision/bypass; clear error.
- [ ] Cancel during setup: no owner-success claim; refresh actual users/profile state.
- [ ] Complete setup manually; receiver initializes owner; `isProfileOwnerApp` true in Work.
- [ ] Compliance/mode activities only choose managed profile; lifecycle works without fixed screen count.
- [ ] Personal receives managed-profile broadcast / onResume refresh, HMAC companion query verifies owner.
- [ ] Reboot, unlock, open host: profile and owner persist; no reprovision.

Development evidence: `adb shell pm list users`, `adb shell dumpsys device_policy`, filtered `adb logcat -s DualProfile-DPC DualProfile-Install DualProfile-CrossProfile DualProfile-Test`.

## PackageInstaller spike FIRST (Probe, not WhatsApp/Zalo)

- [ ] Install dist/DualProfileProbe.apk in Personal; save A.
- [ ] Create its Work instance through host; confirm standard installer UI if requested.
- [ ] Same package/certificate/version visible to Work PackageManager; Open B succeeds, save B.
- [ ] Relaunch both: Personal=A, Work=B. Check signatures using apksigner in development.
- [ ] Single/base APK.
- [ ] Base plus one config split.
- [ ] Base plus multiple config splits (resource and ABI).
- [ ] arm64-v8a compatible ABI; alternative ABI fixture on compatible secondary device.
- [ ] Wrong-only ABI APK set rejected, readable failure, no installed success claim.
- [ ] All copied split APKs match original bytes/SHA-256, no re-signing/modification.
- [ ] Same signature and same version; different signature and downgrade rejected.
- [ ] Target package disappears / updates during copying: refresh/retry, no crash.
- [ ] Unreadable base/split: exclusion/store fallback.
- [ ] Already Work-installed: only mark from Work package query, show Open B.
- [ ] Installation canceled: failure/cancel state and retry/store.
- [ ] App unavailable/no launcher/store blocked: unsupported, no crash.
- [ ] BYOD unaffiliated: installExistingPackage not called; affiliated path catches SecurityException.
- [ ] Auto Blocker/unknown source/device policy failure reported; never change protection automatically.
- [ ] Google Play absent → only use resolved available store; Galaxy Store hidden/blocked is nonfatal.

Optional split fixture: `scripts/prepare-probe-native.sh`, rebuild, then `scripts/make-probe-splits.sh /path/to/bundletool.jar`. Personal fixture installation with `adb install-multiple` is allowed for development only; Work installation must use Dual Profile itself. Use Bundletool-selected sets in dist/split-fixtures/samsung and x86; never combine alternate base masters. Wrong-ABI test on Samsung is expected to fail installation, not silently select an incompatible split. Do not record alternative-ABI runtime PASS on a device that cannot run it.

## Runtime / isolation

- [ ] App list excludes self, system/launcher/IME/admin/disabled/instant/no-launcher/unreadable-no-strategy.
- [ ] Search labels/packages without case sensitivity; sort alphabetically; real icons.
- [ ] Open A and Open B via supported intents; missing launchers/OEM denial show fallback.
- [ ] Two independent target logins/data.
- [ ] Force stop only com.dualprofile.app: both target apps keep working.
- [ ] Reboot/unlock; both apps keep saved values without opening host.
- [ ] Pause Work: host detects quiet mode, warns Account B notifications unavailable.
- [ ] Resume: targets operate again; settings fallback when resume API refused.
- [ ] Light, Dark, System theme; rotation; install confirmation with rotation/process recreation.
- [ ] Forged/expired/replayed cross-profile requests rejected; other app cannot issue arbitrary install commands.

## Notifications

- [ ] Probe: allow its own notification permission in each profile, schedule alarms, kill host, both deliver with correct Work badge. Android may delay alarms.
- [ ] Actual messaging/push target: Personal account A and Work account B; kill host; externally send to A then B; independently deliver, B Work badge present.
- [ ] Pause Work: B suspended; resume and verify target recovery behavior.
- [ ] Reboot, unlock, Work active, do not open host, send to A/B; both receive if target supports it.
- [ ] Record permission/battery/target-app limitations, not fabricated host guarantees.

## OEM matrix

Run applicable checks on Pixel/AOSP, Samsung primary, other Samsung Android16, Xiaomi, OPPO/OnePlus, Vivo. All nonexecuted cells remain NOT TESTED. MVP has no OEM security workaround or Knox dependency.

## Explicit Work APK permission setup

- [ ] Open permission flow: no setting changes merely by opening or canceling.
- [ ] Personal/non-owner/other-admin restriction cannot be cleared.
- [ ] Explicitly confirm own Work-local source policy change, inspect actual DPM state; no Personal/global/OEM change.
- [ ] Manually grant Dual Profile Work install-source permission through Android UI.
- [ ] Return, refresh, custom installed-Personal app uses Copy APK to Work without Google/Play.
- [ ] Cancel consent or system permission: clear failure; no installed success.
- [ ] Other policy/Auto Blocker remains blocking: no bypass or requests to disable protection.
