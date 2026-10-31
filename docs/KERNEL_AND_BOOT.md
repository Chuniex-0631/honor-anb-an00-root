# 内核、启动验证和漏洞研究边界

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

## 已验证事实

本机 Android 15/MagicOS 9.0.0.230，内核为 **5.10.236-android12-9-gd671cf8c2b02**，编译器为 **Android Clang 12.0.5**。ro.boot.flash.locked=1、ro.boot.vbmeta.device_state=locked、ro.boot.verifiedbootstate=green。

从这些内容只能确认当前常规启动状态为**锁定**，不能推断具体 Fastboot OEM 命令是否存在、开发者选项是否允许解锁，亦没有 Root 成功的含义。

## P780 经验的适用范围

可以复用：真实 config 快照 → 上游源码/修复 commit 比对 → 同版本编译器与 Kconfig → 原始日志、SHA 和 GitHub Actions 工件 → 逐构建证据审核。

**不能复用：** P780 的 QAS/TZ/PFM 测试结果、Tinno 旧代码、EDL loader、内核偏移、GhostLock exploit 适配参数、P780 Fastboot 错误响应。ANB-AN00 是完全不同的 OEM 和代码基线。

## 从真实配置新增的结论

- **已有 Futex PI / RT Mutex** 只说明对应内核机制存在，不证明 GhostLock 或任意漏洞能在该内核触发。
- **CFI、Full LTO、PAC、BTI、Shadow Call Stack、KASLR 均启用**，研究必须尊重实际编译器/防护差异，不能复用 P780 的地址。
- **`CONFIG_MODULE_SIG_FORCE=y`** 代表编译配置要求强制模块签名；`CONFIG_KALLSYMS_ALL=y` 和 `CONFIG_KPROBE_EVENTS=y` 是内核功能开关，非 shell 权限保证。
- **`CONFIG_PANIC_ON_OOPS=y`**：优先离线分析，避免未经适配的 crash/UAF 实机试验。

## 当前核心阻断点

1. 已取得 [原厂配置 gzip](../evidence/device/honor-anb-config.gz) 和 [安全特性分类报告](KERNEL_SECURITY_PROFILE.md)：Futex PI/RT Mutex、CFI、Full LTO、Shadow Call Stack、KASLR、模块签名均有真实状态。**这不等于漏洞补丁状态已明**。
2. 还没有找到与 ANB-AN00 9.0.0.230 匹配的厂商内核源码与原厂 vmlinux/boot.img。
3. 仅凭 Android 补丁日期或 5.10 分支号**不能证明任何 CVE 可利用**；需要按修复状态与真实设备行为验证。
4. 历史固件的下载索引不等于可用于锁定 Bootloader 的刷机包，也不说明安全回滚是否允许。

本阶段不进行跨机型刷机、EDL 写入、强制降级、或未经适配的高风险内核崩溃测试。
