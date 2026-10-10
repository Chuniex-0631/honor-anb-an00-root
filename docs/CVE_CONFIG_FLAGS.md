# ANB-AN00：三项 CVE 命名 Kconfig 的出处与真实性边界

**研究日期：2026-10-11**。这不是漏洞利用报告；我们只是在真实设备配置与公开漏洞数据库之间建立可追溯的索引。

## 真机原始证据

`evidence/device/honor-anb-config.gz` 已验证 SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。公共 GKI Kconfig 对照所生成的 [差异 JSON](../evidence/reports/anb-gki-config-delta.json) 中，出现：

- `CONFIG_CVE_2023_52340=y`
- `CONFIG_CVE_2023_5717=y`
- `CONFIG_CVE_2024_26882=y`

这些键不出现在 Google `android12-5.10-2025-05_r1` 求解后的候选配置中。这里的观察仅意味着 HONOR 实机 Kconfig 含这些**以 CVE 命名的配置项**。键名可能代表厂商开关、条件编译或补丁管理手段，**无法据此确定具体修复代码、运行条件、或目前的漏洞状态**。

## 公开 CVE 对应的上游问题

| CVE | 公开公告涉及的内核代码/问题 | 可核实信息及边界 |
|---|---|---|
| [CVE-2023-52340](https://www.cve.org/CVERecord?id=CVE-2023-52340) | Linux IPv6 路由缓存处理相关，可能造成拒绝服务 | Linux 上游曾报告 IPv6 `net/ipv6/route.c` 的资源限制问题；需检查荣耀代码的实际更改，不能凭配置键或 `5.10.236` 推断 |
| [CVE-2023-5717](https://nvd.nist.gov/vuln/detail/CVE-2023-5717) | `kernel/events` / perf 相关堆越界写，公开记录提及本地提权风险 | 公开版本修复记录涉及 5.10.x；当前设备为 5.10.236 且有同名开关，**仍没有可独立验证的荣耀对应代码与实际提权证据** |
| [CVE-2024-26882](https://ubuntu.com/security/CVE-2024-26882) | IPv4 tunnel `ip_tunnel_rcv()` 中内层报文头处理 | [Red Hat 漏洞记录](https://bugzilla.redhat.com/show_bug.cgi?id=CVE-2024-26882) 将内核 5.10.214 列为修复版本之一；当前设备是 5.10.236，**不应基于宏名声称它仍受该 CVE 影响** |

## 可以做的下一步研究

1. 取得与 `ANB-AN00 9.0.0.230(CHNC00E230R2P5)` 匹配的 HONOR Linux 源码及构建资料。
2. 搜索每个 `CONFIG_CVE_...` 的 Kconfig 定义、源文件使用位置、是否存在有条件编译的厂商修补代码及其代码来源。
3. 将实际厂商补丁与公开 Linux 修复记录逐项对照；需要保留实际 commit、原始文件与日期，不能单凭命名判断修复。
4. 继续将这些线索视作**静态配置和公开公告对应关系**。不执行实际触发步骤，不将其它机型的 PoC 视为 ANB 当前版本可复现的能力。

## 为什么优先研究它们？

这三项命名为 CVE 的实机 Kconfig 为源码溯源提供了明确搜索目标，也能检验厂商补丁是否与公共 GKI 不同。然而它们更可能帮助我们**确认保护/修复路径**，而不是直接给出 Root 或 Bootloader 解锁方案。
## 新一轮上游修复位置索引（2026-10-11）

- [CVE-2023-52340 的 Red Hat 记录](https://bugzilla.redhat.com/show_bug.cgi?id=2257979) 指向 Linux 主线修复 [`af6d10345ca76670c1b7c37799f0d5576ccef277`](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=af6d10345ca76670c1b7c37799f0d5576ccef277)：IPv6 路由缓存回收与 ICMPv6 Packet Too Big 资源耗尽；待比对 `net/ipv6/route.c` 厂商修改。
- [CVE-2023-5717](https://www.cve.org/CVERecord?id=CVE-2023-5717) 的上游修复为 [`32671e3799ca2e4590773fd0e63aaa4229e50c06`](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=32671e3799ca2e4590773fd0e63aaa4229e50c06)。[5.10-stable 补丁队列](https://www.spinics.net/lists/stable-commits/msg321171.html) 指出变更 `kernel/events/core.c`、`include/linux/perf_event.h`，利用 `group_generation` 避免继承事件组不一致；仍需与实际 HONOR 源码对照。
- [CVE-2024-26882 的 NVD 资料](https://nvd.nist.gov/vuln/detail/CVE-2024-26882) 与 [Red Hat](https://bugzilla.redhat.com/show_bug.cgi?id=2275686) 将 5.10.214 列为 Linux 5.10 稳定分支修复边界；问题是 `net/ipv4/ip_tunnel.c` 内层报文头的读取。ANB 报告 5.10.236，高于通用稳定分支修复版本，但厂商源码内容及该宏的实际语义**均未独立验证**。

三个配置开关均不是 Root、Bootloader 解锁或补丁实际缺失的证据；补丁追踪应比较真实源码/二进制实现，而非运行高风险触发程序。

## Google 固定参考树的实际代码复查

已直接读取 Google 公共镜像在 `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e` 的 `kernel/events/core.c`、`include/linux/perf_event.h`、`net/ipv4/ip_tunnel.c`、`net/ipv6/route.c`，建立 [源码特征检查报告](GKI_CVE_SOURCE_CHECK.md) 和 [含 Blob SHA 的 JSON](../evidence/reports/anb-gki-cve-static-markers.json)。perf 的 `group_generation` 修复特征存在；IPv4 tunnel 的 `pskb_inet_may_pull` 特征缺失；IPv6 GC 仍有 `rt_max_size` 路径。**这仅是 Google 参考树的代码差异，不可将其称为 ANB 生产内核漏洞。**
