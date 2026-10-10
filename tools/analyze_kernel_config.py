#!/usr/bin/env python3
"""Read-only parser for user-supplied ANB-AN00 kernel config.gz."""
import gzip, hashlib, re, sys
from pathlib import Path
source=Path("evidence/device/honor-anb-config.gz")
assert source.exists(), "Config not uploaded to GitHub yet"
raw=source.read_bytes()
assert raw[:2] == bytes([31,139]), "GZIP signature mismatch"
data=gzip.decompress(raw)
assert len(data)<8000000, "Unexpectedly large extracted file"
text=data.decode("utf-8")
assert "\x00" not in text
config={}
for line in text.splitlines():
    m=re.fullmatch(r"(CONFIG_[A-Z0-9_]+)=(.*)",line)
    if m: key,val=m.groups()
    else:
        m=re.fullmatch(r"# (CONFIG_[A-Z0-9_]+) is not set",line)
        if not m: continue
        key,val=m.group(1),"n"
    assert key not in config, "Duplicate config symbol: "+key
    config[key]=val
assert len(config)>2000, "Unexpectedly short or invalid config"
keys="""CONFIG_FUTEX CONFIG_FUTEX_PI CONFIG_RT_MUTEXES CONFIG_CFI_CLANG CONFIG_CFI_PERMISSIVE CONFIG_LTO_CLANG CONFIG_LTO_CLANG_THIN CONFIG_LTO_CLANG_FULL CONFIG_RANDOMIZE_BASE CONFIG_STACKPROTECTOR_STRONG CONFIG_SHADOW_CALL_STACK CONFIG_ARM64_PTR_AUTH CONFIG_ARM64_BTI CONFIG_HARDENED_USERCOPY CONFIG_FORTIFY_SOURCE CONFIG_SECURITY_SELINUX CONFIG_SECCOMP CONFIG_PANIC_ON_OOPS CONFIG_IKCONFIG CONFIG_IKCONFIG_PROC CONFIG_KALLSYMS CONFIG_DEBUG_INFO CONFIG_FTRACE CONFIG_KPROBES CONFIG_KASAN CONFIG_MODULES CONFIG_MODVERSIONS CONFIG_MODULE_SIG CONFIG_BPF CONFIG_BPF_SYSCALL CONFIG_BPF_JIT CONFIG_ARCH_QCOM CONFIG_ARCH_LAHAINA CONFIG_ARCH_TARO""".split()
counts={v:sum(x==v for x in config.values()) for v in ("y","m","n")}
sha=hashlib.sha256(raw).hexdigest()
rows=["# 实机 ANB-AN00 内核配置审计","","本报告由 GitHub Actions 读取真实 config.gz 后自动生成。仅证明配置项，不证明漏洞可利用或 Bootloader 解锁。","","- GZIP 文件大小："+str(len(raw))+" bytes","- 解压后："+str(len(data))+" bytes","- SHA-256："+sha,"- CONFIG 配置总数："+str(len(config)),"- 状态计数：y="+str(counts["y"])+", m="+str(counts["m"])+", n="+str(counts["n"]),"","| 配置符号 | 实机状态 |","|---|---|"]
for key in keys:
    val=config.get(key)
    state=val if val in ("y","m","n") else ("未出现" if val is None else "非布尔值（省略）")
    rows.append("| "+key+" | "+state+" |")
rows+=["","未出现的键并不等同于 n。其它机型源码不能当作本机私有固件。"]
Path("docs/KERNEL_CONFIG_REPORT.md").write_text("\n".join(rows)+"\n",encoding="utf-8")
print("PASS: "+str(len(config))+" kernel options, sha256="+sha)
