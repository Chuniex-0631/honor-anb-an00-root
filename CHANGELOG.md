# Changelog

## Unreleased — UI rebuild candidate (no signed APK)

- 参考 KernelSU 的信息层级，重建首页／设置／记录三个页面，支持浅色／深色主题和滚动布局。
- 统一 Root 状态、按钮、失败和等待授权的状态模型（`UiState`），新增 JVM 回归测试。
- 首页仅保留唯一恢复按钮；调试授权、管理器刷新移至设置页；最近日志限 32,000 字符，完整内容仍可导出。
- 不更改内核载荷或恢复模板。仅为未签名源码候选版，需实机 UI 验收才能正式发布。


## 2.2.0 — 2026-11-01

- 新增“允许调试工具使用 Root”，默认关闭，开启时说明 ADB／Shizuku 的 shell 授权范围。
- 记住选择并在恢复开始时固定；恢复中或模块已加载时锁定，提示完整重启后修改。
- 原模板按选择使用 allow_shell；8 个载荷与其余加载参数不变，已有尝试不重新加载。
- 新增真实模板两分支回归测试，更新指南与验证边界。新增开启模式尚待实机复验。

## 2.1.0 — 2026-10-31

- Publish ANB Root for HONOR 80 Pro Flat / ANB-AN00, MagicOS 9.0.0.230.
- Home, Records and Help; fixed primary action, setup guidance and local diagnostics.
- Execution core and device payloads preserved from verified 2.0.1.
- Public source, pinned dependencies, build instructions, checksums and attribution.
- Repository renamed from honor-anb-an00-research; private historical metadata sanitized.

## 2.0.1 — 2026-10-31

- Stop KernelSU Manager before loading and start a fresh process after completion.
- Add manager-only refresh without recovery; verified working/LKM display.

## 2.0 — 2026-10-31

- Use Shizuku UserService exclusively, removing the fixed TCP client/private-key import.
- Add prerequisite gates, foreground progress, per-boot tracking and completion checks.

Early research: [archived changelog](docs/RESEARCH_CHANGELOG.md).
