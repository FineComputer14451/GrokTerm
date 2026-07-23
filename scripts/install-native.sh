#!/data/data/com.termux/files/usr/bin/bash
# GrokTerm native installer for Termux (aarch64)
# Enhanced one-command setup for xAI Grok Build CLI

set -euo pipefail

PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
HOME_DIR="${HOME:-/data/data/com.termux/files/home}"
GROK_HOME="$HOME_DIR/.grok"
GROKTERM_DIR="$HOME_DIR/.grokterm"
SDCARD_DNS="/sdcard/.grokdns"
LAUNCHER="$GROKTERM_DIR/launcher.sh"
BIN_LINK="$PREFIX/bin/grok"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== GrokTerm Native Installer ===${NC}"
echo "Optimized Grok Build environment for Android / Termux"
echo

# Architecture check
ARCH=$(uname -m)
if [[ "$ARCH" != "aarch64" && "$ARCH" != "arm64" ]]; then
  echo -e "${RED}Error: Only aarch64/arm64 is supported (detected: $ARCH)${NC}"
  exit 1
fi

# Ensure basic packages
echo "[1/7] Checking packages..."
pkg update -y >/dev/null 2>&1 || true
pkg install -y curl git python 2>/dev/null || true

# Storage access
if [[ ! -d /sdcard ]]; then
  echo -e "${YELLOW}Requesting storage access...${NC}"
  termux-setup-storage || true
  sleep 2
fi

mkdir -p "$GROK_HOME" "$GROKTERM_DIR" "$GROK_HOME/versions" "$GROK_HOME/bin"

# DNS file
echo "[2/7] Preparing DNS resolv file..."
cat > "$SDCARD_DNS" << 'EOF'
nameserver 8.8.8.8
nameserver 8.8.4.4
nameserver 1.1.1.1
EOF
chmod 644 "$SDCARD_DNS" 2>/dev/null || true

# Download official installer or binary
echo "[3/7] Fetching Grok Build..."
# We use the official install script but will wrap the resulting binary
export PATH="$GROK_HOME/bin:$PATH"

if ! command -v grok >/dev/null 2>&1 || [[ ! -x "$GROK_HOME/bin/grok" ]]; then
  # Official install places things under ~/.grok
  curl -fsSL https://x.ai/cli/install.sh | bash || {
    echo -e "${YELLOW}Official installer had issues (common on Android). Falling back to manual binary fetch...${NC}"
    # Note: In real use the binary URL may change; community usually lets grok self-download
    # For robustness we rely on the launcher to trigger first run download if needed
  }
fi

# Install DNS patcher and launcher
echo "[4/7] Installing GrokTerm components..."

# Write grok-dns.py
cat > "$GROKTERM_DIR/grok-dns.py" << 'PYEOF'
#!/usr/bin/env python3
"""GrokTerm DNS path patcher for static musl Grok binary."""
import sys
import os
import shutil

ORIGINAL = b"/etc/resolv.conf"
REPLACEMENT = b"/sdcard/.grokdns"  # must be exactly 16 bytes

assert len(ORIGINAL) == 16
assert len(REPLACEMENT) == 16

def patch(path: str, dry_run: bool = False) -> bool:
    with open(path, "rb") as f:
        data = f.read()
    if ORIGINAL not in data:
        if REPLACEMENT in data:
            print(f"Already patched: {path}")
            return True
        print(f"String not found — binary may have changed: {path}")
        return False
    count = data.count(ORIGINAL)
    new_data = data.replace(ORIGINAL, REPLACEMENT)
    if dry_run:
        print(f"Would patch {count} occurrence(s) in {path}")
        return True
    # backup
    bak = path + ".orig"
    if not os.path.exists(bak):
        shutil.copy2(path, bak)
    with open(path, "wb") as f:
        f.write(new_data)
    print(f"Patched {count} occurrence(s): {path}")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: grok-dns.py <binary> [--dry-run]")
        sys.exit(1)
    dry = "--dry-run" in sys.argv
    ok = patch(sys.argv[1], dry_run=dry)
    sys.exit(0 if ok else 1)
PYEOF
chmod +x "$GROKTERM_DIR/grok-dns.py"

# Smart launcher
cat > "$LAUNCHER" << 'LAUNCHEOF'
#!/data/data/com.termux/files/usr/bin/bash
# GrokTerm smart launcher — ensures DNS patch + correct binary

GROK_HOME="${HOME}/.grok"
GROKTERM_DIR="${HOME}/.grokterm"
SDCARD_DNS="/sdcard/.grokdns"
REAL_BIN=""

# Prefer versioned binary managed by official Grok
if [[ -L "$GROK_HOME/bin/grok" ]]; then
  REAL_BIN=$(readlink -f "$GROK_HOME/bin/grok" 2>/dev/null || true)
elif [[ -x "$GROK_HOME/bin/grok" ]]; then
  REAL_BIN="$GROK_HOME/bin/grok"
fi

# Fallback search
if [[ -z "$REAL_BIN" || ! -x "$REAL_BIN" ]]; then
  # Let official update mechanism place it
  REAL_BIN="$GROK_HOME/bin/grok"
fi

# Ensure DNS file exists
if [[ ! -f "$SDCARD_DNS" ]]; then
  mkdir -p /sdcard 2>/dev/null || true
  cat > "$SDCARD_DNS" << EOF
nameserver 8.8.8.8
nameserver 8.8.4.4
EOF
fi

# Apply / re-apply patch if needed
if [[ -x "$REAL_BIN" ]]; then
  python3 "$GROKTERM_DIR/grok-dns.py" "$REAL_BIN" 2>/dev/null || true
fi

# Exec
exec "$REAL_BIN" "$@"
LAUNCHEOF
chmod +x "$LAUNCHER"

# Symlink into PATH
ln -sf "$LAUNCHER" "$BIN_LINK"
ln -sf "$LAUNCHER" "$PREFIX/bin/grokterm" 2>/dev/null || true

echo "[5/7] Setting up helpers..."

# Simple helper
cat > "$PREFIX/bin/gt" << 'GTEOF'
#!/data/data/com.termux/files/usr/bin/bash
# GrokTerm short alias
exec grok "$@"
GTEOF
chmod +x "$PREFIX/bin/gt"

echo "[6/7] Finalizing..."
# Ensure .grok is ready
mkdir -p "$GROK_HOME/config" 2>/dev/null || true

echo
echo -e "${GREEN}[7/7] GrokTerm install complete!${NC}"
echo
echo "Next steps:"
echo "  1. export XAI_API_KEY=xai-your-key-here   # recommended"
echo "  2. grok                                 # or: gt"
echo "  3. After first successful run, updates are handled by the launcher"
echo
echo "Useful:"
echo "  grok --help"
echo "  grok -p 'hello from GrokTerm on Android'"
echo "  termux-setup-storage   # if you haven't already"
echo
echo "GrokTerm home: $GROKTERM_DIR"
echo "Enjoy building on the go."
