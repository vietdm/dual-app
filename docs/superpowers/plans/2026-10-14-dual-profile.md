# Dual Profile Implementation Plan
Execution: native inline, authorized by user. Spec: ../specs/2026-10-14-dual-profile.md.
Goal: build installable DPC and honest device validation.
Architecture: profile manager, pure eligibility domain, authenticated intent bridge, Work installer, Compose UI. Tech: Kotlin 2.2.20, AGP 8.13.1, Gradle 8.14, Compose.
Global constraints: compile/target 36, minimum 28, one Work profile, public APIs only.
Review focus: canceled provisioning; unpaired/forged commands; unreadable or changing splits; policy-blocked installs; paused or externally managed profile.
- [x] Scaffold wrapper/build and pure-domain failing tests, then implement eligibility/sorting/status and pass tests.
- [x] Implement DPC lifecycle, authenticated cross-profile protocol and APK snapshot/installer. Verify compile incrementally, add authentication tests.
- [x] Build Compose home/diagnostics/theme, actual icons and probe fixture. Run lint/unit/build.
- [x] Verify APK manifest/signature; install/launch via development ADB if available; record physical tests truthfully.
- [x] Complete README, manual matrix, Samsung report and AI Memory. Re-run build.sh.

Execution ledger: direct implementation authorized by user; no approval pauses. Native execution with whole-project independent review completed. Fixed false store eligibility, stale profile snapshots, replay window, durable installation session/confirmation recovery and matching store routing. Platform gate: A57 default no_install_unknown_sources prevents Probe install; preserved controls. Actual base/split fixtures built but Work split/isolation/push tests remain unexecuted. See SAMSUNG_VALIDATION; completion means source/APK delivered, not successful dual-instance acceptance.
