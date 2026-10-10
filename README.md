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
| 内核配置 | /proc/config.gz **存在，显示 42,277 B；尚未提取原始内容** |

原始文本：[evidence/device/honor80pro-flat-baseline.txt](evidence/device/honor80pro-flat-baseline.txt)。
设备属性的空返回值 ro.oem_unlock_supported **不能当作支持或不支持解锁的证明**。

## 研究入口

| 资料 | 内容 |
|---|---|
| [STATUS](docs/STATUS.md) | 已确认、待确认、已失败、未开始的事实状态 |
| [DEVICE_BASELINE](docs/DEVICE_BASELINE.md) | 对用户实机记录的逐项整理与解释 |
| [KERNEL_AND_BOOT](docs/KERNEL_AND_BOOT.md) | 内核、AVB、Bootloader 与 P780 经验边界 |
| [FIRMWARE_AND_SOURCES](docs/FIRMWARE_AND_SOURCES.md) | 官方开源渠道与第三方 ANB 历史固件索引 |
| [HISTORY](docs/HISTORY.md) | 本项目成立前后的研究时间线 |
| [EVIDENCE_MANIFEST](docs/EVIDENCE_MANIFEST.md) | 已上传原件和仍缺材料 |
| [NEXT_STEPS](docs/NEXT_STEPS.md) | 最有价值的下一步实验 |
| [RESEARCH_RULES](docs/RESEARCH_RULES.md) | 如何同步更新、如何判断证据级别 |
| [CHANGELOG](CHANGELOG.md) | 阶段更新日志 |

## 当前值得研究的三条路线

1. **内核配置：** /proc/config.gz 出现在真实设备的文件列表中。下一步首先获取其原始 gzip 字节并核实是否可解压，再分析 Futex、CFI、LTO、模块、调试等选项。
2. **可信源码：** 荣耀提供官方开源发行中心，但尚未取得匹配 ANB 的 9.0.0.230 和实际 5.10.236 构建的源码。公开通用内核或其它荣耀机型代码不能冒充原厂二进制。
3. **历史固件出处：** 第三方公开索引列有 ANB-AN00 的 MagicOS 7/8 版本。这仅是追踪线索，并不证明下载链接仍有效，也不代表完整 OTA、可解锁或可降级。

## 目录

- docs/ —— 研究结论和资料说明。
- evidence/device/ —— 设备所有者提供的、经检查不包含个人唯一标识的只读基线。
- tools/ 和 .github/workflows/ —— 文档与证据的自动检查。
- CHANGELOG.md —— 每阶段同步维护。

**隐私：** GitHub 创建时此仓库为 **Public**，因此目前只提交不含 IMEI、序列号、SIM、账号和令牌的资料。未来要存 Bugreport 或全量日志请先改为 Private 并审查文件。参见 [SECURITY](SECURITY.md)。
