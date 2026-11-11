"""Normalize SDK ZIP separators and reject ambiguous APK entry names."""
import sys
import zipfile
from pathlib import PurePosixPath

source, target = sys.argv[1:]
with zipfile.ZipFile(source) as src, zipfile.ZipFile(target, "w") as dest:
    seen = set()
    for entry in src.infolist():
        name = entry.filename.replace("\\", "/")
        if not name or name.startswith("/") or ".." in PurePosixPath(name).parts or ":" in name or name in seen:
            raise ValueError(f"Unsafe or duplicate APK entry: {name}")
        seen.add(name)
        compression = zipfile.ZIP_STORED if name == "resources.arsc" else zipfile.ZIP_DEFLATED
        dest.writestr(name, src.read(entry), compress_type=compression)
