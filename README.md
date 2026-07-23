# GrokTerm

**Android terminal environment optimized for xAI Grok Build**

A Termux-like experience purpose-built for running the official Grok Build CLI (`grok`) natively on Android (aarch64).  
No proot required for the binary. Automatic DNS patching. One-tap friendly setup. Designed as the mobile companion for SuperGrok / Grok Imagine Cinematic Studio workflows.

> **Status**: Phase 1 — Native Termux integration + enhanced installer (live).  
> Phase 2 — Standalone Android terminal APK with embedded Grok (in progress).

## Why GrokTerm?

Grok Build is a powerful terminal-native coding agent. Running it well on a phone requires:
- Correct aarch64 musl static binary
- Writable DNS resolv for musl
- Persistent storage + proper `$HOME` / `.grok` layout
- Easy auth (API key or browser flow where possible)
- Quick access for skills, Production Bibles, and cinematic agent workflows

GrokTerm solves the friction that currently exists when people try to run `curl ... | bash` inside stock Termux.

## Quick Start (Phase 1 — Recommended)

1. Install **Termux** from [F-Droid](https://f-droid.org/packages/com.termux/) (not Play Store).
2. Open Termux and run:

```bash
pkg update -y && pkg install -y curl git python
curl -fsSL https://raw.githubusercontent.com/FineComputer14451/GrokTerm/main/scripts/install-native.sh | bash
```

3. Authenticate:

```bash
export XAI_API_KEY="xai-..."   # recommended on mobile
# or just run `grok` and follow device / browser flow if available
grok
```

4. Start building:

```bash
cd ~/storage/shared/YourProject   # after termux-setup-storage
grok
```

The installer:
- Downloads the official Grok aarch64 musl binary
- Applies the 16-byte DNS path patch (`/etc/resolv.conf` → writable location)
- Installs a smart launcher that survives `grok update`
- Sets up convenient aliases and a `grokterm` helper

## Features (Current)

- **Native execution** — no proot, no root required for the Grok binary itself
- **Automatic DNS fix** — self-healing on updates
- **Storage ready** — prompts for `termux-setup-storage`
- **Launcher** that re-applies patch after self-updates
- **GrokTerm helper** commands for cinematic studio workflows (coming)
- Clean uninstall

## Architecture Overview

```
GrokTerm/
├── scripts/
│   ├── install-native.sh     # One-command setup
│   ├── grok-dns.py           # Byte-patch + DNS file manager
│   └── launcher.sh           # Smart wrapper around official binary
├── android/                  # Phase 2: Standalone APK (Kotlin + Compose + TerminalView)
├── docs/
│   └── ARCHITECTURE.md
└── README.md
```

The Grok binary is **static musl aarch64**. It runs directly on the Android kernel.  
The only hard-coded dependency that breaks on Termux is the DNS resolv path. We patch that 16-byte string in-place to a location under `/sdcard` (or equivalent writable path after storage setup).

## Phase 2 Roadmap (Standalone APK)

- Modern Jetpack Compose UI
- Embedded terminal emulator (based on proven open-source terminal components)
- One-tap "Install / Update Grok"
- Built-in API key manager + secure storage
- Quick launch tiles for common Grok Build modes (plan, headless, skill)
- File browser integration for Production Bibles and skill folders
- Optional proot fallback for full Linux packages when needed
- Deep link support for Grok Imagine / Cinematic Studio handoff packets

## Requirements

- Android device with **aarch64 / arm64**
- Termux (F-Droid) for Phase 1
- Internet for first download + auth
- SuperGrok or X Premium+ subscription (or valid `XAI_API_KEY`)

## Credits & Inspiration

- Official Grok Build by xAI (open source: `xai-org/grok-build`)
- Community pioneer work by [Thr45hx/grok-cli-termux-native](https://github.com/Thr45hx/grok-cli-termux-native) for the DNS patch technique
- Termux project

## License

MIT — see LICENSE

---

**GrokTerm** is an independent community project. Not affiliated with or endorsed by xAI / SpaceXAI.  
Built for the Grok Imagine Cinematic Studio community and mobile power users.
