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
- **Upgraded Terminal**:
  - Dual mode: **Shell** (`/system/bin/sh`) and **Grok** (direct launch of patched binary)
  - One-tap “Start Grok” / restart Shell from the top bar
  - Automatic injection of `XAI_API_KEY` into the process environment
  - Better concurrent stdout reader + clean lifecycle
- Settings with encrypted API key storage
- `GrokBinaryManager` + 16-byte DNS path patcher (Kotlin port of the Termux technique)

## Next implementation priorities

1. Full PTY + VT100 / xterm-style emulator (for rich Grok TUI: colors, mouse, fullscreen)
   - Candidates: integrate a modern terminal-view component or WebView + xterm.js
2. Robust binary downloader that mirrors official `install.sh`
3. File browser / SAF for Production Bibles & skills
4. Quick-action tiles (Plan mode, headless `-p`, resume session)
5. Adaptive icons + first-run onboarding polish
6. Optional deep link / Intent bridge to Phase 1 Termux environment

## Build notes

- minSdk 28, targetSdk 35, compileSdk 35
- Java 17 / Kotlin 2.0
- Compose BOM 2024.10.01

Binary is expected at the app’s private `files/grok/bin/grok`.  
Use the Phase 1 Termux installer today, or place a known-good aarch64 musl binary there. Automatic download will follow.
