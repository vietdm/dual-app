# Dual Profile workspace instructions

Read `/home/vietdau33/.codex/RTK.md` when available, then these AI Memory files first:
1. docs/ai-memory/PROJECT_CONTEXT.md
2. docs/ai-memory/CODEBASE_MAP.md
3. docs/ai-memory/KNOWN_ISSUES.md
4. docs/ai-memory/AI_PROMPT_TEMPLATE.md
5. docs/ai-memory/README.md

Use code and actual test evidence as truth. Update relevant memory after durable architecture/build/security/device facts change. Do not read the entire source tree by default; follow CODEBASE_MAP to affected modules.
SDK 36/36/28 is fixed by product requirements. Primary physical target is SM-A576B Android16 OneUI8.5. No private API, shell/ADB runtime, root, Shizuku, Knox dependency, APK modification/re-signing, policy bypass or notification proxy.
Never infer owner/provisioning success from Activity.RESULT_OK. Never infer Work-installed from a sent request/local mapping. Preserve security controls; never clear other-admin/global restrictions or change settings automatically to make tests pass. User explicitly requested APK permission setup: WorkInstallPolicyActivity may remove ONLY this admin’s Work-local unknown-source restriction after the user confirms its consent dialog. Android source permission remains a manual system UI action; never automate those consent/settings controls.
Provisioning UI must be operated by the user. Record unexecuted physical tests as NOT TESTED, not PASS. Keep debug/release signing distinction. Never commit keystores/passwords/generated build output.
