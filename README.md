# HONOR 80 Pro 直屏版（ANB-AN00）研究档案

**研究对象：HONOR 80 Pro Flat / ANB-AN00 / HNANB。** 本仓库记录用户真实手机的只读取证、官方及第三方固件来源调查、内核分析计划和研究结果。独立于 [Rakuten Hand 5G P780](https://github.com/Chuniex-0631/p780-kernel-research)：借用研究方法，**不移植其它机型的内核地址、签名或刷机文件**。

**目前没有获得 Root，没有解锁 Bootloader，也未发现对 ANB-AN00 9.0.0.230 已经实机验证的提权路径。**

## 真实设备基线（2026-10-11）

| 项目 | 用户手机的实际 ADB 回显 |
|---|---|
| 设备 | **ANB-AN00**，代号 HNANB |
| 芯片平台属性 | **taro** |
| 系统 | Android 15 / MagicOS 9.0.0 |
| 固件 | **ANB-AN00 9.0.0.230(CHNC00E230R2P5)** |
| 补丁级别 | Android / Vendor：2026-08-01 |
| 内核 | **5.10.236-android12-9-gd671cf8c2b02** |
| 编译器 | Android Clang 12.0.5 / LLD 12.0.5 |
| Verified Boot | **green** |
| Flash / VBMeta | **locked / locked** |
| 内核配置 | [config.gz 原件](evidence/device/honor-anb-config.gz) **已校验**：42,277 B、5,672 项 |

原始文本：[evidence/device/honor80pro-flat-baseline.txt](evidence/device/honor80pro-flat-baseline.txt)。
设备属性的空返回值 ro.oem_unlock_supported **不能当作支持或不支持解锁的证明**。

## 研究入口

| 资料 | 内容 |
|---|---|
| [STATUS](docs/STATUS.md) | 已确认、待确认、已失败、未开始的事实状态 |
| [DEVICE_BASELINE](docs/DEVICE_BASELINE.md) | 对用户实机记录的逐项整理与解释 |
| [KERNEL_AND_BOOT](docs/KERNEL_AND_BOOT.md) | 内核、AVB、Bootloader 与 P780 经验边界 |
| [KERNEL_SECURITY_PROFILE](docs/KERNEL_SECURITY_PROFILE.md) | 真实 Kconfig 防护、诊断接口、内核模块与限制 |
| [KERNEL_CONFIG_REPORT](docs/KERNEL_CONFIG_REPORT.md) | 原始 gzip 自动生成的 Kconfig 状态摘要 |
| [FIRMWARE_AND_SOURCES](docs/FIRMWARE_AND_SOURCES.md) | 官方开源渠道与第三方 ANB 历史固件索引 |
| [SOURCE_PROVENANCE](docs/SOURCE_PROVENANCE.md) | 官方 5.10.236 GKI 精确版本、HONOR 更新记录与出处 |
| [GKI_CONFIG_COMPARISON](docs/GKI_CONFIG_COMPARISON.md) | 实机 5,672 项与 Google GKI 5,351 项的历史差异（宿主 Clang14） |
| [GKI_SYMBOL_PROVENANCE](docs/GKI_SYMBOL_PROVENANCE.md) | 新增的符号定义检查与 Android Clang12 控制实验 |
| [VENDOR_DELTA_TRIAGE](docs/VENDOR_DELTA_TRIAGE.md) | 208 项启用而参考树缺失的符号分类与重要厂商开关 |
| [CVE_CONFIG_FLAGS](docs/CVE_CONFIG_FLAGS.md) | 三个 CVE 命名开关的真实配置与公开漏洞记录，不是可利用性结论 |
| [GKI_COMPARISON_METHOD](docs/GKI_COMPARISON_METHOD.md) | 使用真实配置与 Google 固定源码的离线测试方法 |
| [SOURCE_REQUEST_DRAFT](docs/SOURCE_REQUEST_DRAFT.md) | 向荣耀官方请求匹配版本开源代码的邮件草稿（未发送） |
| [HISTORY](docs/HISTORY.md) | 本项目成立前后的研究时间线 |
| [EVIDENCE_MANIFEST](docs/EVIDENCE_MANIFEST.md) | 已上传原件和仍缺材料 |
| [NEXT_STEPS](docs/NEXT_STEPS.md) | 最有价值的下一步实验 |
| [RESEARCH_RULES](docs/RESEARCH_RULES.md) | 如何同步更新、如何判断证据级别 |
| [CHANGELOG](CHANGELOG.md) | 阶段更新日志 |

## 当前值得研究的三条路线

1. **内核配置：已取得并分析。** 原始 gzip 已保存，确定 Futex PI、CFI、Full LTO、KASLR、Kprobe 和模块签名等配置。下一步寻找**同一固件版本**的真实源码、编译参数及补丁身份。
2. **可信源码：有新线索。** Google 官方已确认 `android12-5.10-2025-05_r1` 对应 5.10.236，并提供精确提交 `b97c62c4...`；其工具链 `clang-r416183b` 与实机 Clang 修订相符。**这仍不是荣耀厂商源码**，需用 Kconfig 对照和向 HONOR 申请源码进一步验证。
3. **历史固件出处：** 第三方公开索引列有 ANB-AN00 的 MagicOS 7/8 版本。这仅是追踪线索，并不证明下载链接仍有效，也不代表完整 OTA、可解锁或可降级。

## 目录

- docs/ —— 研究结论和资料说明。
- evidence/device/ —— 设备所有者提供的、经检查不包含个人唯一标识的只读基线。
- tools/ 和 .github/workflows/ —— 文档与证据的自动检查。
- CHANGELOG.md —— 每阶段同步维护。

**隐私：** 仓库实际为 **Private**，但创建初期曾是 Public。仍不应提交未经筛查的 IMEI、序列号、SIM、账号、令牌或 Bugreport。过去公开过的 Git 历史可能已被外部复制；参见 [SECURITY](SECURITY.md)。

## 内核配置里程碑（2026-10-11）

- **原件已入仓**：[honor-anb-config.gz](evidence/device/honor-anb-config.gz)；gzip **42,277 B**，解压 **191,897 B**；5,672 个配置项。
- SHA-256：`76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。
- **实际核验结果**：`FUTEX_PI=y`、`RT_MUTEXES=y`、`CFI_CLANG=y`、`LTO_CLANG_FULL=y`、`RANDOMIZE_BASE=y`、`PANIC_ON_OOPS=y`、`MODULE_SIG_FORCE=y`。
- [逐项内核配置报告](docs/KERNEL_CONFIG_REPORT.md) · [安全与诊断特性分析](docs/KERNEL_SECURITY_PROFILE.md) · [可机读摘要](evidence/reports/anb-config-audit.json)。
- 这些配置**不证明当前内核存在 GhostLock 或其它可利用漏洞**。仍缺匹配 ANB-AN00 9.0.0.230 的原厂源码、补丁和二进制证据。


## 官方源码对比进度（2026-10-11）

- [Google 5.10.236 GKI 固定版本](docs/SOURCE_PROVENANCE.md)：已核实官方标签、SHA、构建日期和与实机工具链的关系。
- [Google GKI Kconfig 对照工作流](.github/workflows/gki-kconfig-compare.yml)：使用已验证的 `honor-anb-config.gz`，比对 Google 候选源码的 Kconfig 支持情况；仅离线、无实机读写、无刷机镜像。
- [荣耀官方源码请求草稿](docs/SOURCE_REQUEST_DRAFT.md)：官方开源中心确认可请求相应开源代码，但尚未发送请求、尚未获得对应厂商源码。
- 实机 `5.10.236-android12-9-gd671cf8c2b02` 与 Google 候选源码是否同一实际构建**尚未确证**，Root 和 Bootloader 解锁仍未达成。
