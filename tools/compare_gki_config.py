#!/usr/bin/env python3
"""ANB Kconfig vs pinned public Google GKI source: compare, never infer binary identity."""
import hashlib,json,re,sys
from pathlib import Path
def parse(path):
    ret={}
    for line in Path(path).read_text(errors="replace").splitlines():
        m=re.fullmatch(r"(CONFIG_[A-Z0-9_]+)=(.*)",line)
        if m: k,v=m.groups()
        else:
            m=re.fullmatch(r"# (CONFIG_[A-Z0-9_]+) is not set",line)
            if not m: continue
            k,v=m.group(1),"n"
        if k in ret: raise ValueError("Duplicate Kconfig symbol: "+k)
        ret[k]=v
    if len(ret)<2000: raise ValueError("Incomplete config: "+str(path))
    return ret
oldpath,newpath,reportpath,docpath=sys.argv[1:]
a,b=parse(oldpath),parse(newpath)
common=set(a)&set(b)
removed=sorted(set(a)-set(b))
added=sorted(set(b)-set(a))
changed=sorted(k for k in common if a[k]!=b[k])
removed_active=[k for k in removed if a[k] in ("y","m")]
added_active=[k for k in added if b[k] in ("y","m")]
changed_active=[k for k in changed if a[k] in ("y","m") or b[k] in ("y","m")]
keyset=[
    "CONFIG_FUTEX","CONFIG_FUTEX_PI","CONFIG_RT_MUTEXES",
    "CONFIG_CFI_CLANG","CONFIG_LTO_CLANG_FULL","CONFIG_SHADOW_CALL_STACK",
    "CONFIG_ARM64_PTR_AUTH","CONFIG_ARM64_BTI","CONFIG_RANDOMIZE_BASE",
    "CONFIG_MODULES","CONFIG_MODULE_SIG_FORCE","CONFIG_KALLSYMS",
    "CONFIG_KPROBE_EVENTS","CONFIG_PANIC_ON_OOPS","CONFIG_ARCH_QCOM"
]
def s(k,d):return d[k] if k in d else "(missing)"
summary={
    "source":"Google android/kernel/common; android12-5.10-2025-05_r1; 5.10.236",
    "source_commit_expected":"b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e",
    "source_is_honor_production_tree":False,
    "toolchain_is_device_exact":False,
    "original_symbols":len(a),"resolved_symbols":len(b),
    "shared_symbols":len(common),
    "unchanged_shared_symbols":len(common)-len(changed),
    "changed_count":len(changed),
    "removed_count":len(removed),
    "added_count":len(added),
    "removed_active_count":len(removed_active),
    "added_active_count":len(added_active),
    "changed_active_count":len(changed_active),
    "changed":[{"key":k,"device":a[k],"candidate":b[k]} for k in changed],
    "removed":[{"key":k,"device":a[k]} for k in removed],
    "added":[{"key":k,"candidate":b[k]} for k in added],
    "important":{k:{"device":s(k,a),"candidate":s(k,b)} for k in keyset},
    "conclusion":"olddefconfig is only a source-tree compatibility test, NOT an OEM kernel reconstruction, vulnerability finding or symbol layout proof."
}
Path(reportpath).parent.mkdir(parents=True,exist_ok=True)
Path(reportpath).write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n")
lines=[
"# ANB-AN00：真实 Kconfig 与 Google 5.10.236 GKI 的对照",
"",
"> **候选源码验证实验，而不是 HONOR ANB-AN00 出厂内核重建。**",
"",
"- 设备：HONOR ANB-AN00 / MagicOS 9.0.0.230 / Linux 5.10.236-android12-9。",
"- 真正设备的 config.gz SHA-256：\`76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3\`。",
"- 候选源码：[Google android12-5.10-2025-05_r1](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r1)，SHA \`b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e\`。",
"- **构建环境是 GitHub Runner 的编译器**，非实机 Android Clang 12.0.5，所以编译器版本相关 Kconfig 项的变更不代表设备原厂配置错误。",
"",
"## olddefconfig 配置对照",
"",
"| 指标 | 数量 |","|---|---:|",
f"| 原始设备配置项 | {len(a)} |",
f"| Google 候选求解后配置项 | {len(b)} |",
f"| 双方共享配置项 | {len(common)} |",
f"| 共享配置值改变 | {len(changed)} |",
f"| 设备原始配置项在 Google 树中消失 | {len(removed)} |",
f"| 候选树新增配置项 | {len(added)} |",
f"| 原始已启用 (y/m) 而候选树缺失 | {len(removed_active)} |",
f"| 候选树新增已启用 (y/m) | {len(added_active)} |",
"",
"## 核心配置实际对照","","| 配置项 | ANB 真机原始状态 | Google 候选求解后 |",
"|---|---|---|"]
for k in keyset:lines.append(f"| \`{k}\` | \`{s(k,a)}\` | \`{s(k,b)}\` |")
lines.extend(["","## 原本启用但 Google 参考树不识别的配置","","这些配置不应被静默抹掉；可能与 OEM 驱动、保护选项、不同 GKI 版本或厂商私有源代码有关：",""])
if removed_active:
    for k in removed_active[:120]:
        lines.append(f"- \`{k}\`（实机 \`{a[k]}\`）")
    if len(removed_active)>120:lines.append(f"- 其余 {len(removed_active)-120} 项见 JSON 原始清单。")
else:lines.append("无（当前 Kconfig 对照结果）。")
lines+=["","## 原本共享但取值改变的选项（前 80 项）",""]
for k in changed[:80]:lines.append(f"- \`{k}\`: \`{a[k]}\` → \`{b[k]}\`")
if len(changed)>80:lines.append(f"- 其余 {len(changed)-80} 项见 JSON。")
lines+=["","## 结论与禁止误判","","- 这份源码是 Google GKI 的**公共参照系**；5.10.236 一致**不足以证明** HONOR 的 \`gd671cf8c2b02\` 对应同一提交或没有后移补丁。","- 共享配置数高只能说明 Kconfig 的符号覆盖率，不能推导 \`vmlinux\` 二进制相等；被移除的启用配置尤其表明存在来源差异。","- \`olddefconfig\` 既不编译运行内核，也不验证任何 CVE / Root / Bootloader 解锁；实际目标机仍为 locked/green。","- Google 官方 GKI 5.10.236 r1 不等于可刷写 HONOR boot.img。**不要将 Google 的通用镜像写入真实 ANB 手机。**","",
"## 下一步","","1. 申请或检索与 \`ANB-AN00 9.0.0.230(CHNC00E230R2P5)\` 完全匹配的荣耀源码，尤其是 Google 参考树无法解释的已启用 Kconfig 符号。","2. 若要研究具体 CVE，只离线查相应补丁的真实源码状态；在取得可信 OEM 二进制前不给出具体漏洞利用或符号偏移。","3. 如需排除编译器差异，另用实际 \`clang-r416183b\` 在相同公共源码上重跑 \`olddefconfig\`，不要用宿主版 Clang 结果作为原厂一致性证明。",""]
Path(docpath).parent.mkdir(parents=True,exist_ok=True)
Path(docpath).write_text("\n".join(lines),encoding="utf-8")
print(f"GKI_COMPARE PASS original={len(a)} resolved={len(b)} changed={len(changed)} removed={len(removed)} added={len(added)} removed_active={len(removed_active)}")
