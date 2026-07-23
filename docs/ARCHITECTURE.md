# GrokTerm Architecture

## Goals

1. Make Grok Build first-class on Android phones/tablets.
2. Minimize friction vs desktop Linux experience.
3. Stay compatible with official Grok self-updates.
4. Provide a foundation for a future standalone APK that feels like Termux + Grok.

## Phase 1 (Current) — Termux Native

```
User → Termux → GrokTerm launcher → (patched) official grok binary
```

- Binary: static musl aarch64 from xAI
- Only modification: 16-byte string replace for DNS resolv path
- Launcher re-patches after `grok update`
- DNS file lives on shared storage (`/sdcard/.grokdns`)

## Phase 2 — Standalone APK (Planned)

- Kotlin + Jetpack Compose UI
- Terminal emulator component (candidate libraries to evaluate: modern forks of android-terminal-emulator, or integrate Termux terminal view under compatible license)
- Embedded or downloadable Grok binary + automatic patching
- Secure storage for XAI_API_KEY
- Quick actions: Plan mode, Headless, Resume session, Open skills folder
- Optional full Linux env via proot when user needs packages beyond the static binary

## Key Constraints

- Android SELinux + no writable /etc
- Static binary ignores LD_PRELOAD → must patch binary itself
- Storage permission model requires termux-setup-storage or equivalent MANAGE_EXTERNAL_STORAGE / scoped storage handling in APK
- Official binary may change the resolv string in future releases → patcher must be robust / version-aware

## Integration with Grok Imagine Cinematic Studio

Future helpers:
- `gt-studio` — quick open / sync Production Bible folders from shared storage
- Skill install shortcuts
- Handoff packet validation on device
