# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose scaffold for a Termux-like experience focused on Grok Build.

## Open in Android Studio

1. Clone the repo
2. Open the `android/` folder as a project (or the root and select the android module)
3. Let Gradle sync
4. Run on a physical aarch64 device or emulator (note: Grok binary is aarch64-only)

## Current capabilities

- Dark Material3 theme (Grok-inspired cyan on near-black)
- Home screen with status + binary check/patch
- Basic interactive shell terminal (Process + Compose)
- Settings with encrypted API key storage
- GrokBinaryManager + 16-byte DNS patcher (Kotlin port of the Termux technique)

## Next implementation priorities

1. Proper PTY + VT100/xterm emulator (replace the basic shell reader)
2. Direct launch of patched `grok` as the default process when binary is present
3. Robust binary downloader that mirrors official `install.sh`
4. File browser / shared storage access for Production Bibles & skills
5. Quick-action tiles (Plan mode, headless `-p`, resume session)
6. Icon set + adaptive icons polish
7. Optional Termux integration / Intent to launch full native environment

## Build notes

- minSdk 28, targetSdk 35, compileSdk 35
- Java 17 / Kotlin 2.0
- Compose BOM 2024.10.01

The binary management currently expects the aarch64 musl Grok binary to be placed at the app’s private `files/grok/bin/grok` (or obtained via Phase 1 Termux install + future sharing). Full automatic download will be added once the official artifact URL strategy is stable.
