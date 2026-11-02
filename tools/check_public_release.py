"""Check public source integrity and prevent accidentally publishing private files.

The release is described by release/current.json; this checker never hard-codes a version.
Two invariants are enforced separately:

* the payload and the AIDL interface are frozen at the 2.1.0 bytes -- they are the inputs the
  device-verified recovery chain depends on;
* the current version's core manifest must match the tree, so any edit to the execution core
  is an explicit, reviewed change rather than a silent drift.
"""
from pathlib import Path
import hashlib
import json
import re

root = Path(__file__).resolve().parent.parent
FROZEN_PREFIXES = ("app/assets/payload/", "app/aidl/")

current = json.loads((root / "release/current.json").read_text())
version = current["version"]
metadata_path = root / current["metadata"]
core_path = root / current["core"]
assert metadata_path.is_file(), f"Missing release metadata: {current['metadata']}"
assert core_path.is_file(), f"Missing core manifest: {current['core']}"

release = json.loads(metadata_path.read_text())
assert release["version"] == version, "current.json and release metadata disagree"
assert release["model"] == "ANB-AN00"
assert release["transport"] == "Shizuku UserService" and not release["persistent_root"]
assert release["artifact"] == f"ANB-Root-{version}.apk", "artifact name must carry the version"
assert not release["allow_shell_default"] and release["allow_shell_change_requires_full_reboot"]
assert release["sdk"] == 35 and release["firmware_display"] == "ANB-AN00 9.0.0.230(CHNC00E230R2P5)"
assert isinstance(release["device_verified"], bool)
assert release["device_verified"] or release.get("requires_device_reverification") is True
assert re.fullmatch(r"[0-9a-f]{64}", release["signing_certificate_sha256"])

status = release.get("status", "released")
if status == "released":
    assert re.fullmatch(r"[0-9a-f]{64}", release.get("sha256") or ""), "a released artifact needs its SHA256"
    assert release["sha256"] != "0" * 64
else:
    assert status == "candidate", f"unknown release status: {status}"
    if release.get("sha256"):
        assert re.fullmatch(r"[0-9a-f]{64}", release["sha256"]) and release["sha256"] != "0" * 64, "invalid signed candidate hash"
    assert release.get("requires_device_reverification") is True or release.get("device_verified") is True, \
        "a candidate must be either flagged for device reverification or already device verified"

core = json.loads(core_path.read_text())
for name, expected in core.items():
    assert hashlib.sha256((root / name).read_bytes()).hexdigest() == expected, f"Core changed: {name}"

baseline = json.loads((root / "release/v2.1.0-core-files.json").read_text())
assert set(baseline) <= set(core), "the version manifest must keep every frozen input"
for name, expected in baseline.items():
    if not name.startswith(FROZEN_PREFIXES):
        continue
    assert name in core, f"Frozen input dropped: {name}"
    assert core[name] == expected, f"Payload or AIDL changed since 2.1.0: {name}"

manifest = (root / "app/AndroidManifest.xml").read_text()
assert "android.permission.INTERNET" not in manifest
assert 'android:debuggable="false"' in manifest
assert "INTERACT_ACROSS_USERS_FULL" in manifest
assert not (root / "app/java/com/anb/root/AdbClient.java").exists()

patterns = {
    "private key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "access token": re.compile(r"(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})"),
    "operator Windows home": re.compile(r"[A-Za-z]:[\\/](?:Users|Documents and Settings)[\\/][^\s<>\"]+", re.I),
    "operator Unix home": re.compile(r"/(?:Users|home/(?!runner(?:/|$)))[^\s<>\"]+"),
    "private mail ID": re.compile(r"(?:message|消息)\s*ID\s*[:：]?\s*`?[0-9a-f]{16}", re.I),
    "device identifier": re.compile(r"(?:ro\.serialno|ro\.boot\.serialno|IMEI|MEID)\s*[:=]\s*[A-Za-z0-9]{8,}", re.I),
}
excluded = {".git", "build", "test-build", "sdk", "backups", "private", "__pycache__"}
failures = []
for path in root.rglob("*"):
    if not path.is_file() or any(part in excluded for part in path.relative_to(root).parts):
        continue
    if path.suffix.lower() in {".jks", ".keystore", ".pem", ".key", ".apk"}:
        failures.append((str(path.relative_to(root)), "forbidden public file"))
    if path.suffix.lower() not in {".java", ".aidl", ".md", ".txt", ".json", ".yml", ".yaml", ".ps1", ".py"}:
        continue
    text = path.read_text(encoding="utf-8-sig")
    for label, pattern in patterns.items():
        if pattern.search(text):
            failures.append((str(path.relative_to(root)), label))
assert not failures, f"Public privacy check failed: {failures}"
assert (root / "docs/assets/social-preview.png").is_file()
print(f"PASS: {version} ({status}), {len(core)} core inputs, payload and AIDL frozen at 2.1.0, permissions, privacy")
