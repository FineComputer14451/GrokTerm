# GrokTerm Architecture

## Goals

1. Make Grok Build first-class on Android phones/tablets.
2. Minimize friction vs desktop Linux experience.
3. Stay compatible with official Grok self-updates.
4. Provide a foundation for a standalone APK that feels like Termux + Grok.

## Phase 1 (Live) — Termux Native

```
User → Termux → GrokTerm launcher → (patched) official grok binary
```

- Binary: static musl aarch64 from xAI
- Only modification: 16-byte string replace for DNS resolv path
- Launcher re-patches after `grok update`
- DNS file lives on shared storage (`/sdcard/.grokdns`)

## Phase 2 (Scaffolded) — Standalone APK

```
android/
├── settings.gradle.kts / build.gradle.kts
└── app/
    ├── build.gradle.kts          # Compose, minSdk 28, target 35
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/finecomputer/grokterm/
        │   ├── GrokTermApp.kt
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── GrokBinaryManager.kt   # download stub + DNS patch
        │   │   └── ApiKeyStore.kt         # EncryptedSharedPreferences
        │   └── ui/
        │       ├── theme/                 # Grok dark cyan theme
        │       ├── GrokTermNavHost.kt
        │       └── screens/
        │           ├── HomeScreen.kt
        │           ├── TerminalScreen.kt  # basic Process shell (upgrade path to VT emulator)
        │           └── SettingsScreen.kt
        └── res/
```

### Key classes

- **GrokBinaryManager**: Owns the private `files/grok/bin/grok` location, applies the same 16-byte DNS patch used in Phase 1, prepares launch command.
- **ApiKeyStore**: Secure storage for `XAI_API_KEY`.
- **TerminalScreen**: Currently a simple interactive `/system/bin/sh` session with Compose output + input. Designed so the process can later be replaced by the patched Grok binary or a full PTY + libvterm/xterm.js-style emulator.

### Next technical steps for Phase 2

1. Replace basic shell with a proper terminal emulator component (evaluate Termux terminal view under compatible terms, or a modern VT100 library + PTY).
2. When binary is present, default the terminal process to the patched `grok` (or offer a “Launch Grok” action that starts it in plan / interactive mode).
3. Implement reliable binary acquisition (mirror official install.sh or ship a known artifact).
4. Shared storage / SAF integration for opening Production Bibles and skill folders.
5. Foreground service option for long-running agent sessions.
6. Adaptive icons + polish.

## Key Constraints

- Android SELinux + no writable /etc
- Static binary ignores LD_PRELOAD → must patch binary itself
- Storage permission model (scoped storage + MANAGE_EXTERNAL_STORAGE for full access)
- Official binary may change the resolv string → patcher must remain robust

## Integration with Grok Imagine Cinematic Studio

Planned helpers:
- Quick open / sync Production Bible folders from shared storage
- Skill install shortcuts
- Handoff packet validation on device
- Deep links from studio tools into GrokTerm sessions
