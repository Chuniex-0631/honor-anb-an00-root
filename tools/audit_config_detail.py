#!/usr/bin/env python3
"""Offline, source-grounded profile for the owner's actual ANB-AN00 config.gz.

Outputs only selected Kconfig states; doesn't expose extra device identifiers.
CONFIG_ symbols that are absent are NOT automatically disabled.
"""
from pathlib import Path
import gzip
import hashlib
import json
import re

CONFIG_PATH = Path("evidence/device/honor-anb-config.gz")
REPORT = Path("docs/KERNEL_SECURITY_PROFILE.md")
JSON_OUT = Path("evidence/reports/anb-config-audit.json")

GROUPS = {
    "Futex and locking - prerequisite, not proof of vulnerability": [
        "CONFIG_FUTEX", "CONFIG_FUTEX_PI", "CONFIG_RT_MUTEXES",
        "CONFIG_PI_FUTEX", "CONFIG_PREEMPT", "CONFIG_PREEMPT_VOLUNTARY",
        "CONFIG_PREEMPT_NONE", "CONFIG_PANIC_ON_OOPS",
    ],
    "Compiler and memory integrity hardening": [
        "CONFIG_CFI_CLANG", "CONFIG_CFI_PERMISSIVE", "CONFIG_LTO_CLANG",
        "CONFIG_LTO_CLANG_FULL", "CONFIG_LTO_CLANG_THIN",
        "CONFIG_SHADOW_CALL_STACK", "CONFIG_ARM64_PTR_AUTH",
        "CONFIG_ARM64_BTI", "CONFIG_RANDOMIZE_BASE",
        "CONFIG_STACKPROTECTOR", "CONFIG_STACKPROTECTOR_STRONG",
        "CONFIG_HARDENED_USERCOPY", "CONFIG_FORTIFY_SOURCE",
        "CONFIG_STRICT_KERNEL_RWX", "CONFIG_STRICT_MODULE_RWX",
        "CONFIG_ARM64_PAN", "CONFIG_ARM64_UAO",
    ],
    "Isolation and kernel interfaces": [
        "CONFIG_SECURITY", "CONFIG_SECURITY_SELINUX",
        "CONFIG_SECURITY_SELINUX_DEVELOP", "CONFIG_SECCOMP",
        "CONFIG_SECCOMP_FILTER", "CONFIG_BPF", "CONFIG_BPF_SYSCALL",
        "CONFIG_BPF_JIT", "CONFIG_BPF_JIT_ALWAYS_ON",
        "CONFIG_BPF_UNPRIV_DEFAULT_OFF", "CONFIG_USER_NS",
        "CONFIG_NAMESPACES", "CONFIG_ANDROID_BINDER_IPC",
        "CONFIG_SECURITY_DMESG_RESTRICT", "CONFIG_DEVMEM",
    ],
    "Introspection and kernel metadata": [
        "CONFIG_IKCONFIG", "CONFIG_IKCONFIG_PROC",
        "CONFIG_KALLSYMS", "CONFIG_KALLSYMS_ALL",
        "CONFIG_FTRACE", "CONFIG_FUNCTION_TRACER",
        "CONFIG_KPROBES", "CONFIG_KPROBE_EVENTS",
        "CONFIG_DEBUG_FS", "CONFIG_DEBUG_INFO",
        "CONFIG_DEBUG_INFO_DWARF4", "CONFIG_DEBUG_INFO_BTF",
        "CONFIG_KASAN", "CONFIG_KCSAN",
    ],
    "Modules and device platform": [
        "CONFIG_MODULES", "CONFIG_MODVERSIONS",
        "CONFIG_MODULE_SIG", "CONFIG_MODULE_SIG_FORCE",
        "CONFIG_MODULE_UNLOAD", "CONFIG_MODULE_COMPRESS",
        "CONFIG_ARCH_QCOM", "CONFIG_ARCH_TARO",
        "CONFIG_ARCH_LAHAINA", "CONFIG_ARCH_HOLI",
        "CONFIG_ARM64", "CONFIG_SMP",
    ],
}
def load():
    raw = CONFIG_PATH.read_bytes()
    assert raw.startswith(b"\x1f\x8b"), "Not a gzip file"
    data = gzip.decompress(raw)
    s = data.decode("utf-8")
    assert "\x00" not in s, "Unexpected NUL"
    result = {}
    for line in s.splitlines():
        matched = re.fullmatch(r"(CONFIG_[A-Z0-9_]+)=(.*)", line)
        if matched:
            k, v = matched.groups()
        else:
            matched = re.fullmatch(r"# (CONFIG_[A-Z0-9_]+) is not set", line)
            if matched is None: continue
            k,v = matched.group(1),"n"
        if k in result: raise ValueError("Duplicate Kconfig key: "+k)
        result[k]=v
    assert len(result)>4000, "Unexpectedly incomplete Kconfig"
    return raw,data,result

