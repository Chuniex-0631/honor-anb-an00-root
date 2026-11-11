"""Run presentation-only instrumentation in five display configurations, restoring settings.

Default invocation only reads and prints the device baseline. Install a production-signed
com.anb.root.uitest APK, then use --run for the matrix. Results stay under test-build.
No recovery/verification entry point is called by the instrumentation.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import time
from pathlib import Path

CONFIGURATIONS = (
    ("light-portrait", "no", "1.0", "0"),
    ("dark-portrait", "yes", "1.0", "0"),
    ("light-portrait-font2", "no", "2.0", "0"),
    ("light-landscape", "no", "1.0", "1"),
    ("dark-landscape-font2", "yes", "2.0", "1"),
)
SETTINGS = ("font_scale", "accelerometer_rotation", "user_rotation")
COMPONENT = "com.anb.root.uitest/com.anb.root.uitest.UiAuditInstrumentation"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", help="Optional device selector; never written to reports")
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--run", action="store_true", help="Run five configurations, then restore settings")
    action.add_argument("--restore-state", type=Path, help="Restore a previously captured baseline JSON")
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parent.parent / "test-build/device-ui")
    args = parser.parse_args()
    command = [args.adb] + (["-s", args.serial] if args.serial else [])

    def adb(*parts: str, timeout: int = 30) -> str:
        result = subprocess.run(command + list(parts), capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"ADB command failed ({result.returncode}): {' '.join(parts)}: {result.stderr.strip()}")
        return result.stdout.strip()

    def restore(snapshot: dict) -> None:
        failures = []
        # Attempt each restoration even if another command fails (for example after disconnect).
        for key in SETTINGS:
            try:
                value = snapshot[key]
                if value == "null":
                    adb("shell", "settings", "delete", "system", key)
                else:
                    adb("shell", "settings", "put", "system", key, str(value))
            except Exception as error:
                failures.append(str(error))
        try:
            adb("shell", "cmd", "uimode", "night", snapshot["night_mode"])
        except Exception as error:
            failures.append(str(error))
        if failures:
            raise RuntimeError("Device configuration restoration incomplete: " + "; ".join(failures))

    adb("get-state")
    if args.restore_state:
        restore(json.loads(args.restore_state.read_text(encoding="utf-8")))
        print("Device display settings restored.", flush=True)
        return
    baseline = {key: adb("shell", "settings", "get", "system", key) for key in SETTINGS}
    night = adb("shell", "cmd", "uimode", "night")
    match = re.search(r"Night mode:\s*(auto|yes|no|custom)\b", night)
    if not match:
        raise RuntimeError("Unrecognized night mode; refuse changes without an exact restoration value")
    baseline.update(night_mode=match.group(1), size=adb("shell", "wm", "size"), density=adb("shell", "wm", "density"))
    print(json.dumps({"baseline": baseline, "configurations": [row[0] for row in CONFIGURATIONS], "read_only": not args.run}, ensure_ascii=False), flush=True)
    if not args.run:
        return
    installed_instrumentation=adb("shell", "pm", "list", "instrumentation")
    if COMPONENT not in installed_instrumentation and "com.anb.root.uitest/.UiAuditInstrumentation" not in installed_instrumentation:
        raise RuntimeError("Install the production-signed com.anb.root.uitest APK first")
    args.output.mkdir(parents=True, exist_ok=True)
    state_file = args.output / "baseline.json"
    state_file.write_text(json.dumps(baseline, indent=2), encoding="utf-8")
    results = []
    try:
        for name, night_mode, font, rotation in CONFIGURATIONS:
            adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
            adb("shell", "settings", "put", "system", "user_rotation", rotation)
            adb("shell", "settings", "put", "system", "font_scale", font)
            adb("shell", "cmd", "uimode", "night", night_mode)
            time.sleep(1)
            output = adb("shell", "am", "instrument", "-w", "-e", "expected_night", night_mode,
                         "-e", "expected_font", font, "-e", "expected_rotation", rotation, COMPONENT, timeout=150)
            (args.output / f"{name}.txt").write_text(output + "\n", encoding="utf-8")
            passed = re.search(r"PASS: (\d+) production Activity assertions", output)
            if not passed or "FAIL:" in output or "INSTRUMENTATION_FAILED" in output:
                raise RuntimeError(f"UI test failed for {name}; inspect {name}.txt")
            row = {"configuration": name, "assertions": int(passed.group(1)), "result": "passed"}
            results.append(row)
            print(json.dumps(row), flush=True)
    finally:
        try:
            restore(baseline)
            print("Restored the original font, rotation, and night-mode preferences.", flush=True)
        finally:
            (args.output / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"PASS: {sum(row['assertions'] for row in results)} assertions across {len(results)} configurations", flush=True)


if __name__ == "__main__":
    main()
