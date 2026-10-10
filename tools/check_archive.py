#!/usr/bin/env python3
"""Read-only evidence and Markdown link check for the public research archive."""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
raw = root / "evidence/device/honor80pro-flat-baseline.txt"
assert raw.is_file(), "Missing ADB baseline"
data = raw.read_text(encoding="utf-8")
for phrase in (
    "ro.product.model = ANB-AN00",
    "ro.product.device = HNANB",
    "ro.board.platform = taro",
    "ro.build.version.release = 15",
    "ro.boot.flash.locked = 1",
    "ro.boot.verifiedbootstate = green",
    "5.10.236-android12-9-gd671cf8c2b02",
    "clang version 12.0.5",
    "/proc/config.gz",
):
    assert phrase in data, f"Missing expected evidence: {phrase}"
assert not re.search(r"(?:ro\.serialno|ro\.boot\.serialno|imei\s*[=:]|imsi\s*[=:]|iccid\s*[=:])", data, re.I)
markdown = list(root.rglob("*.md"))
checked = 0
bad = []
for path in markdown:
    content = path.read_text(encoding="utf-8")
    for link in re.findall(r"!?\[[^\]]*\]\(([^)]+)\)", content):
        url = link.strip().split(" ", 1)[0].strip("<>")
        if not url or url.startswith(("http://", "https://", "#", "mailto:", "data:")):
            continue
        relative = url.split("#")[0].split("?")[0]
        checked += 1
        if not (path.parent / relative).exists():
            bad.append((str(path.relative_to(root)), relative))
assert not bad, f"Broken local links: {bad}"
print(f"PASS: {len(markdown)} Markdown files, {checked} links checked; model and OS baseline verified")
