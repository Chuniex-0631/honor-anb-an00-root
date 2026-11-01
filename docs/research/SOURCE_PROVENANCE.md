# ANB-AN00 原厂源码与公开 GKI 出处调查（2026-10-11）

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

## A. 已有的厂商/设备事实

| 判断 | 可靠来源 | 内容 |
|---|---|---|
| 荣耀型号归属 | [荣耀开发者机型表](https://developer.honor.com/cn/docs/game_center/guides/jieruzhinan/jixing) | Anna B / 荣耀 80 Pro 直屏版 / **ANB-AN00**，不同于 ANN/ANP |
| 用户固件 | [实机基线](../../evidence/device/honor80pro-flat-baseline.txt) | MagicOS **9.0.0.230(CHNC00E230R2P5)**、Android15、Linux **5.10.236-android12-9-gd671cf8c2b02** |
| 完整内核配置 | [gzip 原件](../../evidence/device/honor-anb-config.gz) | 42,277 B、5,672 配置项、SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3` |
| 当前启动链 | [设备属性](DEVICE_BASELINE.md) | locked / green；Root 与 BL 解锁未达成 |

## B. HONOR 已公开的该型号更新记录

以下均在厂商官方社区内由“荣耀版本升级 / 官方产品维护”发布，支持型号名单含 **ANB-AN00**，不是从第三方固件命名推断：

| 发布信息 | 日期 | 说明 |
|---|---|---|
| [MagicOS 8.0.0.150](https://club.honor.com/cn/thread-28854024-1-1.html) | 2024-09-21 | ANB/ANP/ANN 在支持型号内 |
| [MagicOS 9.0.0.120](https://club.honor.com/cn/thread-29163791-1-1.html) | 2025-01-16/17 | 明确说明从 8.0.0.160/161 到 9.0.0.120 的官方升级路径 |
| [MagicOS 9.0.0.157](https://club.honor.com/cn/thread-29245413-1-1.html) | 2025-02-24 | 官方发布说明列出 ANB-AN00；9.0.0.120→157 |
| [MagicOS 9.0.0.175](https://club.honor.com/thread-29483449-1-1.html) | 2025-06-11 | 支持 ANN/ANP/**ANB**，从 9.0.0.165 升到 175 |
| [MagicOS 9.0.0.187](https://club.honor.com/cn/thread-29639896-1-1.html) | 2025-08-28 | 官方维护发布，支持 ANB-AN00 |
| [MagicOS 9.0.0.200](https://club.honor.com/cn/thread-29882734-1-1.html) | 2025-12-02 | 厂商官方发布说明列出 ANN/ANP/**ANB** |
| 9.0.0.230（本机） | 实机属性报告 2026-10-11 | **已实际运行**；本轮未找到能独立复核的对应官方发布帖/下载文件 |

注意“官方支持型号+对应版本发布说明”仍然不是“可以下载完整分区镜像”。不同地区、批次与 build suffix 的 OTA 有差异。

## C. 精确 Linux 5.10.236 的 Google 开源参考

[Android 12 / 5.10 官方 deprecated GKI builds](https://source.android.com/docs/core/architecture/kernel/gki-android12-5_10-deprecated-builds) 明确记载：

| 源码标签 | 日期 | Google 源码提交 | 本项目身份 |
|---|---|---|---|
| [android12-5.10-2025-05_r1](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r1) | 2025-05-09 | `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e` | 第一轮固定候选参照 |
| [android12-5.10-2025-05_r9](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r9) | 2026-04-27 | 官方页面前缀 `aa29f5ae5084c02e29e7` | 可供后续对比不同安全修补批次 |

Google 官方的 [android12-5.10 build.config.common](https://android.googlesource.com/kernel/common/+/refs/heads/android12-5.10/build.config.common) 指明 **KMI generation 9** 与 **clang-r416183b**；用户 /proc/version 的 Android Clang **12.0.5** 也报告基于 `r416183b`。对应的 [官方编译器归档](https://android.googlesource.com/platform/prebuilts/clang/host/linux-x86/+/refs/heads/android12L-d2-s2-release/clang-r416183b/) 确实存在。

**这是同系列 GKI/编译器的出处证据，不等于 HONOR 原厂内核源代码完全等于 Google 公共 tag。** 机主固件比 2025-05_r1 更晚，可能加入厂商私有代码、回移修补和不同编译器选项。不能仅凭 `5.10.236` 或 `android12-9` 判定设备实际基于哪个 Google tag。

## D. HONOR 源码提供渠道

[HONOR Open Source Release Center](https://www.honor.com/global/opensource/) 官方声明：若产品代码无对应下载，可使用**主题 `Open Source Codes Issue`** 发信至 **support@hihonor.com** 请求其开放源码义务范围内的代码。这个邮箱和主题来自官方页面。

本轮核实**没有取得**精确标注 ANB-AN00 MagicOS 9.0.0.230 的厂商源码下载包；任何现有 GitHub 其他机型的 `taro` 或 Qualcomm 5.10 树只能作为静态参考。我们已准备可直接提交的 [请求模板](SOURCE_REQUEST_DRAFT.md)，但尚未向厂商发送邮件，不虚构厂商已回复。

## E. 非官方固件资料的边界

[第三方固件索引](https://gist.github.com/0x8008/3ef2413fbd09648b0c02a0d1cf585d22) 列有 ANB 7/8 固件版本号，另外论坛自称提供 ANB 9→8→7 降级服务。两者均未提供由本项目验过 SHA、签名与分区适配的当前 **9.0.0.230** 镜像。

**不购买/试刷所谓解锁或维修固件以替代源码取证，不将 ANP/ANN 的镜像视为 ANB。**

## 本阶段交付

已建立 [Google 官方候选源码的 Kconfig 比对工作流](../research-workflows/gki-kconfig-compare.yml)，使用真实设备 config.gz 进行离线配置兼容性评估。原件与自动比较结果将分开保存。实验成功即说明符号能否在 Google 参考树中得到解释，**不是 Root/Bootloader 解锁或厂商二进制一致的结论**。

## F. 2026-10-11 续接检索记录

对 `ANB-AN00` / `HNANB` / `9.0.0.230` / `CONFIG_HONOR_EIMA_GKI` 等再次检索，本轮未取得可核对的精确 HONOR 厂商内核源码、对应 OTA 或完整 boot 镜像。网络上的 [XDA ANB 帐号/EDL 提问](https://xdaforums.com/t/remove-honor-id-on-honor-80-pro-flat-anb-an00.4633271/) 只是求助帖子，不是 EDL/BL 解锁成功证据。

官方 [HONOR Open Source Release Center](https://www.honor.com/global/opensource/) 仍给出 `Open Source Codes Issue` 与 `support@hihonor.com` 的源码请求路径；项目未发送邮件。另已确认 Google [Android Clang r416183b 预编译目录](https://android.googlesource.com/platform/prebuilts/clang/host/linux-x86/+/refs/heads/android12L-gsi/clang-r416183b/) 对外可读；[新工作流](../GKI_SYMBOL_PROVENANCE.md) 会单独验证实际下载、Clang 版本与 Kconfig 结果，未成功前不称已复现。

## G. 参考树补丁特征核查

新建 [GKI_CVE_SOURCE_CHECK](GKI_CVE_SOURCE_CHECK.md)：直接在固定 SHA 的 Google GKI 源文件里检查三个公开漏洞修复的代码特征，保留原始 Git blob SHA。注意 Android 的补丁分支与 Linux stable 分支不可由版本号直接视为等价。本检查不是厂商补丁状态证明。
