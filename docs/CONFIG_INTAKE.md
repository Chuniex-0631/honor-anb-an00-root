# ANB-AN00 内核配置：接收与验证结果（2026-10-11）

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

## 实际接收

- 真实设备：`HONOR ANB-AN00 / Android 15 / MagicOS 9.0.0.230 / kernel 5.10.236`，设备代号 HNANB，硬件平台属性 taro。
- 实机原始路径：`/proc/config.gz`，ADB 先记录大小 **42,277 B**。
- 原始 gzip 已上传 [evidence/device/honor-anb-config.gz](../evidence/device/honor-anb-config.gz)，大小 **42,277 B**，已成功解压至 **191,897 B**。
- SHA-256：`76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`（对 GitHub 仓库中真实 gzip 的计算结果）。
- GitHub Actions [#38072192104](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38072192104) 确认处理成功，生成 [KERNEL_CONFIG_REPORT](KERNEL_CONFIG_REPORT.md)。
- 进一步分类审计工作流 [kernel-config-audit](research-workflows/kernel-config-audit.yml) 读取同一原件，生成 [研究型安全配置报告](KERNEL_SECURITY_PROFILE.md) 和 [精选配置 JSON](../evidence/reports/anb-config-audit.json)。

## 初步结果

配置共 **5,672** 个条目（y=2,044、m=0、n=3,525、其他值=103）。已证实 Futex PI、RT Mutex、CFI、Clang Full LTO、Shadow Call Stack、Pointer Authentication、BTI、KASLR、模块强制签名等功能配置启用。

**限制：** Kconfig 只表明编译时设定；不说明某一个具体 CVE 是否已被厂商回移修复，不提供设备符号地址，也不证明非 Root Shell 能使用所有调试接口。仓库当前 Public，不要加入有个人唯一标识的原始全量设备日志。