def state(values,key):
    if key not in values: return "not present"
    if values[key] in ("y","m","n"): return values[key]
    return "value present (redacted)"

def main():
    raw,uncompressed,values=load()
    totals={tag:sum(1 for x in values.values() if x==tag) for tag in ("y","m","n")}
    totals["other"]=len(values)-sum(totals.values())
    expected_hash="76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3"
    h=hashlib.sha256(raw).hexdigest()
    assert h==expected_hash, "Config bytes changed; investigate source before updating expected hash"
    assert len(raw)==42277 and len(uncompressed)==191897
    selected={k:state(values,k) for keys in GROUPS.values() for k in keys}
    payload={
        "source":"evidence/device/honor-anb-config.gz",
        "device":"HONOR ANB-AN00 / HNANB / firmware 9.0.0.230(CHNC00E230R2P5)",
        "source_sha256":h,
        "compressed_bytes":len(raw), "uncompressed_bytes":len(uncompressed),
        "symbols":len(values), "counts":totals,
        "groups":GROUPS, "selected_states":selected,
        "note":"Configuration values do not verify live code, security patch state, reachable exploit path, root or bootloader unlock."
    }
    JSON_OUT.parent.mkdir(parents=True,exist_ok=True)
    JSON_OUT.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    lines=[
        "# ANB-AN00 内核配置专题：实机证据与安全边界",
        "",
        "> 2026-10-11 | 基于本仓库真实 gzip 文件，由 GitHub Actions 自动解析；不是其他机型的 Kconfig。",
        "",
        "- 设备：HONOR ANB-AN00 / HNANB；MagicOS 9.0.0.230，Android 15；Linux 5.10.236。",
        f"- 原始 gzip：**{len(raw)} B**；解压后：**{len(uncompressed)} B**；SHA-256：`{h}`。",
        f"- 配置总数：**{len(values)}**；`y={totals['y']}`、`m={totals['m']}`、`n={totals['n']}`、其它非布尔值 `{totals['other']}`。",
        "- `not present` 表示该名称不在配置中，**不等于** `n`；非布尔值不向公共报告复制具体字符串。",
        "",
    ]
    for name,keys in GROUPS.items():
        lines += [f"## {name}","","| Kconfig key | 实际状态 |","|---|---|"]
        for key in keys: lines.append(f"| `{key}` | `{selected[key]}` |")
        lines.append("")
    lines += [
        "## 研究结论（严格限定范围）","",
        "- 配置中是否开启 Futex PI、RT Mutex 是判断候选漏洞的**必要前提之一**，不是漏洞存在或能利用的证明。必须取得与此固件匹配的内核源码和补丁证据。",
        "- CFI、Full LTO、Shadow Call Stack、指针认证、BTI、KASLR、SELinux 等开关只描述**编译配置**，不能从此推导每个内核运行路径的保护是否可绕过。",
        "- `CONFIG_PANIC_ON_OOPS=y` 时，未验证的内核崩溃触发测试可能造成系统重启或不稳定。优先进行离线研究。",
        "- `CONFIG_MODULES=y` 与配置中 `=m` 计数为 0 可以同时成立，不能推断设备不存在动态加载的 vendor 模块。",
        "- `CONFIG_KALLSYMS` / `CONFIG_KPROBES` / `CONFIG_FTRACE` 被设置不代表非 Root ADB shell 一定能够读取受限制的信息。",
        "- 目前**没有真实原厂 boot.img、vmlinux、System.map 或本机可用的 CVE 利用证据**；Root 和 Bootloader 均未解锁。",
        "",
        "## 后续建议","",
        "1. 获取本机固件完全匹配的 HONOR 内核源码和构建描述，比较源码身份、修复补丁、具体 Clang 12.0.5 工具链。",
        "2. 对任何候选 CVE，先确认源码位置、修补状态、编译开关和运行触发条件，再决定是否值得进一步验证。",
        "3. 保留完整原始 gzip 文件而不是依赖文字统计；`evidence/reports/anb-config-audit.json` 提供自动化的已脱敏精选开关摘要。",
        "",
    ]
    REPORT.write_text("\n".join(lines),encoding="utf-8")
    print(f"Deep audit PASS: {len(values)} flags; raw {len(raw)}B; sha256={h}")
if __name__=="__main__": main()
