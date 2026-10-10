# HONOR ANB-AN00 研究更新日志

## 2026-10-11 — v0.1 初始化

- 通过用户授权的 GitHub 验证新仓库可读可写，同时发现仓库目前为 **Public**。
- 将 2026-10-10 前期调查的 ANB 与 ANP 区分、官方开源中心和第三方 ANB 历史固件列表整理入专题文档。
- 归档用户提供的 2026-10-11 ADB 基线文本，核对设备为 ANB-AN00/HNANB，Android 15、MagicOS 9.0.0.230、kernel 5.10.236、Clang 12.0.5、Locked/Green。
- 保留重要阴性/未知情况：ro.oem_unlock_supported 输出为空；/proc/config.gz 存在但未取得内容；未采集 Fastboot/EDL；未实现 Root 或 Bootloader 解锁。
- 设置文档链接与基线数据自动校验；开始按 P780 的研究规范长期同步 GitHub。

## 2026-10-11 — v0.4：官方 GKI 源码溯源与离线配置对照

- 用户确认仓库已改为 **Private**，GitHub API 实际核实为私有。
- 官方 Google `android12-5.10-2025-05_r1` 对应 5.10.236 的完整 SHA 确认为 `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`；Google build config 中 KMI generation 9 和 Clang `r416183b` 与设备内核编译器修订对应。
- 查到 HONOR ANB-AN00 适配的官方 8.0.0.150/9.0.0.120/9.0.0.175/9.0.0.200 更新公告；未找到对应实机 9.0.0.230 的可验证完整 OTA。
- 创建 [SOURCE_PROVENANCE](docs/SOURCE_PROVENANCE.md)、[SOURCE_REQUEST_DRAFT](docs/SOURCE_REQUEST_DRAFT.md)、[GKI_COMPARISON_METHOD](docs/GKI_COMPARISON_METHOD.md)。
- 配置了使用实机 config.gz、Google 固定源代码、源 SHA 核对和 Kconfig 差异统计的 GitHub Actions。首次 Google Git 下载因 HTTP 502 失败（**不代表配置不兼容**），已追加下载重试与备用官方 Gitiles 地址。
- 原厂 `vmlinux`/boot.img、完整厂商源码仍未获得；GhostLock、Root 和 BL 解锁均**未被证明可行**。

## 2026-10-11 — v0.3：实机 Kconfig 验证成功

- 用户把 `honor-anb-config.gz` 上传至 `evidence/device/`，实际文件 **42,277 B**；解压 **191,897 B**，SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。
- GitHub Actions [#38072192104](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38072192104) 成功从原始压缩文件中解析 **5,672** 项 Kconfig，生成 `docs/KERNEL_CONFIG_REPORT.md`。
- 第二轮自动审计分类检查 Futex/锁机制、Clang 安全缓解、隔离与诊断接口、模块签名及 Qualcomm 架构，生成 `docs/KERNEL_SECURITY_PROFILE.md` 和 `evidence/reports/anb-config-audit.json`。
- 更新 README、STATUS、DEVICE_BASELINE、KERNEL_AND_BOOT、EVIDENCE_MANIFEST、CONFIG_INTAKE、HISTORY、NEXT_STEPS，消除原先“未上传/未读取配置”的过期描述。
- 研究结果依然是：**未 Root、Bootloader locked、尚无匹配的原厂源码/完整固件，不声称存在可利用 CVE**。

## 2026-10-11 — v0.2：内核配置接收与审计准备

- 用户将 honor-anb-config.gz 上传至对话，文件服务记录大小 42,277 字节。
- 本地文件执行环境异常，尚不能计算该附件的 SHA-256 或解压内容；不能据此声明任何 CONFIG 项已验证。
- 已准备 GitHub 端解压、检查和生成摘要的分析脚本与 Actions；原件进入仓库后方可开始实际分析。
- README、STATUS 与 EVIDENCE_MANIFEST 已同步标注缺口。
