# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose scaffold for a Termux-like experience focused on Grok Build.

## Open in Android Studio

1. Clone the repo
2. Open the `android/` folder as a project
3. Let Gradle sync
4. Run on a physical **aarch64** device (Grok binary is aarch64-only)

## Current capabilities

- Dark Material3 theme (Grok-inspired cyan on near-black)
- Home screen with status + binary check/patch
- **Full TUI Terminal** powered by **xterm.js**:
  - Proper ANSI colors, cursor, mouse support, resize
  - Dual mode: **Shell** and **Grok** (direct binary launch)
  - Automatic `XAI_API_KEY` injection
  - Clean process lifecycle
- Settings with encrypted API key storage
- `GrokBinaryManager` + 16-byte DNS path patcher

## Terminal architecture

```
TerminalScreen
  └── XtermTerminal (WebView + xterm.js + FitAddon)
        ↕ JavascriptInterface bridge
  └── Process (Shell or patched grok binary)
```

Output is streamed as raw bytes/characters so Grok’s rich TUI renders correctly.  
User keystrokes come from xterm.js → AndroidBridge → process stdin.

> Note: The current scaffold loads xterm.js from jsDelivr CDN.  
> For offline / production builds, vendor the assets into `app/src/main/assets/`.

## Next implementation priorities

1. Vendor xterm.js assets for fully offline use
2. Robust binary downloader (mirror official install.sh)
3. SAF / file browser for Production Bibles & skills
4. Quick-action tiles (Plan mode, headless `-p`, resume)
5. Adaptive icons + first-run onboarding
6. Optional deep-link bridge to Phase 1 Termux environment
7. Explore native PTY later if needed for even lower latency

## Build notes

- minSdk 28, targetSdk 35, compileSdk 35
- Java 17 / Kotlin 2.0
- Compose BOM 2024.10.01
- Internet permission required (for CDN assets + Grok network calls)

Binary is expected at the app’s private `files/grok/bin/grok`.  
Use the Phase 1 Termux installer today, or place a known-good aarch64 musl binary there.
