# 研究历史与当前状态

| 阶段 | 结论 |
|---|---|
| 2026-10-11 基线 | 取得型号、固件、内核和 config；当时没有已验证 Root 路径 |
| 2026-10-11 GKI 对照 | 固定公共参考源码与 Clang12；不能证明 HONOR 生产代码一致 |
| 2026-10-31 临时 Root | GhostLock-H80GT 已适配链在 ANB-AN00 支持组合验证成功 |
| 2026-10-31 手机独立恢复 | Shizuku UserService，移除固定 TCP 与私钥导入依赖 |
| 2.0.1 | 修复管理器旧进程导致的界面状态问题 |
| 2.1.0 | 核心保持不变，发布用户界面与说明 |
| 2.2.0 · 2026-11-01 | 增加默认关闭的调试工具 Root 选项；模板／构建验证通过，开启模式待实机复验 |

早期档案：[研究入口快照](RESEARCH_README.md)、[原历史](HISTORY.md)、[原更新日志](RESEARCH_CHANGELOG.md)、[配置报告](KERNEL_CONFIG_REPORT.md)、[GKI 来源](SOURCE_PROVENANCE.md)、[Clang12 实验](../GKI_SYMBOL_PROVENANCE.md)、[证据清单](../EVIDENCE_MANIFEST.md)。

早期“未获得 Root”等陈述按原日期理解。当前状态以 [主页](../../README.md)、[兼容范围](../COMPATIBILITY.md)、[验证摘要](../VALIDATION.md) 为准。

公开分支清理私人邮件内部编号，统一公开提交署名；保留技术时间线。原 Git 历史仅本地备份。旧公开副本或缓存不能由历史改写撤回，见 [脱敏说明](../PRIVACY.md)。
