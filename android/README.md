# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose scaffold for a Termux-like experience focused on Grok Build.

## Open in Android Studio

1. Clone the repo
2. Open the `android/` folder as a project
3. **Vendor xterm.js assets** (required once — see below)
4. Let Gradle sync → Run on a physical **aarch64** device

## Vendor xterm.js (offline / production)

The terminal is fully offline-capable. You must place the JS/CSS files once:

```bash
cd android/app/src/main/assets/xterm

curl -L -o xterm.min.js           https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css          https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

See `assets/xterm/README.md` for details. After this, the terminal UI needs no network.

## Current capabilities

- Dark Material3 theme (Grok-inspired cyan on near-black)
- Home screen with status + binary check/patch
- **Full TUI Terminal** powered by **xterm.js** (offline):
  - Proper ANSI colors, cursor, mouse support, resize
  - Dual mode: **Shell** and **Grok** (direct binary launch)
  - Automatic `XAI_API_KEY` injection
  - Clean process lifecycle
- Settings with encrypted API key storage
- `GrokBinaryManager` + 16-byte DNS path patcher

## Terminal architecture

```
TerminalScreen
  └── XtermTerminal (WebView → file:///android_asset/xterm/index.html)
        ↕ JavascriptInterface bridge
  └── Process (Shell or patched grok binary)
```

## Next implementation priorities

1. Robust binary downloader (mirror official install.sh)
2. SAF / file browser for Production Bibles & skills
3. Quick-action tiles (Plan mode, headless `-p`, resume)
4. Adaptive icons + first-run onboarding
5. Optional deep-link bridge to Phase 1 Termux environment

## Build notes

- minSdk 28, targetSdk 35, compileSdk 35
- Java 17 / Kotlin 2.0
- Compose BOM 2024.10.01

Binary expected at app private `files/grok/bin/grok`.  
Use Phase 1 Termux installer or place a known-good aarch64 musl binary.
