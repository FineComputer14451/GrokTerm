# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose app for running Grok Build on Android.

## Open in Android Studio

1. Clone the repo
2. Open the `android/` folder
3. Vendor xterm.js assets (see below)
4. Gradle sync → Run on **aarch64** device

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
- SAF project / Production Bible picker
- **DocumentFile browser** (navigate folders inside the selected tree)
- Quick Actions: Grok · Plan · Headless · Resume
- Encrypted API key + working-directory injection

## Next

- Optional Termux deep-link bridge
- Text file preview inside browser
- Release polish
