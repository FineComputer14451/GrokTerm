# GrokTerm Android (Phase 2)

Kotlin + Jetpack Compose scaffold for a Termux-like experience focused on Grok Build.

## Open in Android Studio

1. Clone the repo
2. Open the `android/` folder as a project
3. Vendor xterm.js assets (see below)
4. Gradle sync → Run on a physical **aarch64** device

## Vendor xterm.js (offline)

```bash
cd android/app/src/main/assets/xterm

curl -L -o xterm.min.js           https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css          https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

## Current capabilities

- Adaptive icon (dark background + cyan terminal prompt vector)
- **First-run onboarding** (4 steps: welcome → binary → API key → project)
- One-tap Download / Update Grok Binary
- Full TUI via offline xterm.js
- Dual Shell / Grok mode + API key injection
- SAF Project / Production Bible picker
- Quick Actions: Grok · Plan · Headless · Resume

## Next priorities

1. Deeper DocumentFile browser inside selected trees
2. Optional Termux deep-link bridge
3. Polish / release prep

## Build notes

- minSdk 28, targetSdk 35
- Internet permission required for binary download + Grok network calls
