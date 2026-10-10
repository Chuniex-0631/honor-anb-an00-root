# 研究时间线

| 日期 | 已完成的事项 | 尚未完成 |
|---|---|---|
| 2026-10-10 | 决定单独研究 Honor 80 Pro 直屏版；辨别 ANB 与 ANP，回顾官方开源渠道及第三方历史固件索引。 | 未下载或验证完整固件；未运行任何 Root/解锁实验。 |
| 2026-10-11 | 机主提供真实 ADB 基线；确认 MagicOS 9.0.0.230、Android 15、安全补丁 2026-08、Linux 5.10.236、Clang 12.0.5、Locked/Green，以及 /proc/config.gz 存在。 | 还需验证读取 config.gz，未获取 boot.img 或精确源码。 |
| 2026-10-11 | 新建 GitHub 独立仓库，并将前述**已有事实与调查线索**分专题入档。 | 当时尚未取得 config.gz。 |
| 2026-10-11 | 机主上传原始 `honor-anb-config.gz`，GitHub Actions 成功验证 42,277 B gzip、5,672 项 Kconfig，并归档自动报告与精选配置 JSON。 | 仍需匹配的 HONOR 原厂源码、固件二进制、补丁验证；Root/BL 解锁仍未完成。 |

早期 [P780 研究仓库](https://github.com/Chuniex-0631/p780-kernel-research) 只作工作方法参考；其设备证据不归入本机历史。

| 2026-10-11 | 检索并核实 HONOR 官方机型表、9.0.0.120/175/200 发布记录、Google 5.10.236 GKI 2025-05_r1 的固定 SHA、Android Clang r416183b；建立候选 GKI Kconfig 对比实验和源码请求草稿。 | 厂商匹配 9.0.0.230 的私有补丁/源码尚无；首次 Google Git 克隆遇 HTTP 502，工作流正在改进下载重试。 |

| 2026-10-11 | 完成 Google GKI 5.10.236 官方源码的 Kconfig 对照：共享值变化 5，缺失 329（其中实机启用 208），新增 8；从 208 项识别 HONOR/HN 命名 84、CVE 命名 3。证据与报告已归档。 | 原厂精确源码缺失；三项 CVE 配置不能证明仍有漏洞，BL 和 Root 状态没有变化。 |
