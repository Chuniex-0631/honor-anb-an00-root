# HONOR ANB-AN00 研究更新日志

## 2026-10-11 — v0.1 初始化

- 通过用户授权的 GitHub 验证新仓库可读可写，同时发现仓库目前为 **Public**。
- 将 2026-10-10 前期调查的 ANB 与 ANP 区分、官方开源中心和第三方 ANB 历史固件列表整理入专题文档。
- 归档用户提供的 2026-10-11 ADB 基线文本，核对设备为 ANB-AN00/HNANB，Android 15、MagicOS 9.0.0.230、kernel 5.10.236、Clang 12.0.5、Locked/Green。
- 保留重要阴性/未知情况：ro.oem_unlock_supported 输出为空；/proc/config.gz 存在但未取得内容；未采集 Fastboot/EDL；未实现 Root 或 Bootloader 解锁。
- 设置文档链接与基线数据自动校验；开始按 P780 的研究规范长期同步 GitHub。

## 2026-10-11 — v0.2：内核配置接收与审计准备

- 用户将 honor-anb-config.gz 上传至对话，文件服务记录大小 42,277 字节。
- 本地文件执行环境异常，尚不能计算该附件的 SHA-256 或解压内容；不能据此声明任何 CONFIG 项已验证。
- 已准备 GitHub 端解压、检查和生成摘要的分析脚本与 Actions；原件进入仓库后方可开始实际分析。
- README、STATUS 与 EVIDENCE_MANIFEST 已同步标注缺口。
