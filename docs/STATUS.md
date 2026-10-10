# 研究状态总表 — 2026-10-11

资料等级：**A = 用户 ANB-AN00 实机**；**B = 厂商官方网页**；**C = 第三方网络线索**；**D = 推断、尚需验证**。

| 命题 | 状态 | 证据/限制 |
|---|---|---|
| 型号 ANB-AN00、代号 HNANB、平台 taro | **实机确认** | A，ADB baseline |
| MagicOS 9.0.0.230、Android 15、补丁 2026-08-01 | **实机确认** | A |
| 内核 Linux 5.10.236、Android Clang 12.0.5 | **实机确认** | A |
| Flash locked、VBMeta locked、AVB green | **实机确认** | A，不代表任何已解锁能力 |
| /proc/config.gz 原件 | **已入仓、gzip 解压成功、SHA-256 已存档** | A，42,277 B、5,672 项；[报告](KERNEL_CONFIG_REPORT.md) |
| ro.oem_unlock_supported 的结果为空 | **实机确认** | A，不能解释为空 false |
| Futex PI / RT Mutex | **均启用** | A，`CONFIG_FUTEX_PI=y`、`CONFIG_RT_MUTEXES=y`；**非漏洞证据** |
| 荣耀官方开源发行渠道存在 | **确认** | B，官网指定 `Open Source Codes Issue` 主题可请求源码，尚无 ANB 当前版本的源码包 |
| Google 公共 GKI `android12-5.10-2025-05_r1` | **官方源码身份与版本已验证** | B，5.10.236，源码固定 SHA `b97c62c4...`；**不是厂商生产内核证据** |
| Google Clang `r416183b` | **官方工具链存在且标识相符** | B + A，不能推出二进制完全一致 |
| Google GKI 公共 Kconfig（宿主 Clang14 历史实验） | **成功、原始证据保留** | 5672→5351，变化 5、缺失 329（原启用 208）、新增 8；非 OEM 源码 |
| Google GKI 公共 Kconfig（匹配 Android Clang12.0.5 r416183b） | **成功、JSON 永久归档** | 5672→5366，变化 0、缺失 311（原启用 193）、新增 5；仍非 OEM 源码 |
| 旧实验 208 项已启用缺失符号的 Google 参考树声明扫描 | **成功、JSON 永久归档** | 扫描 1580 个 Kconfig：28 项有定义，180 项未找到定义；不证明全部为 HONOR 独有 |
| 第三方 ANB 历史固件索引 | **发现** | C，未验证下载、签名、包类型 |
| 适用于当前系统的 Root 提权漏洞 | **未知** | 无精确修补/运行证据 |
| Fastboot 与 EDL / Sahara 交互 | **尚未测试** | 不能挪用 P780 的测试结果 |
| Bootloader 解锁、Root、修改分区 | **均未达成** | 无成功记录 |

**续接核验：** GitHub API 确认仓库为 Private；Actions #38073627910（GKI 对照）与 #38073733732（归档审计）均为 success。新的符号定义与 Clang12 实验已由 Actions #38076177418 **成功**、#38076601768 **归档成功**。完整数据和旧实验差异见 [GKI_SYMBOL_PROVENANCE](GKI_SYMBOL_PROVENANCE.md)。

**优先任务：** 配置采集已完成；寻找当前版本匹配源码和补丁，核实真正的启动链及可复现性。继续离线优先，不使用未经适配的刷机文件。详见 [下一步](NEXT_STEPS.md)。

## 2026-10-11 配置分析的实际成果

- 原始文件：[honor-anb-config.gz](../evidence/device/honor-anb-config.gz)，**42,277 B**；解压 **191,897 B**；SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。
- 解析 **5,672** 个 Kconfig 符号（y=2,044、m=0、n=3,525、其它取值=103），不把缺失符号自动等同于 n。
- **运行机制**：Futex PI/RT Mutex 启用；**现代缓解**：CFI、Full LTO、Shadow Call Stack、Pointer Authentication、BTI、KASLR、Strong Stack Protector、SELinux/SECCOMP 均启用。
- **其它重要设置**：`CONFIG_MODULE_SIG_FORCE=y`、`CONFIG_DEBUG_INFO_DWARF4=y`、`CONFIG_KPROBE_EVENTS=y`、`CONFIG_KALLSYMS_ALL=y`、`CONFIG_PANIC_ON_OOPS=y`；这些只能说明配置或编译条件，不代表 Shell 可任意读取符号或载入模块。
- [真实配置摘要](KERNEL_CONFIG_REPORT.md)、[分类研究报告](KERNEL_SECURITY_PROFILE.md) 与 [JSON 证据](../evidence/reports/anb-config-audit.json) 均在 GitHub Actions 中生成并保存。
- **未验证**：原厂二进制代码是否含特定漏洞、Bootloader 解锁途径、无 Root 的动态调试权限、可被第三方签名模块加载的实际状态。


## 208 项缺失配置的新归类

[厂商差异报告](VENDOR_DELTA_TRIAGE.md) 已从真实 diff 中分类 **208 项原本启用却未保留在 Google GKI 参考树求解配置中的符号**：荣耀/HN 命名 84 项、CVE 命名 3 项、编译器/内存/ARM64 26 项、Qualcomm/平台 4 项、内存/IO 29 项、启动/诊断 10 项、其它 52 项。**分类仅凭名称，不是源代码实现确认**。

具体三个 CVE 开关与公开漏洞公告的关系见 [CVE 配置线索](CVE_CONFIG_FLAGS.md)。无任何配置项本身能证明该机型可提权或已可绕过厂商安全机制。

## 最新源码调查

详见 [官方出处、更新历史与请求路径](SOURCE_PROVENANCE.md)，以及 [Google GKI 配置对照方法](GKI_COMPARISON_METHOD.md)。本阶段不把公共参考树当作厂商内核，也不将 Google 的原生 GKI boot.img 用于 ANB 真机。
