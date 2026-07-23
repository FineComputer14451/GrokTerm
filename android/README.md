# GrokTerm Android — v0.3.0

Jetpack Compose app for Grok Build on Android.

## Build

1. Open this `android/` folder in Android Studio
2. **Vendor xterm.js** (required once):

```bash
cd app/src/main/assets/xterm
curl -L -o xterm.min.js           https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css          https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

3. Gradle sync → Run on physical **aarch64** device

## Release build

```bash
./gradlew :app:assembleRelease
```

- `minifyEnabled` + `shrinkResources` on
- Rules in `app/proguard-rules.pro` (WebView JS bridge, Tink, DataStore)

## Feature map

- Download / update official aarch64 binary + DNS patch
- Offline xterm.js TUI (Shell + Grok)
- Encrypted API key
- SAF project picker + DocumentFile browser + text preview
- Quick Actions: Grok · Plan · Headless · Resume
- Termux bridge
- Onboarding + adaptive icon

## Notes

- minSdk 28 · targetSdk 35 · versionName **0.3.0**
- Internet required for binary download and Grok network calls
- Grant Termux RUN_COMMAND for GrokTerm to auto-start sessions in Phase 1
