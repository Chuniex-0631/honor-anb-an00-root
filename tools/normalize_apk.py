import sys
import zipfile

source, target = sys.argv[1:]
with zipfile.ZipFile(source) as src, zipfile.ZipFile(target, "w") as dest:
    for entry in src.infolist():
        name = entry.filename.replace("\\", "/")
        compression = zipfile.ZIP_STORED if name == "resources.arsc" else zipfile.ZIP_DEFLATED
        dest.writestr(name, src.read(entry), compress_type=compression)
