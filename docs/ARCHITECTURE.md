# GrokTerm Architecture

## Goals

1. Make Grok Build first-class on Android phones/tablets.
2. Minimize friction vs desktop Linux experience.
3. Stay compatible with official Grok self-updates.
4. Provide a Termux-like standalone APK and a native Termux path.

## Phase 1 (Live) — Termux Native

```
User → Termux → GrokTerm launcher → (patched) official grok binary
```

- Binary: static musl aarch64 from xAI
- Only modification: 16-byte string replace for DNS resolv path
- Launcher can re-patch after `grok update`
- DNS file on shared storage (`/sdcard/.grokdns`)

## Phase 2 (v0.3.0) — Standalone APK

```
android/app/src/main/java/com/finecomputer/grokterm/
├── data/
│   ├── GrokBinaryManager.kt       # path + DNS patch
│   ├── GrokBinaryDownloader.kt    # x.ai/cli + GCS fallback
│   ├── ApiKeyStore.kt             # EncryptedSharedPreferences
│   ├── ProjectStore.kt            # SAF tree URI + persistable perms
│   ├── OnboardingStore.kt
│   ├── LaunchAction.kt / PendingLaunch
│   └── TermuxBridge.kt            # RUN_COMMAND / launch / F-Droid
└── ui/
    ├── terminal/XtermTerminal.kt  # WebView + offline xterm.js bridge
    └── screens/
        ├── OnboardingScreen.kt
        ├── HomeScreen.kt
        ├── TerminalScreen.kt
        ├── SettingsScreen.kt
        └── FileBrowserScreen.kt   # DocumentFile + text preview
```

### Terminal stack

```
TerminalScreen
  └── XtermTerminal (AndroidView → WebView → file:///android_asset/xterm/)
  └── Process (Shell or patched grok binary)
        stdout → term.write()
        term.onData → process stdin
```

### Binary download flow

1. `GET https://x.ai/cli/stable` → version
2. `GET https://x.ai/cli/grok-<ver>-linux-aarch64`
3. Fallback GCS bucket
4. Write to app files → executable → DNS patch

### Key constraints

- Android SELinux + no writable `/etc`
- Static musl binary → patch resolv path in-place
- SAF trees may not resolve to real paths (cwd fallback to app files)
- Official binary may change the resolv string → patcher must stay defensive

## Integration with Grok Imagine Cinematic Studio

Supported today:
- Open Production Bible / skills folders via SAF
- Browse + preview Bible / skill text files
- Launch Grok with that folder as working directory when resolvable
- Hand off to Termux for full native sessions
