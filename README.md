<div align="center">

<img src="docs/assets/banner.svg" alt="ANB Root — 荣耀 80 Pro 直屏版 ANB-AN00 本机临时 Root，基于 Shizuku 与 KernelSU" width="100%">

# ANB Root

**荣耀 80 Pro 直屏版的本机临时 Root 助手**<br>HONOR 80 Pro Flat · ANB-AN00 · Shizuku · KernelSU LKM

[![正式版](https://img.shields.io/github/v/release/Chuniex-0631/honor-anb-an00-root?style=flat-square&color=087f67&label=Release)](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest) [![Android 15](https://img.shields.io/badge/Android-15-087f67?style=flat-square)](docs/COMPATIBILITY.md) [![CI](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Chuniex-0631/honor-anb-an00-root/actions/workflows/ci.yml) [![Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-087f67?style=flat-square)](LICENSE)

**[下载 2.4.0 APK](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/download/v2.4.0/ANB-Root-2.4.0.apk)** · [发布说明](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/tag/v2.4.0) · [使用指南](docs/USER_GUIDE.md) · [English](README.en.md)

</div>

通过无线调试启动 Shizuku，授予 ANB Root 权限，再由 App 完成已适配的 GhostLock 恢复流程和 KernelSU 模块加载。**日常恢复可以在手机上完成，无需连接电脑。**

> **先确认兼容范围。** 仅支持 ANB-AN00 的指定固件与内核，不适用于 ANP-AN00 曲屏版。Root 在完整重启后失效；重启后需要重新启动 Shizuku，再由 App 恢复。本项目不解锁 Bootloader。

> **2.4.0 更换发布签名。** 2.3.5 及以前版本必须先卸载再安装，不能覆盖升级。请先导出诊断；已有 Root 或本次开机已有恢复尝试时，先完整重启，再卸载旧版、安装新版并重新授权。不要在恢复进行中清除数据或卸载。详见 [签名迁移步骤](docs/USER_GUIDE.md#从旧版迁移到-240)。Shizuku 和 KernelSU 无需卸载。

## 为日常使用整理的恢复流程

| 能做什么 | 使用体验 |
|---|---|
| 准备引导 | 检查设备、Shizuku、KernelSU 和实际执行环境，按缺项引导 |
| 恢复与验证 | 显示五阶段进度；已有模块时提供验证入口，避免重复启动恢复 |
| 权限选择 | “允许调试工具使用 Root”默认关闭，开启前说明授权范围 |
| 记录与帮助 | 本地记录、诊断导出、KernelSU 刷新，以及重启后的使用说明 |
| 外观适配 | 文字标题、深色模式、大字体、横屏与左右贯通的列表按压反馈 |

App 通过 **Shizuku UserService** 获取 shell 环境，不内置固定 5555 ADB 客户端，不导入电脑 ADB 私钥，也不申请联网权限。执行环境与验证摘要会保存在本机 App 专属目录；手动导出时由你选择保存位置，不会自动上传。

## 历史界面参考（2.3.5）

<div align="center">
<img src="docs/assets/v2.3.5-home.png" alt="ANB Root 2.3.5 首页：文字标题、四项准备检查与恢复选项" width="30%">
<img src="docs/assets/v2.3.5-records.png" alt="ANB Root 2.3.5 记录页：诊断导出与独立刷新 KernelSU" width="30%">
<img src="docs/assets/v2.3.5-help.png" alt="ANB Root 2.3.5 帮助页：首次使用与每次重启后的步骤" width="30%">
<br>
<sub>首页 · 记录 · 帮助｜2.3.5 历史截图，仅供界面参考；并非 2.4.0 截图</sub>
</div>

<details>
<summary>查看历史深色模式与 2.3.5 改进</summary>

<p align="center"><img src="docs/assets/v2.3.5-dark.png" alt="ANB Root 2.3.5 深色模式" width="32%"></p>

- 首页四个准备项使用完整的左右贯通的列表按压反馈，检查按住、松开、返回和滑动取消。
- 沿用 2.3.4 的任务、弹窗与排版修复；本版统一列表行的左右贯通反馈。
- 统一弹窗与遮罩管理，完善按钮禁用、导出保护、空记录和已有模块时的提示。
- 支持两倍字号与横屏布局；顶部保留文字标题。

详见 [2.3.5 UI 审计](docs/AUDIT_2.3.5.md) 与 [更新日志](CHANGELOG.md)。

</details>

## 支持范围

| 项目 | 当前支持 |
|---|---|
| 手机 | **荣耀 80 Pro 直屏版 / HONOR 80 Pro Flat · ANB-AN00（HNANB）** |
| 系统 | Android 15 · MagicOS **9.0.0.230** |
| 完整固件 | `ANB-AN00 9.0.0.230(CHNC00E230R2P5)` |
| 内核 | `5.10.236-android12-9-gd671cf8c2b02` |
| Shizuku | 官方 13+，通过无线调试启动的 shell 模式 |
| KernelSU | 管理器 **v3.3.0**，配合内置的荣耀定制 LKM |

机型、SDK、完整固件与内核均需匹配。相同 SoC 或相近版本号不代表兼容；升级系统后需重新确认适配。[查看完整要求 →](docs/COMPATIBILITY.md)

## 五步开始

1. **下载与安装。** 安装 [2.4.0 APK](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/download/v2.4.0/ANB-Root-2.4.0.apk)；2.3.5 及以前版本按迁移步骤卸载重装，可用 [SHA-256 校验文件](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/download/v2.4.0/SHA256SUMS.txt) 核对下载。
2. **准备依赖。** 安装官方 [Shizuku](https://shizuku.rikka.app/download/) 和 [KernelSU v3.3.0](https://github.com/tiann/KernelSU/releases/tag/v3.3.0)。
3. **启动并授权。** 在 Shizuku 中通过无线调试完成配对、启动服务，再回到 ANB Root 授予 Shizuku 权限。
4. **检查并恢复。** 解锁并等待开机约 90 秒；四项检查通过后完成首次准备，按提示恢复。有本次开机匹配记录的已加载模块只需验证；记录缺失时按提示完整重启。
5. **确认结果。** 如提示授权，在 KernelSU → 超级用户中允许 ANB Root，再返回验证，看到“Root 已验证”才算完成。

**每次完整重启：解锁并等待启动完成 → 启动 Shizuku → 打开 ANB Root 恢复。** 关闭开发者选项或撤销配对后，需要先重新配置 Shizuku 的启动条件。

<details>
<summary>“允许调试工具使用 Root”应该开启吗？</summary>

日常使用建议保持关闭。开启后，ADB、Shizuku 等以 shell 身份运行的调试工具可直接获准使用 Root，普通应用仍由 KernelSU 单独授权。

选择会被保存，在新一次恢复加载模块时使用。同一新签名覆盖升级会保留选择；从旧签名卸载重装后需重新设置；恢复进行中或模块已经加载时无法即时切换。需完整重启后，在恢复前修改。关闭此项不会撤销 KernelSU 中其他已有授权。详见 [使用指南](docs/USER_GUIDE.md)。

</details>

## 遇到问题

| 现象 | 下一步 |
|---|---|
| Shizuku 未运行／未授权 | 在 Shizuku 中重新启动或授权，再返回检查 |
| App 已验证，KernelSU 仍显示未安装 | 使用“记录 → 刷新 KernelSU”，更新管理器显示 |
| 恢复超时或连接中断 | 先保存诊断，按提示处理；不要连续重复启动 |
| 系统升级后无法继续 | 核对完整固件与内核，不要套用其他版本载荷 |

[故障排查](docs/TROUBLESHOOTING.md) · [提交问题](https://github.com/Chuniex-0631/honor-anb-an00-root/issues/new/choose) · [安全问题与脱敏](SECURITY.md)

## 验证记录与开源资料

2.4.0 梳理恢复、验证、异常处理与界面状态，修正记录丢失、授权状态变化、任务并发、后台回调和文案问题。原载荷与 AIDL 保持字节；成功判定新增原 loader 的初始化／收尾标记，并实时读回模块签名校验、SELinux、内核保护及后台服务状态。仅模块存在或取得 `uid=0` 不会直接判定完整成功。

实机结果、自动化检查及保留边界见 [2.4.0 审计与验证](docs/AUDIT_2.4.0.md)。历史版本的实机成功不自动代表新控制逻辑或所有模块组合均已验证；竞态利用与异常断电等场景仍不能保证成功。发布版本与证书以 [当前发布清单](release/current.json) 为准。

| 想了解什么 | 从这里开始 |
|---|---|
| 安装、兼容与常见问题 | [文档导航](docs/README.md) · [使用指南](docs/USER_GUIDE.md) |
| 代码、构建与贡献 | [构建说明](docs/BUILD.md) · [技术架构](docs/ARCHITECTURE.md) · [贡献指南](CONTRIBUTING.md) |
| 审计与实测边界 | [2.4.0 审计与验证](docs/AUDIT_2.4.0.md) · [验证摘要](docs/VALIDATION.md) · [2.3.5 UI 审计](docs/AUDIT_2.3.5.md) · [2.3.3 代码审计](docs/AUDIT_2.3.3.md) |
| 研究来源与公开证据 | [研究历史](docs/research/RESEARCH_HISTORY.md) · [证据清单](docs/EVIDENCE_MANIFEST.md) · [脱敏边界](docs/PRIVACY.md) |

## 致谢与许可

基于 [GhostLock-H80GT](https://github.com/yakidango-official/GhostLock-H80GT) 的已有研究成果，结合 [Shizuku](https://github.com/RikkaApps/Shizuku) 与 [KernelSU](https://github.com/tiann/KernelSU) 完成本机操作流程。感谢 [NebuSec / CyberMeowfia](https://github.com/NebuSec/CyberMeowfia) 与 [Magisk](https://github.com/topjohnwu/Magisk) 等上游项目；漏洞发现与原始载荷归属保留给原作者。

App 自有源码采用 **[Apache-2.0](LICENSE)**。第三方载荷与依赖保留各自许可，包括 KernelSU 相关组件的 GPL-2.0。详见 [NOTICE](NOTICE) 与 [第三方来源](docs/THIRD_PARTY.md)。
