# GrokTerm

**Android terminal environment optimized for xAI Grok Build**

A Termux-like experience purpose-built for running the official Grok Build CLI (`grok`) on Android (aarch64).  
Automatic DNS patching · offline full TUI · Production Bible workflows · Phase 1 Termux bridge.

> **Status (v0.3.0)**  
> **Phase 1** — Termux native installer → **live**  
> **Phase 2** — Standalone Compose APK → **beta feature-complete**

## Quick Start — Phase 1 (Termux)

1. Install **Termux** from [F-Droid](https://f-droid.org/packages/com.termux/).
2. Run:

```bash
pkg update -y && pkg install -y curl git python
curl -fsSL https://raw.githubusercontent.com/FineComputer14451/GrokTerm/main/scripts/install-native.sh | bash
```

3. Authenticate and launch:

```bash
export XAI_API_KEY="xai-..."
grok
```

## Phase 2 — Android App (v0.3.0)

Open the `android/` folder in Android Studio.

**Before first build**, vendor xterm.js assets:

```bash
cd android/app/src/main/assets/xterm
curl -L -o xterm.min.js           https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.min.js
curl -L -o xterm.min.css          https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.min.css
curl -L -o addon-fit.min.js       https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.min.js
curl -L -o addon-web-links.min.js https://cdn.jsdelivr.net/npm/@xterm/addon-web-links@0.11.0/lib/addon-web-links.min.js
```

Then Gradle sync → Run on a physical **aarch64** device.

### App capabilities

| Area | Features |
|------|----------|
| Binary | Download latest from x.ai/cli + auto DNS patch |
| Terminal | Offline xterm.js TUI · Shell / Grok modes |
| Auth | Encrypted API key storage + env injection |
| Projects | SAF picker · DocumentFile browser · text preview |
| Actions | Grok · Plan · Headless `-p` · Resume |
| Bridge | Open in Termux (RUN_COMMAND) / F-Droid install |
| UX | Onboarding · adaptive icon · Material3 dark |

See [CHANGELOG.md](CHANGELOG.md) and [android/README.md](android/README.md).

## Architecture

```
GrokTerm/
├── scripts/           # Phase 1 Termux installer + DNS patcher
├── android/           # Phase 2 Compose APK
├── docs/ARCHITECTURE.md
├── CHANGELOG.md
└── README.md
```

## Requirements

- aarch64 / arm64 Android device
- SuperGrok / valid `XAI_API_KEY`
- Termux (F-Droid) for Phase 1

## License

MIT

---

**GrokTerm** is an independent community project. Not affiliated with or endorsed by xAI.
