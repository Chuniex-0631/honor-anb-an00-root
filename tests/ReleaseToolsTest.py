"""Exercise APK normalization with malformed and ordinary SDK archives."""
import subprocess
import sys
import tempfile
import unittest
import warnings
import zipfile
from pathlib import Path


class ReleaseToolsTest(unittest.TestCase):
    def normalize(self, entries):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "source.apk"
            target = Path(directory) / "target.apk"
            with zipfile.ZipFile(source, "w") as archive:
                for name, data in entries:
                    with warnings.catch_warnings():
                        warnings.simplefilter("ignore", UserWarning)
                        archive.writestr(name, data)
            command = Path(__file__).resolve().parents[1] / "tools/normalize_apk.py"
            result = subprocess.run([sys.executable, str(command), str(source), str(target)], capture_output=True)
            normalized = None
            if result.returncode == 0:
                with zipfile.ZipFile(target) as archive:
                    normalized = {entry.filename: (archive.read(entry), entry.compress_type) for entry in archive.infolist()}
            return result, normalized

    def test_sdk_entries_preserve_bytes_and_store_resources(self):
        result, entries = self.normalize([("assets\\payload\\data", b"binary\x00"), ("resources.arsc", b"resources")])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(entries["assets/payload/data"][0], b"binary\x00")
        self.assertEqual(entries["resources.arsc"], (b"resources", zipfile.ZIP_STORED))

    def test_normalized_name_collision_is_rejected(self):
        result, _ = self.normalize([("assets\\data", b"one"), ("assets/data", b"two")])
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(b"duplicate APK entry", result.stderr)

    def test_unsafe_names_are_rejected(self):
        for name in ("../outside", "assets/../outside", "/absolute", "C:/absolute"):
            with self.subTest(name=name):
                result, _ = self.normalize([(name, b"data")])
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(b"Unsafe", result.stderr)


if __name__ == "__main__":
    unittest.main()
