# Changelog

## 2.4.0 — 2026-11-11

- 全面梳理恢复、验证、管理器刷新、前台服务及界面操作的状态转移，完善授权状态变化、缺失记录、重复任务、退出页面后回调和错误提示。
- 强化完成判定：除模块、SELinux 与 App Root 外，检查原 loader 的初始化／收尾标记，并实时读回模块签名校验、kallsyms 挂载、内核保护参数及后台服务状态；无法确认时引导保存记录并完整重启。
- 保留原 native 载荷和 AIDL 字节；对原链路异常终止与竞态利用的边界继续如实记录，不承诺全部场景成功。
- 修正诊断本地保存与手动导出的说明，完善轻更新兼容记录、故障排查、界面状态测试和发布包完整性检查。
- **更换发布签名。2.3.5 及以前版本须先导出诊断，已有 Root 或当前开机有恢复尝试时先完整重启，再卸载旧版、安装 2.4.0，并重新配置选项与 Shizuku／KernelSU 授权。Shizuku 和 KernelSU 无需卸载。** 同一新签名的后续版本可覆盖升级。
- 新证书 SHA-256：`623049d5e0d4bb3806077181e4865836e925ad79d40b683a7f65c027ca04f3b2`。迁移步骤见 [使用指南](docs/USER_GUIDE.md#从旧版迁移到-240)，验证范围见 [2.4.0 审计](docs/AUDIT_2.4.0.md)。

## 2.3.5 — 2026-11-02

- 首页四个准备项、六个常见问题及恢复选项行，采用左右贯通的矩形按压反馈；原有配色保持不变。
- 文字内边距移至行内，保留内容位置；松手淡出，卡片外轮廓统一裁切。
- 五种显示配置通过 1,467 项 Activity 断言，并逐一检查十个入口的按住、松开与滑动取消。
- 恢复逻辑、8 个载荷及 AIDL 不变；更新中英文首页、正式版截图、文档导航及问题反馈模板。

## 2.3.4 — 正式版 · 2026-11-02

- 顶部改为 ANB Root 文字标题，取消页面左上角图标与重复机型副标题。

- 修正首页四个准备项的按压遮罩边界与内边距，检查按住、松开、返回和滑动取消。
- 修复任务完成后的 ticker 遗留，区分环境检查、管理器刷新与恢复的标题、按钮和进度。
- 统一弹窗所有权，防止叠层；切到后台取消未确认弹窗，许可页面交接不留遮罩。
- 完善按钮禁用、重复导出保护、空记录、已有模块与失败阶段文案。
- 支持主操作换行、导航自适应高度、横屏滚动主操作、开孔四边安全区与页面滚动状态恢复。
- ANB-AN00 完成 15 组页面截图、四行动态按压与滑动取消；五种显示配置下 1,277 项真实 Activity 断言通过。
- 9 组 JVM／替身测试；恢复流程、8 个载荷和 AIDL 不变。本轮没有重新提权或重启。
- 2.3.3 本机验证由用户报告完成；2.3.4 本轮记录为 UI 实测，不冒充新版本 Root／重启复验。见 [UI 审计](docs/AUDIT_2.3.4.md)。

## 2.3.3 — 2026-11-02

- 严格检查机型、SDK、完整固件与内核，执行前再次核对。
- 修复失败后的操作入口与旧成功状态覆盖当前前置条件；验证／检查进度不会启动新恢复链。
- 结构化保护状态错误，明确提示保存记录并完整重启。
- 统一检查、恢复、管理器刷新与设置的任务所有权；重复启动不撤掉原任务的前台状态。
- 清理被中断的控制进程和输出流，拒绝不完整输出；更新 Shizuku UserService 缓存版本。
- 清理暂停／销毁后的界面回调；诊断导出在 I/O 线程执行，使用实际内核与开机编号。
- 状态头与日志尾部隔离，目录绑定开机编号，丢失记录但已有锁时拒绝重跑；验证保留原运行目录。
- 同步恢复选项行的禁用和无障碍状态，披露上游副作用，勘误旧审计记录。
- 新增审计回归与实际引擎／服务替身测试；8 个载荷及 AIDL 不变。本轮未进行手机提权／重启恢复复验。

## 2.3.0 — 历史候选阶段（当前状态见 2.3.3）

- 界面重写：事件驱动渲染，取消每秒整屏刷新与主线程 Binder 轮询；恢复期间只更新阶段与耗时。
- 新增深色主题（`values-night`）、语义化配色与系统栏适配；正文与提示色对比度提升到 WCAG AA。
- 失败状态可在首页直接处理：显示原因分类与下一步操作，不再需要自行切到记录页。
- 进度改为五阶段时间线（完成／进行中／待授权／已中断），并显示已用时间。
- 无障碍：48dp 触控目标、状态行单一朗读语义、Tab 选中状态、阶段变化主动播报、按钮随字号自适应。
- 记录/帮助页按返回键回到首页；主按钮防抖；版本号改为读取系统 `versionName`。
- 新增 `StatusModel`、`FailureText`、`Diagnostics` 三个纯 Java 类与 3 组单元测试；执行核心与 8 个载荷字节未改动。
- 发布链路改为 `release/current.json` 单一版本入口；校验器改为"载荷与 AIDL 自 2.1.0 冻结"。
- 删除开发期迁移残留：`adbkey` 清理、`retireLegacyTcp()` 与未使用的 `version()`；日志改为追加写入。
- 界面排版重构：首页去掉冗余卡片，检查清单加状态点与完成计数，帮助页改为步骤列表，底部导航加图标。
- 界面已在 ANB-AN00 真机验证（浅色/深色、三页切换、返回键）；恢复链仍未实机执行，发布前需完成回归（见 [审计报告](docs/AUDIT_2.3.0.md)）。

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

Early research: [archived changelog](docs/research/RESEARCH_CHANGELOG.md).
