#!/usr/bin/env python3
"""GrokTerm DNS path patcher for static musl Grok binary on Android/Termux."""
import sys
import os
import shutil

ORIGINAL = b"/etc/resolv.conf"
REPLACEMENT = b"/sdcard/.grokdns"  # exactly 16 bytes

assert len(ORIGINAL) == 16, "ORIGINAL must be 16 bytes"
assert len(REPLACEMENT) == 16, "REPLACEMENT must be 16 bytes"

def patch(path: str, dry_run: bool = False) -> bool:
    if not os.path.isfile(path):
        print(f"Not a file: {path}")
        return False
    with open(path, "rb") as f:
        data = bytearray(f.read())
    if ORIGINAL not in data:
        if REPLACEMENT in data:
            print(f"Already patched: {path}")
            return True
        print(f"Target string not found (binary may have changed upstream): {path}")
        return False
    count = data.count(ORIGINAL)
    new_data = data.replace(ORIGINAL, REPLACEMENT)
    if dry_run:
        print(f"Would patch {count} occurrence(s) in {path}")
        return True
    bak = path + ".orig"
    if not os.path.exists(bak):
        shutil.copy2(path, bak)
        print(f"Backup created: {bak}")
    with open(path, "wb") as f:
        f.write(new_data)
    os.chmod(path, 0o755)
    print(f"Patched {count} occurrence(s): {path}")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: grok-dns.py <path-to-grok-binary> [--dry-run]")
        sys.exit(1)
    dry = "--dry-run" in sys.argv
    success = patch(sys.argv[1], dry_run=dry)
    sys.exit(0 if success else 1)
