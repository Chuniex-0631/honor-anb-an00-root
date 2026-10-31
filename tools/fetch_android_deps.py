"""Download pinned public Android dependencies; verify extracted JAR hashes."""
from pathlib import Path
from urllib.request import urlopen
import hashlib
import io
import json
import zipfile

root = Path(__file__).resolve().parent.parent
destination = root / "deps"
destination.mkdir(exist_ok=True)
lock = {row["file"]: row["sha256"] for row in json.loads((destination / "dependencies.lock.json").read_text())}
for name in ("api", "provider", "aidl", "shared", "annotation"):
    path = destination / f"{name}.jar"
    expected = lock[path.name]
    if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() == expected:
        continue
    if name == "annotation":
        url = "https://dl.google.com/dl/android/maven2/androidx/annotation/annotation/1.3.0/annotation-1.3.0.jar"
        data = urlopen(url, timeout=60).read()
    else:
        url = f"https://repo.maven.apache.org/maven2/dev/rikka/shizuku/{name}/13.1.5/{name}-13.1.5.aar"
        with zipfile.ZipFile(io.BytesIO(urlopen(url, timeout=60).read())) as archive:
            data = archive.read("classes.jar")
    if hashlib.sha256(data).hexdigest() != expected:
        raise SystemExit(f"Dependency hash mismatch: {name}")
    path.write_bytes(data)
    print(f"Verified {path.name}")
