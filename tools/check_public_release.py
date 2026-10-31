"""Check public source integrity and prevent accidentally publishing private files."""
from pathlib import Path
import hashlib
import json
import re

root = Path(__file__).resolve().parent.parent
release = json.loads((root / "release/v2.1.0.json").read_text())
assert release["version"] == "2.1.0" and release["model"] == "ANB-AN00"
assert release["transport"] == "Shizuku UserService" and not release["persistent_root"]
assert len(release["sha256"]) == 64
core = json.loads((root / "release/core-files.json").read_text())
for name, expected in core.items():
    assert hashlib.sha256((root / name).read_bytes()).hexdigest() == expected, f"Core changed: {name}"
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
    "private mail ID": re.compile(r"(?:消息|message)\s*ID\s*[:：]?\s*`?[0-9a-f]{16}", re.I),
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
print(f"PASS: {len(core)} core inputs, release metadata, permissions, privacy and public assets")
