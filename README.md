# Dual Profile

Native Kotlin / Jetpack Compose / Material 3 application, `com.dualprofile.app`. Creates **one** Android managed Work Profile so a compatible application can run with two independent logins. Primary validation target: Samsung Galaxy A57 SM-A576B, Android 16/API 36, One UI 8.5. Uses public Android Enterprise APIs; no Knox SDK/license.

## Architecture

Personal Profile = Account A. Work Profile = Account B. Android supplies separate UID/data/processes. Original package names, certificates and APK bytes stay unchanged. Target applications receive their own notifications; Dual Profile does not proxy them and need not remain running.

`ProfileAdminReceiver` handles provisioning completion and verifies `isProfileOwnerApp`. Modern provisioning mode/compliance/success activities initialize the owner idempotently. Protected profile broadcasts and activity resume trigger live state refresh. Activity RESULT_OK alone is never success evidence.

Personal scans packages and snapshots all installed APK files. A Work-only companion is enabled by the DPC. A public cross-profile intent filter routes requests through the OS forwarder. Commands and replies have HMAC-SHA256 authentication using a random key passed in provisioning admin extras, short expiry and replay detection. FileProvider grants provide read-only base/split APK access. Work validates digest, package and version before committing a PackageInstaller session. Android verifies signatures and compatibility. Profile Owner affiliation is required before trying `installExistingPackage`; SecurityException falls back to the installer. Store provenance must match a resolved Work store before store-only eligibility is granted. Resolved Work stores are fallback options, Google Play is preferred for apps with Play provenance; when only Galaxy provenance is verified, the available Galaxy route is retained instead of opening an unverified Play listing. Store availability does not guarantee a particular package is listed or compatible.

A successful Work query confirms installation; local preferences never establish installed state. Eligibility excludes self, framework/system packages, launcher, IMEs, admins, disabled/instant/no-launcher apps. This conservative MVP excludes **all system/updated-system apps**, including some noncritical preinstalled applications. Before the first verified companion roundtrip the main list is empty. Diagnostics shows exclusion reasons.

## Minimum device requirements

| Requirement | Minimum / condition |
|---|---|
| Android OS | Android 9 (Pie), API 28 or later |
| Android feature | `android.software.managed_users` / Android Enterprise Work Profile supported |
| Provisioning | `DevicePolicyManager.isProvisioningAllowed(ACTION_PROVISION_MANAGED_PROFILE)` must return true |
| Existing management | No conflicting Work Profile owned by another DPC; only one Personal + one Work instance supported |
| Installation | At least one allowed Work installation path: PackageInstaller, authorized affiliated installExistingPackage, or a launchable Work store |
| Storage | Enough free space for the target application's base/splits, temporary transfer copies, installation and separate Work data |
| CPU / RAM | No additional fixed CPU/RAM requirement claimed; the target app must support the device ABI and have sufficient resources in both profiles |
| Network | Host does not require Internet permission; app-store downloads and target-app login/push may require a network |
| Manufacturer | Any compatible Android manufacturer; Samsung is preferred for validation, not mandatory |
| User privileges | No root, Knox license, ADB, Shizuku or Android Studio required for production use |

**Recommended:** Android 12/API 31 or later. **Primary validation target:** Samsung SM-A576B, Android 16/API 36, One UI 8.5 (individual physical-test outcomes in the validation report). Android Go, OEM/enterprise policy and existing device management can make Work Profile unavailable even if the Android version meets the minimum. Meeting the OS floor does not guarantee every installed app is eligible.

## Development requirements

- Android 9/API 28 minimum; preferred Android 12+, primary Android 16.
- Managed-users support, provisioning allowed by Android, no conflicting managed profile.
- Java 17, Android SDK `platforms;android-36` and `build-tools;36.0.0`.
- Gradle Wrapper 8.14, AGP 8.13.1, Kotlin 2.2.20; internet for first dependency download. No Android Studio required.

SDK 36 is intentional and fixed. A newer locally installed SDK does not silently change the Android 16 target or reduce restrictions.

