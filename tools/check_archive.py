#!/usr/bin/env python3
"""Read-only checks of baseline, archive links and immutable evidence ledger."""
from pathlib import Path
import gzip
import hashlib
import json
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


# Regression guard for the original device capture and preserved old experiments.
gz_path = root / "evidence/device/honor-anb-config.gz"
assert gz_path.is_file(), "Missing original device config.gz"
compressed = gz_path.read_bytes()
assert len(compressed) == 42277, "Original gzip byte size changed"
digest = hashlib.sha256(compressed).hexdigest()
assert digest == "76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3", "Original gzip SHA256 changed"
raw_config = gzip.decompress(compressed)
assert len(raw_config) == 191897, "Original uncompressed Kconfig length changed"
config_items = re.findall(rb"^(?:CONFIG_[A-Z0-9_]+=|# CONFIG_[A-Z0-9_]+ is not set)", raw_config, re.M)
assert len(config_items) == 5672, "Original Kconfig parsed item count changed"
delta = json.loads((root / "evidence/reports/anb-gki-config-delta.json").read_text(encoding="utf-8"))
assert delta["original_symbols"] == 5672 and delta["resolved_symbols"] == 5351
assert delta["removed_count"] == 329 and delta["removed_active_count"] == 208
assert delta["changed_count"] == 5 and delta["added_count"] == 8
assert delta["source_commit_expected"] == "b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e"
assert delta["source_is_honor_production_tree"] is False
assert delta["toolchain_is_device_exact"] is False
triage = json.loads((root / "evidence/reports/anb-vendor-delta-triage.json").read_text(encoding="utf-8"))
assert triage["total"] == 208
assert sum(len(group["keys"]) for group in triage["groups"]) == 208
assert (root / "docs/GKI_SYMBOL_PROVENANCE.md").is_file()
status = (root / "docs/STATUS.md").read_text(encoding="utf-8")
assert "云端实验中；尚无成功结果" not in status, "Stale experimental status returned"
manifest = (root / "docs/EVIDENCE_MANIFEST.md").read_text(encoding="utf-8")
assert "PRIVACY.md" in manifest, "Missing public privacy boundary"
print("PASS: evidence SHA256, 5672 original symbols, pinned GKI delta and 208 triaged entries")


# Regression guard for successfully archived controlled Google GKI experiments.
symbol_report = json.loads((root / "evidence/reports/anb-gki-symbol-definitions.json").read_text(encoding="utf-8"))
clang12_report = json.loads((root / "evidence/reports/anb-gki-clang12-comparison.json").read_text(encoding="utf-8"))
pinned_google_commit = "b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e"
for report in (symbol_report, clang12_report):
    assert report["source_sha"] == pinned_google_commit
    assert report["device_config_sha256"] == digest
    assert report["honor_production_source"] is False
assert symbol_report["kconfig_files_scanned"] == 1580
assert symbol_report["active_removed_count"] == 208
assert symbol_report["declared_anywhere"] == 28
assert symbol_report["not_declared_anywhere"] == 180
assert len(symbol_report["entries"]) == 208
assert clang12_report["original_symbols"] == 5672
assert clang12_report["resolved_symbols"] == 5366
assert clang12_report["changed_count"] == 0
assert clang12_report["removed_count"] == 311
assert clang12_report["removed_active_count"] == 193
assert clang12_report["added_count"] == 5
assert "r416183b" in clang12_report["compiler"] and "12.0.5" in clang12_report["compiler"]
old_missing = {x["symbol"] for x in symbol_report["entries"]}
new_missing = set(clang12_report["removed_active"])
assert len(old_missing - new_missing) == 16
assert new_missing - old_missing == {"CONFIG_AS_HAS_CFI_NEGATE_RA_STATE"}
assert (root / "docs/GKI_SYMBOL_DEFINITIONS.md").is_file()
assert (root / "docs/GKI_CLANG12_COMPARISON.md").is_file()
print("PASS: controlled GKI source definitions 28/180 and exact Clang12 delta 5366, 0, 311, 193, 5")
