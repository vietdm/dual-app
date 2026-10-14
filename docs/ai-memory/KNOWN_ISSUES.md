# Known issues and evidence limits

1. A57 fresh Work profile includes no_install_unknown_sources. MVP deliberately preserves it, blocking unaffiliated PackageInstaller path. Work stores are only valid for packages with store-source evidence. Local Probe is not published; it must not be listed as store-only eligible. Do not clear restriction/disable Auto Blocker to manufacture success.
2. SM-A576B shell refuses access to Work user10, including instrumentation installation. ADB Personal tests and dumpsys are available. Do not use ADB to install the cloned target in Work or label that as app-flow success.
3. Unaffiliated BYOD installExistingPackage unavailable by documented Android restriction. Affiliation cannot be invented by local preferences or setting arbitrary affiliation IDs.
4. Cross-profile OS confirmation/OEM launch denial remains possible; use Work-badged target icon fallback.
5. No Work instance means no evidence yet for independent A/B data, Work notifications or Work reboot notifications. Probe local alarms are not push tests. Report NOT TESTED rather than inferring from architecture.
6. Store may delist/restrict packages, require accounts and update shared package code globally. User confirmation/clear failure fallback still necessary even with provenance.
7. Installation survives rotation through application-scoped preparation plus persisted nonce/session mapping. Android confirmation intent stored privately for process recovery; missing/interrupted sessions have retry/cancel instead of indefinite success/polling. Validate these paths physically where installation allowed.
8. Losing pairing data breaks authentication safely. Existing Work target applications continue operating; no silent key recovery/takeover. Recreate profile via normal UI only after preserving required Work data.
9. OneUI/Knox runtime versions unavailable through supported APIs; do not introduce SystemProperties/reflection or shell. Dev adb property is evidence only.
10. Conservative system-app exclusion may hide usable preinstalled apps; expand only with safe metadata/capability tests.

Resolved during initial implementation: min28 lint error for API30 isManagedProfile; fallback guarded DPC public API. Store-resolution false eligibility; replay-window clearing; stale profile snapshots; activity-scoped install/confirmation loss. Keep their regression tests.

Modern EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES is ArrayList<Integer>, not int[]. Mode activity uses getIntegerArrayListExtra; physical instrumentation rejects fully-managed-only mode. API35+ LauncherUserInfo.userType distinguishes actual managed profile from private/other associated profiles, with no hidden-profile permission requested.

User deferred reboot verification until after delivery (2026-10-14). Quiet-mode toggle was explained; no paused/resumed transition performed in this session. Both remain NOT TESTED.
