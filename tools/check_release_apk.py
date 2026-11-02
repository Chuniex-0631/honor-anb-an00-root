"""Verify that a signed release preserves the exact unsigned build and source assets.

Cryptographic signature/certificate verification is performed separately by apksigner.
"""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("apk", type=Path)
parser.add_argument("--unsigned", type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
current = json.loads((root / "release/current.json").read_text(encoding="utf-8"))
metadata = json.loads((root / current["metadata"]).read_text(encoding="utf-8"))
assert args.apk.name == metadata["artifact"]
assert hashlib.sha256(args.apk.read_bytes()).hexdigest() == metadata["sha256"]
with zipfile.ZipFile(args.apk) as signed, zipfile.ZipFile(args.unsigned) as unsigned:
    assert len(signed.namelist()) == len(set(signed.namelist())), "Duplicate archive entries"
    for name in unsigned.namelist():
        assert signed.read(name) == unsigned.read(name), f"Signed build changed: {name}"
    for name in set(signed.namelist()) - set(unsigned.namelist()):
        assert name.startswith("META-INF/"), f"Unexpected signed entry: {name}"
    for path in (root / "app/assets").rglob("*"):
        if path.is_file():
            name = "assets/" + path.relative_to(root / "app/assets").as_posix()
            assert signed.read(name) == path.read_bytes(), f"Source asset mismatch: {name}"
    dex = signed.read("classes.dex")
    for fixture in (b"EngineIntegrationTest", b"ServiceRegressionTest", b"AuditRegressionTest", b"UiAuditInstrumentation"):
        assert fixture not in dex, "Test fixture packaged into production"
    assert b"Lcom/anb/root/DeviceProfile;" in dex and b"Lcom/anb/root/TaskGate;" in dex
print("PASS: release hash, every unsigned ZIP entry unchanged, all source assets, no test fixtures in production")
