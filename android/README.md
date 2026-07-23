# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose app for running Grok Build on Android.

## Open in Android Studio

1. Clone the repo → open `android/`
2. Vendor xterm.js assets (below)
3. Gradle sync → Run on **aarch64** device

## Vendor xterm.js

```bash
cd android/app/src/main/assets/xterm
curl -L -o xterm.min.js           https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css          https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

## Features

- Adaptive icon + first-run onboarding
- Official aarch64 binary download + DNS patch
- Offline xterm.js full TUI
- SAF project picker + DocumentFile browser
- Quick Actions: Grok · Plan · Headless · Resume
- **Termux bridge (Phase 1)**
  - Detects Termux
  - `RUN_COMMAND` to start `grok` (optionally in project dir)
  - Fallback: launch Termux app or open F-Droid install page

## Termux RUN_COMMAND setup

In Termux: Settings → allow external apps / RUN_COMMAND for GrokTerm.  
Without that grant, the bridge falls back to simply opening Termux.

## Phase 1 vs Phase 2

| | Phase 1 (Termux) | Phase 2 (this app) |
|--|------------------|--------------------|
| Binary | Native musl via installer | Downloaded into app storage |
| TUI | Termux terminal | xterm.js WebView |
| Best for | Full native, packages | One-app mobile workflow |
