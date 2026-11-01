# 接下来研究什么（已取得内核 config.gz）

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

## 已完成：原始配置采集

[evidence/device/honor-anb-config.gz](../../evidence/device/honor-anb-config.gz) 已由用户上传，gzip 解压及 SHA-256 验证成功。**不需要重新运行 ADB 提取命令或再次上传配置。**

查看 [配置摘要](KERNEL_CONFIG_REPORT.md)、[安全特性报告](KERNEL_SECURITY_PROFILE.md) 和 [研究状态](../STATUS.md)。

## P1：精确来源的内核源码（当前首要阻断点）

实机固件为 `ANB-AN00 9.0.0.230(CHNC00E230R2P5)`，Linux `5.10.236-android12-9-gd671cf8c2b02`，编译器 Clang `12.0.5`。

- 官方 Google `android12-5.10-2025-05_r1` 的 5.10.236 源码及 SHA 已确认，已经建立 [候选源码配置比对工作流](../research-workflows/gki-kconfig-compare.yml)。该候选不是荣耀原厂内核，不生成可刷固件。
- [HONOR 开源中心](https://www.honor.com/global/opensource/) 明确允许按 `Open Source Codes Issue` 主题向 `support@hihonor.com` 申请源代码。本仓库准备了 [设备版本明确的英文申请草稿](SOURCE_REQUEST_DRAFT.md)；**尚未发送**。
- 如取得源码，在 GitHub Actions 做原生 `olddefconfig`、严格记录原始/求解后配置变更、尝试编译单一对象、保存源码 commit 与编译器版本；不编造原厂内核二进制一致性。
- 针对 CVE 使用精确修复 commit 与设备实际构建对照，不用泛化版本号代替证据。

## P1.5：跟进旧 208 项与新版 193 项交叉差异

旧宿主 Clang14 实验得到 Google 参考配置 5,351 项、原启用缺失 208 项；**精确 Android Clang12.0.5** 复算已成功，Google 参考配置 5,366 项，原启用缺失 **193** 项，共享配置取值变化 0。旧 208 项中 16 项重获保留，另有 1 项在新实验中缺失；详情见 [正式归档](../GKI_SYMBOL_PROVENANCE.md)。后续优先从 [厂商差异归类](VENDOR_DELTA_TRIAGE.md) 的 84 个 HONOR/HN 命名项、3 个 CVE 命名项着手寻找源码，不对其它 121 项直接作厂商私有功能判断。三个 CVE 宏应按 [公开漏洞与厂商命名](CVE_CONFIG_FLAGS.md) 核对，不当作漏洞利用指示。[Kconfig 定义扫描](../GKI_SYMBOL_PROVENANCE.md) 已验证旧 208 项在 Google 全树中 28 项有声明、180 项未找到声明；下一阶段应将新版 193 项与源码定义结果交叉分类，再调查厂商补丁和 Qualcomm 实现，避免把未输出的项全部误称厂商私有代码。

## P2：验证可用的只读接口

配置显示 Kprobe/Ftrace/DebugFS 等选项启用，**但 Shell 的实际访问权限尚未知**。下一步如确实需要只读检查，可设计最小 ADB 审计；不会尝试内核崩溃触发测试。模块强制签名和 PANIC_ON_OOPS 更要求保持谨慎。

## P3：官方固件真实性与启动链

已有 BL locked 和 AVB green 记录，但无 Fastboot、EDL 成功研究。第三方历史 ANB OTA 索引尚未通过包签名/来源检查，不建议刷机或降级。

**你目前无需采取任何额外操作。** 等需要新的实机证据时再提供针对性的只读命令。

## 追加：针对 GKI 参考树出现的代码差异

[三个 CVE 参考树静态核查](GKI_CVE_SOURCE_CHECK.md) 表明有的上游修复特征可见、有的未见，因此后续对比需从 **确切代码与补丁 diff** 入手。不要只根据 Linux 5.10.236 与上游 stable 5.10.214 的数字关系推断真实手机受保护或存在漏洞。
