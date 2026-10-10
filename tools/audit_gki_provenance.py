#!/usr/bin/env python3
"""Analyze 208 active Kconfig entries absent after public-GKI olddefconfig.
Do not infer OEM code identity, vulnerability, root, or bootloader status.
"""
import argparse
import json
import re
import subprocess
from pathlib import Path

DEFINE = re.compile(r"^\s*(?:menuconfig|config)\s+([A-Za-z0-9_]+)\b")
SET = re.compile(r"^(CONFIG_[A-Za-z0-9_]+)=(.*)$")
UNSET = re.compile(r"^# (CONFIG_[A-Za-z0-9_]+) is not set$")


def parse_config(path):
    config = {}
    for line in Path(path).read_text(errors="replace").splitlines():
        m = SET.fullmatch(line)
        if m:
            key, val = m.groups()
        else:
            m = UNSET.fullmatch(line)
            if not m:
                continue
            key, val = m.group(1), "n"
        if key in config:
            raise ValueError("duplicate " + key)
        config[key] = val
    if len(config) < 2000:
        raise ValueError("too few symbols " + str(path))
    return config


def main():
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=("symbols", "toolchain"))
    for k in ("source", "delta", "original", "output", "markdown"):
        p.add_argument("--" + k, required=True)
    p.add_argument("--resolved")
    p.add_argument("--version-file")
    a = p.parse_args()
    source = Path(a.source)
    commit = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
    expected = "b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e"
    if commit != expected:
        raise ValueError("Source ref mismatch " + commit)
    delta = json.loads(Path(a.delta).read_text())
    original = parse_config(a.original)
    assert len(original) == 5672 and delta["source_commit_expected"] == expected
    active = sorted(x["key"] for x in delta["removed"] if x["device"] in ("y", "m"))
    assert len(active) == 208
    payload = {
        "device_config_sha256": "76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3",
        "reference": "android12-5.10-2025-05_r1",
        "source_sha": commit,
        "honor_production_source": False,
        "mode": a.mode,
    }
    if a.mode == "symbols":
        declared = {}
        files = 0
        for path in sorted(source.rglob("Kconfig*")):
            if not path.is_file() or ".git" in path.parts:
                continue
            files += 1
            for line in path.read_text(errors="replace").splitlines():
                m = DEFINE.match(line)
                if m:
                    declared.setdefault("CONFIG_" + m.group(1), set()).add(str(path.relative_to(source)))
        entries = [{"symbol": k, "definition_paths": sorted(declared.get(k, []))}
                   for k in active]
        found = sum(bool(x["definition_paths"]) for x in entries)
        payload.update({
            "kconfig_files_scanned": files, "active_removed_count": 208,
            "declared_anywhere": found, "not_declared_anywhere": 208 - found,
            "entries": entries,
            "caveat": "Definition anywhere does not guarantee reachability in ARCH=arm64 Kconfig.",
        })
        lines = ["# Google GKI 参考源码符号定义扫描", "",
                 f"固定源码 `{commit}`；共扫描 {files} 个 Kconfig 文件。",
                 f"原始启用而旧求解结果缺失 208 项中：全树有定义 **{found}**，全树未找到定义 **{208-found}**。",
                 "有定义不代表当前 ARM64 Kconfig 图可达；无定义不证明荣耀独家拥有该符号。", "",
                 "## 有定义（但旧求解结果缺失）", ""]
        lines += [f"- `{x['symbol']}`: {', '.join(x['definition_paths'][:3])}"
                  for x in entries if x["definition_paths"]]
        lines += ["", "## 未找到定义", ""]
        lines += [f"- `{x['symbol']}`" for x in entries if not x["definition_paths"]]
    else:
        if not a.resolved or not a.version_file:
            raise ValueError("resolved and version-file required")
        version = Path(a.version_file).read_text()
        if "r416183b" not in version or "12.0.5" not in version:
            raise ValueError("Compiler identity mismatch " + version)
        resolved = parse_config(a.resolved)
        before, after = set(original), set(resolved)
        changed = sorted(k for k in before & after if original[k] != resolved[k])
        removed = sorted(before - after)
        added = sorted(after - before)
        active_removed = [k for k in removed if original[k] in ("y", "m")]
        payload.update({
            "compiler": version, "method": "ARCH=arm64 LLVM=1 LLVM_IAS=1 olddefconfig",
            "original_symbols": len(original), "resolved_symbols": len(resolved),
            "changed_count": len(changed), "removed_count": len(removed),
            "removed_active_count": len(active_removed), "added_count": len(added),
            "changed": [{"symbol": k, "device": original[k], "candidate": resolved[k]} for k in changed],
            "removed_active": active_removed,
            "previous_runner_clang14_removed_active": delta["removed_active_count"],
            "caveat": "Toolchain matching does not mean kernel source or binary matching.",
        })
        lines = ["# Android Clang r416183b Google GKI 对照", "",
                 f"固定源码：`{commit}`。",
                 f"Clang: `{version.splitlines()[0]}`。",
                 f"原始配置 {len(original)}；重新求解 {len(resolved)}；共享值变化 {len(changed)}；缺失 {len(removed)}（启用 {len(active_removed)}）；新增 {len(added)}。",
                 f"旧 Ubuntu Clang14 对照中原已启用而缺失：{delta['removed_active_count']}。",
                 "", "## 取值变动", ""]
        lines += [f"- `{k}`: `{original[k]}` → `{resolved[k]}`" for k in changed]
        lines += ["", "仅公共 GKI 控制实验，不证明 HONOR 生产内核代码、Root 或 Bootloader 解锁。"]
    out = Path(a.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")
    md = Path(a.markdown)
    md.parent.mkdir(parents=True, exist_ok=True)
    md.write_text("\n".join(lines) + "\n")
    print(f"PASS {a.mode} {commit}")


if __name__ == "__main__":
    main()
