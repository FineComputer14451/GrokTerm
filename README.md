# GrokTerm

**Android terminal environment optimized for xAI Grok Build**

A Termux-like experience purpose-built for running the official Grok Build CLI (`grok`) natively on Android (aarch64).  
No proot required for the binary. Automatic DNS patching. One-tap friendly setup. Designed as the mobile companion for SuperGrok / Grok Imagine Cinematic Studio workflows.

> **Status**:  
> **Phase 1** — Native Termux integration + enhanced installer → **live**  
> **Phase 2** — Standalone Android APK (Kotlin + Compose) → **scaffolded & building**

## Quick Start — Phase 1 (Recommended today)

1. Install **Termux** from [F-Droid](https://f-droid.org/packages/com.termux/) (not Play Store).
2. Open Termux and run:

```bash
pkg update -y && pkg install -y curl git python
curl -fsSL https://raw.githubusercontent.com/FineComputer14451/GrokTerm/main/scripts/install-native.sh | bash
```

3. Authenticate:

```bash
export XAI_API_KEY="xai-..."   # recommended on mobile
grok
```

## Phase 2 — Android App Scaffold

The `android/` directory contains a complete, modern Jetpack Compose project:

- Material3 dark theme (cyan accent on near-black)
- Home / Terminal / Settings screens
- `GrokBinaryManager` + Kotlin DNS path patcher (same 16-byte technique)
- Encrypted API key storage
- Basic interactive shell (ready to be upgraded to full VT emulator)

**Open in Android Studio**: open the `android/` folder → Gradle sync → Run.

See [android/README.md](android/README.md) for details and next priorities.

## Architecture

```
GrokTerm/
├── scripts/                  # Phase 1 Termux native installer + DNS patcher
├── android/                  # Phase 2 Compose APK scaffold
│   └── app/src/main/java/... 
│       ├── data/             # GrokBinaryManager, ApiKeyStore, DnsPatcher
│       └── ui/               # Screens + theme + navigation
├── docs/ARCHITECTURE.md
└── README.md
```

## Requirements

- aarch64 / arm64 Android device
- SuperGrok or valid `XAI_API_KEY`
- Termux (F-Droid) for Phase 1

## Credits

- Official Grok Build by xAI (open source: `xai-org/grok-build`)
- DNS patch technique pioneered in community Termux ports
- Termux project

## License

MIT

---

**GrokTerm** is an independent community project. Not affiliated with or endorsed by xAI.  
Built for the Grok Imagine Cinematic Studio community and mobile power users.
