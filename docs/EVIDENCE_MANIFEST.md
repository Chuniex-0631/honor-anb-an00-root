# 已归档证据和未取得原件

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

当前 App 发行证据：公开 [发行元数据](../release/current.json)、[核心文件哈希](../release/v2.3.0-core-files.json)、[APK 校验值](../release/SHA256SUMS.txt) 和 [验证摘要](VALIDATION.md)。原始私人运行日志仅本地保存。

| 文件/资料 | 目前状态 | 说明 |
|---|---|---|
| [honor80pro-flat-baseline.txt](../evidence/device/honor80pro-flat-baseline.txt) | **已归档文本** | 用户 2026-10-11 的真实只读 ADB 输出；源附件 1,292 B；GitHub 是可读文本转录，非已独立 SHA 验证的原始字节备份。 |
| [honor-anb-config.gz](../evidence/device/honor-anb-config.gz) | **原始 gzip 已入 Git 并通过校验** | 42,277 B；解压 191,897 B；SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3` |
| ANB 9.0.0.230 完整 OTA | **未取得** | 历史固件版本列表不足以证明整包存在。 |
| 匹配 ANB 的 boot.img/vendor_boot.img/vmlinux | **未取得** | 未获得原厂固件二进制。 |
| 同版本对应的荣耀内核源码 | **未取得** | 已找到官方开源渠道，具体包未验证。 |
| [第三方历史固件索引](https://gist.github.com/0x8008/3ef2413fbd09648b0c02a0d1cf585d22) | **已登记链接和版本示例** | 未取得验证过的下载文件。 |
| [荣耀开源中心](https://www.honor.com/global/opensource/) | **官方网页已找到** | 不代表存在精确设备版本源码。 |

### 隐私

本页的可见性记录属于历史状态；当前仓库用于公开发布，按 PRIVACY.md 脱敏。仍不归档 IMEI、序列号、SIM/运营商账号、含认证令牌的 OTA URL 或未经筛查的完整 Bugreport；改变仓库可见性不能消除旧公开历史可能造成的泄露。

## 2026-10-11 配置的实物验收与历史状态修正

- 本文件此前记录“附件已上传、尚未归档”：**已失效**。用户已将原始 `honor-anb-config.gz` 上传至本 GitHub 仓库；Actions [#38072192104](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38072192104) 实际解压并生成报告。
- [KERNEL_CONFIG_REPORT](KERNEL_CONFIG_REPORT.md) 是直接读取实际 gzip 的自动结果；[KERNEL_SECURITY_PROFILE](KERNEL_SECURITY_PROFILE.md) 和 [JSON](../evidence/reports/anb-config-audit.json) 则提供分类的精选符号、统计和研究边界。
- 经过 SHA-256 对仓库**当前原件**的完整性核查；尚未和机主本地未改动的备份单独做 SHA 比对，不声称存在多个独立来源认证。
- `ro.oem_unlock_supported` 为空结果、Bootloader 仍 locked；配置可读取不是 Root 或解锁证据。


## 2026-10-11 GKI 真实差异输出与新资料

- 已保存 [Google 5.10.236 对照报告](GKI_CONFIG_COMPARISON.md)、[完整配置差异 JSON](../evidence/reports/anb-gki-config-delta.json)、[208 项名称归类](VENDOR_DELTA_TRIAGE.md) 与 [可机读分类数据](../evidence/reports/anb-vendor-delta-triage.json)。后续若重跑不同 Clang 或源码 tag，应新增独立版本记录，不覆盖原始证据来源而不留痕。
- [首次实算 CI](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38073022058) 产生配置差异，后由成功的 [Artifact 归档 CI](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38073491737) 永久保存。
- 三个以 CVE 编号命名的配置键及其可能的公开问题属于离线线索，不等于原厂安全补丁状态；见 [CVE_CONFIG_FLAGS](CVE_CONFIG_FLAGS.md)。

## 2026-10-11 官方开源来源与待完成实验

- [Google 官方 `android12-5.10-2025-05_r1`](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r1)：源码公开的固定 SHA 已从 Android 官方 release 文档独立核实；**尚未把整个内核源码复制到本仓库**。
- [Google 构建配置](https://android.googlesource.com/kernel/common/+/refs/heads/android12-5.10/build.config.common) 与 [Clang r416183b](https://android.googlesource.com/platform/prebuilts/clang/host/linux-x86/+/refs/heads/android12L-d2-s2-release/clang-r416183b/) 是公开可用的来源信息，不是 ANB OEM 专有源码。
- [本项目 CI 工作流](research-workflows/gki-kconfig-compare.yml) 对照的是**真实 gzip 与 Google 公共源码**。首次 clone 遇 HTTP 502，仅表示网络下载失败；有效 Kconfig 结果须以成功的 Actions 产物为准。
- HONOR 当前版本的完整内核源码/补丁、`boot.img`、`vmlinux`、`System.map` **依旧没有取得**，不得由 Google GKI 源文件代替。

## v0.6 新实验登记（待证据入库）

- 新增只读工作流 [GKI symbols / Clang12](research-workflows/gki-symbol-toolchain.yml)，产物为 `symbol-definitions.json`、`clang12-comparison.json` 和环境日志；已经由 [Actions #38076177418](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38076177418) 验证成功，并经 [归档 #38076601768](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38076601768) 检验后永久入 Git；旧 GKI JSON 继续保留。
- 之前成功运行：[GKI #38073627910](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38073627910)、[archive audit #38073733732](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/runs/38073733732)。

## v0.6 GKI 源码定位记录

- [GKI 参考树 CVE 代码特征 JSON](../evidence/reports/anb-gki-cve-static-markers.json) 与 [分析报告](GKI_CVE_SOURCE_CHECK.md)：来源为已固定公共镜像提交，包含四个 Git blob SHA、匹配特征及解释边界；**不包含设备 OEM 源码，也没有声称已验证任何真实设备 CVE**。

### 精确 Clang12 和符号定义已归档数据

- [符号定义 JSON](../evidence/reports/anb-gki-symbol-definitions.json)：1,580 个 Kconfig 文件，208 项中 28 项有定义、180 项无定义；[逐项源码声明路径](GKI_SYMBOL_DEFINITIONS.md)。
- [Android Clang12 配置比较 JSON](../evidence/reports/anb-gki-clang12-comparison.json)：5,672→5,366，变化 0、缺失 311（原启用 193）、新增 5；[报告](GKI_CLANG12_COMPARISON.md)。
- [Clang 版本文本](../evidence/reports/anb-clang12-version.txt)、[LLD 版本文本](../evidence/reports/anb-lld12-version.txt)、[工具链 Git 源 SHA](../evidence/reports/anb-toolchain-source-commit.txt)。本次克隆源代码为 Google 公共源码与官方编译器，**不是生产 OEM 源码**。
- 原始 CI Artifact ID 11678900920；归档时验证源提交和设备哈希。历史失败 #38076018784、#38076037276 均保留。
