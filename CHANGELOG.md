# Changelog

## 2.3.0 — 待实机复验

- 界面重写：事件驱动渲染，取消每秒整屏刷新与主线程 Binder 轮询；恢复期间只更新阶段与耗时。
- 新增深色主题（`values-night`）、语义化配色与系统栏适配；正文与提示色对比度提升到 WCAG AA。
- 失败状态可在首页直接处理：显示原因分类与下一步操作，不再需要自行切到记录页。
- 进度改为五阶段时间线（完成／进行中／待授权／已中断），并显示已用时间。
- 无障碍：48dp 触控目标、状态行单一朗读语义、Tab 选中状态、阶段变化主动播报、按钮随字号自适应。
- 记录/帮助页按返回键回到首页；主按钮防抖；版本号改为读取系统 `versionName`。
- 新增 `StatusModel`、`FailureText`、`Diagnostics` 三个纯 Java 类与 3 组单元测试；执行核心与 8 个载荷字节未改动。
- 发布链路改为 `release/current.json` 单一版本入口；校验器改为"载荷与 AIDL 自 2.1.0 冻结"。
- 本版未在 ANB-AN00 实机运行，发布前必须完成实机回归（见 [审计报告](docs/AUDIT_2.3.0.md)）。

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
