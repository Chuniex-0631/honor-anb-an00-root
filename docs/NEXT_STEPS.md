# 接下来研究什么（已取得内核 config.gz）

## 已完成：原始配置采集

[evidence/device/honor-anb-config.gz](../evidence/device/honor-anb-config.gz) 已由用户上传，gzip 解压及 SHA-256 验证成功。**不需要重新运行 ADB 提取命令或再次上传配置。**

查看 [配置摘要](KERNEL_CONFIG_REPORT.md)、[安全特性报告](KERNEL_SECURITY_PROFILE.md) 和 [研究状态](STATUS.md)。

## P1：精确来源的内核源码（当前首要阻断点）

实机固件为 `ANB-AN00 9.0.0.230(CHNC00E230R2P5)`，Linux `5.10.236-android12-9-gd671cf8c2b02`，编译器 Clang `12.0.5`。

- 官方 Google `android12-5.10-2025-05_r1` 的 5.10.236 源码及 SHA 已确认，已经建立 [候选源码配置比对工作流](../.github/workflows/gki-kconfig-compare.yml)。该候选不是荣耀原厂内核，不生成可刷固件。
- [HONOR 开源中心](https://www.honor.com/global/opensource/) 明确允许按 `Open Source Codes Issue` 主题向 `support@hihonor.com` 申请源代码。本仓库准备了 [设备版本明确的英文申请草稿](SOURCE_REQUEST_DRAFT.md)；**尚未发送**。
- 如取得源码，在 GitHub Actions 做原生 `olddefconfig`、严格记录原始/求解后配置变更、尝试编译单一对象、保存源码 commit 与编译器版本；不编造原厂内核二进制一致性。
- 针对 CVE 使用精确修复 commit 与设备实际构建对照，不用泛化版本号代替证据。

## P2：验证可用的只读接口

配置显示 Kprobe/Ftrace/DebugFS 等选项启用，**但 Shell 的实际访问权限尚未知**。下一步如确实需要只读检查，可设计最小 ADB 审计；不会尝试内核崩溃触发测试。模块强制签名和 PANIC_ON_OOPS 更要求保持谨慎。

## P3：官方固件真实性与启动链

已有 BL locked 和 AVB green 记录，但无 Fastboot、EDL 成功研究。第三方历史 ANB OTA 索引尚未通过包签名/来源检查，不建议刷机或降级。

**你目前无需采取任何额外操作。** 等需要新的实机证据时再提供针对性的只读命令。
