# Prompt template

Work on Dual Profile in this workspace. First read AGENTS.md and the five docs/ai-memory files. Task: <specific change/bug>.

Preserve SDK36/36/28, com.dualprofile.app, one managed Work profile, public Android APIs and all security/privacy constraints. Never treat activity result or local mapping as owner/installation truth. Do not bypass OEM/other-admin/global policy or change security settings automatically. User requested permission setup: only the explicit consent UI may remove Dual Profile’s own Work-local unknown-source restriction; source permission remains a manual Android UI action. Record exact effective restrictions and actual session results.

Read CODEBASE_MAP to select relevant sources. Implement and run meaningful tests/lint/build.sh. Update memory when durable facts change. Report changed behavior, evidence, APK absolute path and outstanding physical acceptance. Do not claim SM-A576B PASS unless executed and supported by logs/query/user observation.

For physical tests: identify actual device/API/build; user operates provisioning UI. ADB permitted only for development install/diagnostics/logs/validation, never required in product. Test original lawful Probe before third-party apps, including base/multiple splits/ABI/signature/version variants where policy permits. Compare Personal A and Work B and host-independent notifications; local alarm is not push validation.
