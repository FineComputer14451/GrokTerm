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

## Phase 2 — Standalone APK (with full TUI)

```
android/
└── app/src/main/java/com/finecomputer/grokterm/
    ├── data/
    │   ├── GrokBinaryManager.kt      # binary location + 16-byte DNS patch
    │   └── ApiKeyStore.kt            # EncryptedSharedPreferences
    └── ui/
        ├── terminal/
        │   └── XtermTerminal.kt      # WebView + xterm.js + FitAddon + JS bridge
        └── screens/
            ├── HomeScreen.kt
            ├── TerminalScreen.kt     # dual Shell/Grok mode + process management
            └── SettingsScreen.kt
```

### Terminal stack

```
TerminalScreen
  └── XtermTerminal (Compose AndroidView → WebView)
        • xterm.js 5.x + FitAddon + WebLinksAddon
        • JavascriptInterface bridge (AndroidBridge)
  └── Process (Shell or patched grok binary)
        • stdout/stderr → raw stream → term.write()
        • keystrokes ← term.onData → process stdin
```

This gives Grok Build its full TUI (colors, cursor, mouse, resize, scrollback) without requiring NDK/PTY yet.

### Key classes

- **GrokBinaryManager**: Owns private binary path, applies DNS patch, exposes launch path.
- **ApiKeyStore**: Secure storage for `XAI_API_KEY` (injected into process env).
- **XtermTerminal / XtermController**: Renders the terminal and exposes write/clear/focus.
- **TerminalScreen**: Owns process lifecycle, mode switching, and bridges the two sides.

### Next technical steps

1. Vendor xterm.js assets for offline use
2. Robust binary downloader (mirror official install.sh)
3. SAF / file browser for Production Bibles & skills
4. Quick-action tiles (Plan mode, headless, resume)
5. Adaptive icons + onboarding
6. Optional native PTY later for lower latency if needed

## Key Constraints

- Android SELinux + no writable /etc
- Static binary ignores LD_PRELOAD → must patch binary itself
- Storage permission model
- Official binary may change the resolv string → patcher must stay robust

## Integration with Grok Imagine Cinematic Studio

Planned helpers:
- Quick open / sync Production Bible folders
- Skill install shortcuts
- Handoff packet validation
- Deep links from studio tools into GrokTerm sessions
