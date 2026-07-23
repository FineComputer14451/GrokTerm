# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose app for running Grok Build on Android.

## Setup

1. Open `android/` in Android Studio
2. Vendor xterm.js into `app/src/main/assets/xterm/` (see curl commands in git history / prior README)
3. Run on **aarch64** device

## Features

- Adaptive icon + first-run onboarding
- Official aarch64 binary download + DNS patch
- Offline xterm.js full TUI
- SAF project picker + DocumentFile browser
- **Text file preview** (md, txt, json, yaml, source code, …) up to 512 KB
- Quick Actions: Grok · Plan · Headless · Resume
- Termux Phase 1 bridge (RUN_COMMAND / launch / F-Droid)

## Next

- Release polish (version, ProGuard, changelog)
