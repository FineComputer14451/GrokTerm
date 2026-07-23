# Changelog

All notable changes to GrokTerm are documented here.

## [0.3.0] — 2026-07-22

### Phase 2 Android app — feature complete for first beta

**Added**
- Official Grok Build aarch64 binary downloader (x.ai/cli + GCS fallback) with progress UI
- Automatic 16-byte DNS path patch after download
- Offline xterm.js terminal (assets under `android/app/src/main/assets/xterm/`)
- Dual Shell / Grok session mode with `XAI_API_KEY` injection
- SAF project / Production Bible picker with persistable permissions
- DocumentFile browser with folder navigation
- Text file preview (md, source, config, …) up to 512 KB
- Quick Actions: Interactive Grok, Plan mode, Headless `-p`, Resume
- First-run onboarding (4 steps)
- Adaptive launcher icon (dark + cyan terminal prompt)
- Termux Phase 1 bridge (`RUN_COMMAND` / launch / F-Droid install)

**Changed**
- Version bumped to `0.3.0`
- Architecture docs updated to match shipped surface area

**Notes**
- Vendor xterm.js assets before building (see `android/README.md`)
- Physical **aarch64** device required
- Termux `RUN_COMMAND` permission must be granted in Termux for auto-start

## [0.2.0] — Phase 2 scaffold

- Jetpack Compose project structure
- Home / Terminal / Settings
- Encrypted API key store
- Basic process + DNS patch manager

## [0.1.0] — Phase 1

- Termux native installer scripts
- DNS path patcher (`scripts/grok-dns.py`)
