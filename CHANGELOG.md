# Changelog

## Unreleased — v2.2.2 candidate (not an official release)

- 参考 KernelSU 的卡片信息层级，改成紧凑、无标签的单页滚动 UI：Root 状态、环境、恢复选项、快捷操作、诊断与帮助。
- 保留唯一底部主按钮和 `UiState` 阻断状态模型，避免检查中／恢复中重复触发。
- 移除左上角 A 标识，将“打开 Shizuku”／“打开 KernelSU”横排；后者在安全条件满足时使用已有的管理器刷新功能，否则直接打开，不修改恢复链。
- 全新「盾牌＋小钥匙」Adaptive、Legacy、Monochrome 图标，适配圆形遮罩安全区。
- 原生载荷、恢复引擎、LKM 与 loader 均不改变；候选版需要 CI 和 ANB-AN00 实机复验，并使用正式签名证书后才能发版。


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
