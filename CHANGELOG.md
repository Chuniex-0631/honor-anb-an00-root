# HONOR ANB-AN00 研究更新日志

## 2026-10-11 — v0.6：仓库续接、证据分层与 Clang12 控制实验

- 核实 GitHub 仓库实际 Private、main 的所有证据及近期 Actions；修正 STATUS 的 GKI 比较状态、README/manifest 的隐私说明及 HISTORY 时间线格式。
- 旧成功 GKI 结果 5672→5351、329 缺失（原启用 208）继续保留。5 项共享值变化中四项为版本字段、一项为 `LTO_NONE`；观察到旧求解 `AS_IS_GNU=y`，不能把全部差异归为厂商源码。
- 新增只读的 `tools/audit_gki_provenance.py`、`gki-symbol-toolchain.yml`，独立检查 208 项在 Google 参考树是否有 Kconfig 声明，以及 Android Clang 12.0.5 (r416183b) 的第二次求解。结果须以实际 Actions 及 Artifact 为准，未验证前不报成功。
- CVE 专题登记三个公开上游修补的 commit/代码位置；另外补充发现 ANB 适配的 9.0.0.157 与 9.0.0.187 荣耀官方版本帖。检索 9.0.0.230 固件与 OEM 源码仍未获得可信匹配件。无手机侧操作，无 Root 或 BL 解锁。

## 2026-10-11 — v0.1 初始化

- 通过用户授权的 GitHub 验证新仓库可读可写，同时发现仓库目前为 **Public**。
- 将 2026-10-10 前期调查的 ANB 与 ANP 区分、官方开源中心和第三方 ANB 历史固件列表整理入专题文档。
- 归档用户提供的 2026-10-11 ADB 基线文本，核对设备为 ANB-AN00/HNANB，Android 15、MagicOS 9.0.0.230、kernel 5.10.236、Clang 12.0.5、Locked/Green。
- 保留重要阴性/未知情况：ro.oem_unlock_supported 输出为空；/proc/config.gz 存在但未取得内容；未采集 Fastboot/EDL；未实现 Root 或 Bootloader 解锁。
- 设置文档链接与基线数据自动校验；开始按 P780 的研究规范长期同步 GitHub。

## 2026-10-11 — v0.5：Google 5.10.236 公共源码对照与厂商差异筛查

- Google GKI `android12-5.10-2025-05_r1` 精确源码克隆和版本核对成功；对实机 5,672 项运行 `olddefconfig`，得到 Google 参考树配置 5,351 项。
- **共享值变化 5 项、缺失 329 项（其中 208 项原本启用）、新增 8 项**。第一次实算 CI 因 Git 并发提交冲突推送失败，后通过独立 Artifact 恢复工作流完成数据永久归档。
- 208 项按命名规则分类：84 个 HONOR/HN 命名、3 个 CVE 命名、26 个编译安全/ARM64、4 个 Qualcomm/平台、29 个内存/IO、10 个启动/诊断、52 个其它；不将此归类当作源代码已分析的结果。
- 新增 [GKI 实验报告](docs/GKI_CONFIG_COMPARISON.md)、[厂商差异索引](docs/VENDOR_DELTA_TRIAGE.md)、[三个 CVE 标志研究](docs/CVE_CONFIG_FLAGS.md)。参考源码和 Ubuntu Clang 14 均不能证明 HONOR 生产内核代码身份。
- **没有取得 Root、Bootloader 解锁、原厂 boot.img 或真正匹配的完整荣耀源码**。

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