## Build

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
chmod +x build.sh
./build.sh                 # debug-signed installable APK
./build.sh debug
```

The script checks Java/SDK/wrapper, writes local.properties, runs unit tests and blocking lint, builds both app and lawful probe, verifies manifest/target and signature, copies `dist/DualProfile.apk`, `dist/DualProfileProbe.apk`, probe AAB and checksums. It exits nonzero on missing dependencies or build failure. Wrapper files are included; add them to version control with the rest of the source.

Release signing is supplied by you:

```bash
export DUALPROFILE_KEYSTORE=/absolute/path/your-release.jks
export DUALPROFILE_KEYSTORE_PASSWORD='your-secret'
export DUALPROFILE_KEY_ALIAS='your-alias'
export DUALPROFILE_KEY_PASSWORD='your-secret'
./build.sh release
```

Missing values cause `ERROR: release signing configuration is missing.` No production keystore is generated automatically. `scripts/generate-dev-keystore.sh` is optional and interactive. Keep a stable signing key for updates and both profile instances. Never commit passwords or keystores.

## Install

Development:

```bash
adb install -r dist/DualProfile.apk
adb install -r dist/DualProfileProbe.apk
```

End users can open the APK and use Android's normal installation flow. Runtime has no ADB, root, Shizuku, private API or shell requirement. Device security policy may refuse sideloading; Dual Profile cannot disable Auto Blocker, Safe Install, Play Protect or enterprise policies.

## First run

Open Dual Profile → Create Work Profile → accept the **real Android/Samsung setup UI yourself** → return → verify Work companion → select compatible app → Copy APK to Work or Install via Work store → confirm inside Work → sign in to Account B. Android may show a cross-profile confirmation screen. There is no assumed screen count or Samsung text automation.

Test the supplied Probe before messaging applications. Save A in Personal and B in Work. Schedule its delayed notifications independently; kill only the Dual Profile host. Probe alarms are local notification smoke tests, **not push/Firebase validation**. For actual messaging/reboot push use a lawful app with two accounts and external senders. See [manual checklist](docs/MANUAL_TEST.md).

## Cấp quyền cài APK custom trong Work

Sau khi user yêu cầu cấp quyền, ứng dụng cung cấp luồng **chủ động xác nhận**, không thay đổi bảo mật tự động:

1. Mở Dual Profile ở Personal → **Quyền cài APK custom trong Work**.
2. Bên Work, chọn **1. Cho phép APK custom trong Work** → đọc scope → **Cho phép trong Work**. DPC chỉ gỡ `DISALLOW_INSTALL_UNKNOWN_SOURCES` do **chính admin Dual Profile** quản lý trong Work. Không gỡ restriction global/install-apps/bên khác hoặc thay đổi Auto Blocker/Play Protect.
3. Chọn **2. Cấp quyền nguồn cài đặt Android** → tự bật cho phép cài từ **Dual Profile bên Work** trong màn hình Android, nếu được policy cho phép.
4. **Hoàn tất / quay lại** → refresh Work apps. Custom app đã cài trong Personal có APK đọc được sẽ dùng **Copy APK to Work** khi capability thực tế cho phép.

Nếu policy khác/OEM vẫn chặn, luồng báo hạn chế và không vượt qua. Không cần tài khoản Google cho copy APK. File APK chưa được cài vào Personal không nằm trong scanner của MVP; đây chưa phải tính năng import APK từ file picker.

Nút **Install via Work store** chỉ mở store; nó không copy APK. Nút **Copy APK to Work** mới chạy snapshot/URI transfer/PackageInstaller. CH Play trong Work có tài khoản/dữ liệu riêng; đăng nhập tài khoản Google khác không phải requirement của cơ chế copy APK.

## Work active / paused

Quiet mode is queried from UserManager. Resume uses the public API; if permission/OEM policy refuses, the app opens Android settings so you can resume normally. Paused Work apps cannot receive notifications. Unlock both profiles after reboot if required by Android.

## Remove Work Profile

Use Android Settings → Accounts / Work Profile → Remove Work Profile (labels vary by OEM), or the launcher/quick-settings Work management UI. This deletes **all Work data**; export needed data first. Dual Profile does not silently wipe profiles. An externally managed profile cannot be taken over. Removing only the Personal host does not remove target applications' Personal data.

## Privacy and permissions

- `QUERY_ALL_PACKAGES`: device management scanner and eligibility/diagnostics; distribution stores may require policy review.
- `REQUEST_INSTALL_PACKAGES`: unmodified APK session installation inside Work, subject to system policy/confirmation.
- `BIND_DEVICE_ADMIN` is a **component gate**, not a permission we pretend to hold. Android alone invokes protected admin/provisioning components.
- AndroidX adds a package-private signature permission `com.dualprofile.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` to protect internal dynamic receivers; it is not a user-granted capability.
- Host has no POST_NOTIFICATIONS, Internet, notification listener, Accessibility, screen capture, credential or app-data access permission.
- Probe alone asks POST_NOTIFICATIONS to test its own notifications. Neither app reads other apps' data.
- Pairing/preferences are profile-local private storage, backup disabled. No account credentials saved.

## Known limitations

1. Physical provisioning and package installation require user participation; building an APK is not proof of acceptance. Current evidence is in [Samsung report](docs/SAMSUNG_VALIDATION.md).
2. BYOD profile owners are generally unaffiliated; installExistingPackage is unavailable there. PackageInstaller can require confirmation or be blocked. No policy bypass.
3. Store fallback may install another version. Android shares package code across users; a store update can affect the version used by Personal. Review store prompts. APK transfer targets the installed Personal version and does not request downgrade.
4. Some banks, DRM, integrity-sensitive and OEM applications refuse Work Profile or additional accounts. Available installation method does not guarantee their business logic supports dual use.
5. Cross-profile launch can be refused by OEM/policy. Open the target's Work-badged icon as the supported fallback.
6. After host data is cleared/reinstalled with lost pairing, companion authentication fails safely. Existing Work applications continue to operate. MVP cannot silently recover the pairing key; inspect the Work companion and recreate the Work Profile via Android UI only if its data is disposable.
7. Work package/capability snapshots are invalidated on resume and profile broadcasts and refreshed by authenticated sync. A package changed while the Personal screen stays open may need Refresh Work apps; Open B handles missing launcher without crashing. No install success is fabricated.
8. A Java-only fixture does not prove ABI compatibility. Optional fixture-generation scripts create native ABI/resource splits for physical testing; all requested split variants must be recorded individually.
9. One UI and Knox framework versions have no supported generic public API here; runtime reports unavailable instead of reading hidden properties. ADB getprop is development-only.
10. Installation sessions and confirmation intents have private persistent recovery, retry/cancel controls and application-scoped preparation. OEM restrictions can still prevent confirmation UI; no background activity launch bypass.
11. Physical A57 provisioning succeeded, but the fresh Work profile has `no_install_unknown_sources` policy. The MVP preserves it until the user explicitly confirms the new own-Work-policy permission flow; it never changes security automatically. The development Diagnostics Probe spike reports this limitation. Samsung also refuses shell access to Work user 10, so Work instrumentation could not be installed through ADB.

## OEM matrix

| OEM | Target | Status |
|---|---|---|
| Samsung | SM-A576B / Android 16 | See physical report |
| Samsung other | Android 16 | NOT TESTED |
| Pixel / AOSP | Android 12+ | NOT TESTED |
| Xiaomi | Android 12+ | NOT TESTED |
| OPPO / OnePlus | Android 12+ | NOT TESTED |
| Vivo | Android 12+ | NOT TESTED |

## API references

[DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager), [managed-profile content URI sharing](https://developer.android.com/work/managed-profiles), [PackageInstaller sessions](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session), [UserManager quiet mode](https://developer.android.com/reference/android/os/UserManager). These document affiliation restrictions, provisioning lifecycle and supported URI sharing. Platform forwarding behavior was checked against [AOSP IntentForwarderActivity](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/app/IntentForwarderActivity.java).

## AI Memory

Start with [docs/ai-memory/README.md](docs/ai-memory/README.md) and the five context files listed in AGENTS.md. They map modules, build commands, limitations and verified evidence without reading all sources. Trust code and reports over stale memory, and update memory when durable facts change.

## License

Dự án sử dụng [Dual Profile Non-Commercial License 1.0](LICENSE), mã SPDX tùy chỉnh `LicenseRef-DualProfile-NonCommercial-1.0`.

- Được sử dụng, học tập, sao chép, chỉnh sửa và chia sẻ miễn phí cho **mục đích phi thương mại**.
- **Không được kinh doanh/thương mại hóa**: bán, thu phí, quảng cáo kiếm tiền, cung cấp sản phẩm/dịch vụ trả phí hoặc dùng để phục vụ hoạt động kinh doanh. Hạn chế áp dụng cả bản sửa đổi tích hợp code của dự án.
- Khi chia sẻ phải giữ LICENSE, thông tin bản quyền và ghi rõ nếu đã chỉnh sửa.
- Muốn sử dụng thương mại phải có chấp thuận bằng văn bản riêng từ chủ sở hữu quyền tương ứng.

Đây là giấy phép riêng dạng source-available/phi thương mại. Dependency, AndroidX/Kotlin/Compose và Gradle Wrapper vẫn giữ giấy phép của bên thứ ba; LICENSE này không thay đổi giấy phép của họ. Văn bản tiếng Anh trong LICENSE là điều khoản áp dụng, phần tiếng Việt ở đây là tóm tắt.
