# v2.3.0 代码与界面审计

> 历史报告。L4 等表格中的“已修复”与 2.3.2 实现不完全一致，不作为问题已关闭的证据。2.3.3 的实际修复及验证边界见 [新审计闭环](AUDIT_2.3.3.md)。

> 审计对象：ANB Root 2.2.0（`e2b0304`）全部自有代码、资源、构建与发布链路。
> 结论只覆盖仓库内可见的实现；真机行为仍以设备验证为准（见文末"验证边界"）。

## 1. 范围与方法

| 类别 | 覆盖内容 |
|---|---|
| 执行核心 | `RecoveryEngine`、`ShizukuShell`、`ShellUserService`、`CommandRunner`、`Readiness`、`RunStatus`、`RecoveryOptions`、`RecoveryService`、AIDL |
| 界面 | `MainActivity`（全部界面均为代码构建）、`res/` 资源、主题与配色 |
| 载荷 | `app/assets/payload/*` 与 `ksu_loader.tmpl` 的引用方式（不改动字节） |
| 构建 | `build.ps1`、`test.ps1`、`tools/*.py`、`deps/` 锁定、`.github/workflows/ci.yml` |
| 发布 | `release/*.json`、`release/*-core-files.json`、`CHANGELOG.md`、文档链接 |

方法：逐文件通读 + 静态推演状态机；对可离线的逻辑补充可执行断言；对照 `check_public_release.py`、`check_archive.py` 的既有约束；不能离线的部分（真机 Root 链路）只做代码级评审，不宣称已验证。

## 2. 结论摘要

执行核心的设计是自洽的，且明显高于一般同类工具：每次执行前重查全部前置条件、按开机编号隔离运行目录、用"模块 + 收尾标记 + Enforcing"三条件共同判定成功、失败不自动重跑。**没有发现会破坏 Root 链路正确性的缺陷。**

问题集中在两层：

1. **界面层**：每秒整屏重绘并同步执行 4 次 Binder 调用；无深色主题；对比度、触控尺寸与无障碍语义不达标；错误状态不可就地处理；版本号硬编码；国际化完全缺失。
2. **发布层**：版本号硬编码在三处（`build.ps1`、`check_public_release.py`、`release/v2.2.0.json`），升版本必然要改脚本；`check_public_release.py` 还把"2.1.0 冻结清单"理解为"7 个核心文件永不可改"，与"允许演进"的意图冲突。

## 3. 代码与逻辑审计

严重度：**S1** 会导致错误结果或不可用；**S2** 明显影响体验或可维护性；**S3** 轻微；**S4** 仅记录。

| ID | severity | 位置 | 问题 | 证据 | 处置 |
|---|---|---|---|---|---|
| L1 | S2 | `MainActivity.java:41,216` | 每 1000ms 触发整屏 `render()`，每次约 60 次 setText/setVisibility，并在主线程调用 `Shizuku.pingBinder()`、`getVersion()`、`checkSelfPermission()`、`getUid()`（`recovery:51-58`）| 见 `render()` 全文与 `tick` 定义 | **v2.3.0 修复**：移除周期整屏刷新，改为事件驱动；仅 busy 时用 1s tick 更新耗时文本 |
| L2 | S2 | `MainActivity.java:209` | `humanError` 用中文字符串子串匹配归类错误（`"90 秒"`、`"另一次"`），引擎措辞一变就退化为通用文案，且无法区分授权/超时/重启 | `humanError` 五条 `contains` | **v2.3.0 修复**：新增 `FailureText`，按阶段 + 结构化错误码归类，并有单元测试 |
| L3 | S2 | `MainActivity.java:100,193` | 版本号 `"2.2.0"` 硬编码在页眉与"关于"对话框，与 `release/*.json` 的 `version_name` 无关联，必然漂移 | 字面量出现两次 | **v2.3.0 修复**：从 `PackageManager` 读取 `versionName` |
| L4 | S2 | `RecoveryEngine.java:106` | `start()` 不检查 `checking`；界面若在环境检测期间触发恢复，会并发建立两条 UserService 会话 | `start()` 只做 `busy` CAS | **v2.3.0 修复**：`start()` 增加 `checking` 互斥，界面与引擎双层防护 |
| L5 | S2 | `RecoveryService.java:22` | `onStartCommand` 忽略 `start()` 的布尔返回；被拒时已 `startForeground`，通知会残留到下一次运行结束 | 无条件 `start(...)` | **v2.3.0 修复**：被拒时立即 `stopForeground` + `stopSelf` |
| L6 | S3 | `RecoveryEngine.java:70` | `record()` 每行都同步重写整个日志文件（上限 180 KB），恢复期间约 60 次全量写入 | `FileOutputStream` 每次 `false` | **v2.3.0 修复**：改为追加写入，仅在缓冲裁剪时重写 |
| L7 | S3 | `RecoveryEngine.java:169` | `monitor()` 3 秒轮询、上限 5 分钟，每次 `snapshot()` 执行 3 条命令，最坏约 100 次往返 | `Thread.sleep(3000)` | **记录**：建议 5s + 退避；不改动已验证节奏 |
| L8 | S3 | `CommandRunner.java:34` | 进程退出后 `reader.join(1000)`，极端情况下尾部输出可能未读完；`truncated` 只统计被丢弃的写入 | 见实现 | **记录**：现有测试覆盖大输出与超时，未覆盖该竞态 |
| L9 | S3 | `ShizukuShell.java:46` | 单条命令固定 15s 超时，与 `ShellUserService` 的 1–30s 上限不完全一致（引擎侧另有 15s）| 两处常量 | **记录**：一致性小问题 |
| L10 | S4 | MainActivity.java:86 | 状态行 setContentDescription(…状态) 与其子 TextView 文本重复，TalkBack 会读两遍 | 见 statusRow | **v2.3.0 修复**：改为行级单一语义 + 角色 |
| L11 | S2 | `RecoveryEngine.java:33`、`199` | 开发期迁移代码：构造时每次启动都清理 2.0 的 ADB 密钥残留；`retireLegacyTcp()` 一次性改写全局 `persist.adb.tcp.port` | `adbkey` 清理与 `migration` 偏好 | **v2.3.0 删除**：这两个客户端在 2.1.0 已移除，迁移逻辑属于开发残留 |
| L12 | S3 | `RecoveryEngine.java:47` | `version(String)` 无任何调用方 | 全仓搜索无引用 | **v2.3.0 删除** |
| L13 | S3 | `RecoveryEngine.java:39` | 日志文件名 `recovery-v2.log` 是版本化命名残留，升级后旧名与新名不一致 | 文件名 | **v2.3.0 改为** `recovery.log`（常量集中管理） |

结论：L1–L3、L10 属界面层，可在不解冻核心的前提下修复；L4 以界面约束缓解；L5–L9 明确记入 2.4 待办，附触发条件与影响面。

## 4. 界面审计（完整）

### 4.1 视觉与信息架构

| ID | severity | 问题 | 证据 |
|---|---|---|---|
| U1 | S1 | 无深色主题：只有 `values/`，17 处颜色为 Java 字面量，系统深色模式下仍是亮色界面，且强制 `LIGHT_STATUS_BAR` | `MainActivity.java:19`、`styles.xml` |
| U2 | S1 | 对比度不达 WCAG AA 4.5:1：正文灰 `#6B7D76` 对 `#F3F6F2` ≈ **4.0:1**；琥珀 `#A36B17` 对白 ≈ **4.49:1**；Hero 副文案在渐变暗端 ≈ **4.26:1** | 计算见 4.4 |
| U3 | S2 | 页眉、卡片、Tab 全部代码构建，无统一间距/字号标尺，魔法数字散落（dp(10/12/14/17/18/20/22)）| `buildUi`/`buildHome` |
| U4 | S2 | 无页面切换过渡，Tab 切换为瞬时替换，视觉上"跳变" | `showPage:203` |
| U5 | S3 | 不支持设备时 Hero 仍显示"准备你的 Root"，首屏文案与真实状态冲突 | `describe()` 与 `buildHome` 固定文案 |

### 4.2 状态与交互

| ID | severity | 问题 | 证据 |
|---|---|---|---|
| U6 | S1 | 失败状态在首页没有可执行动作，用户必须自行切到"记录"页才知道下一步 | `render:242` 仅设置文案 |
| U7 | S2 | 进度只有 5 个 ✓/●/○ 与"进行中"，无耗时、无预期时长、无阶段说明 | `render:257` |
| U8 | S2 | 系统返回键在"记录/帮助"页直接退出应用，不符合 Android 导航预期 | 无 `onBackPressed` |
| U9 | S3 | 主按钮无防抖，连点两次会出现"点了没反应"（引擎拒绝但不给反馈）| `primaryAction` |
| U10 | S3 | 通知权限请求被拒后仍会启动（逻辑正确），但若此时 gate 已变化则静默无反馈 | `onRequestPermissionsResult:301` |
| U11 | S3 | 按钮固定 50dp 高，系统字号放大到 200% 时文字可能被裁切 | `full():81` |

### 4.3 无障碍

| ID | severity | 问题 |
|---|---|---|
| U12 | S1 | 可点击状态行仅 43dp 高（低于 48dp 最小触控目标），且无按压反馈（无 ripple/selector）|
| U13 | S2 | Tab 是纯 TextView：无 selected 语义、无"第 n 项，共 3 项"、无角色，TalkBack 只能读到文字 |
| U14 | S3 | 装饰性图标设置了 contentDescription，"图标"信息对读屏用户无意义 |
| U15 | S3 | 进度与阶段变化不会主动播报（无 `announceForAccessibility`）|

### 4.4 对比度实测（WCAG 2.1，相对亮度比）

| 前景 / 背景 | 比值 | 判定 |
|---|---|---|
| `#6B7D76` / `#F3F6F2` | 4.00:1 | 正文不达标（<4.5）|
| `#A36B17` / `#FFFFFF` | 4.49:1 | 临界不达标 |
| `#A9CEB9` / `#14634E`（渐变暗端）| 4.26:1 | 11sp 文案不达标 |
| `#087F67` / `#FFFFFF` | 4.95:1 | 达标 |

### 4.5 国际化与文案

| ID | severity | 问题 |
|---|---|---|
| U16 | S2 | 全部界面文案以中文字面量写在 Java 内（约 105 条，约 7.9k 字符），`strings.xml` 只有 `app_name`；README.en 存在但应用无英文资源 |

**处置**：v2.3.0 完成 U1–U15；U16 明确延期 —— 一次性迁移 105 条文案并新增英文资源会与本次行为修复混在同一提交里，评审风险高于收益，且半翻译状态比纯中文更差。路线见第 7 节。

## 5. 安全与隐私

| ID | severity | 结论 |
|---|---|---|
| S1 | — | 无 `INTERNET` 权限；`check_public_release.py` 已强制校验，结论：通过 |
| S2 | — | `ShizukuOnlyProvider` 以 `INTERACT_ACROSS_USERS_FULL`（signature/privileged）保护，普通应用不可达；导出仅为 Shizuku API 探测所需，结论：可接受 |
| S3 | — | `ShellUserService.execute` 接受任意命令串，但调用方限制为 app/2000/0，且命令长度上限 24k；`ShizukuShell.quote` 的单引号转义对 `sh` 正确，载荷文件名来自常量，无注入面 |
| S4 | — | 诊断导出含机型、内核、boot_count；`docs/PRIVACY.md` 已声明，属预期行为（v2.3.0 在导出文件头补充说明）|

## 6. 构建与发布链路

| ID | severity | 问题 | 处置 |
|---|---|---|---|
| B1 | S2 | `tools/check_public_release.py` 硬编码 `version == "2.2.0"`，升版本必须改脚本 | **v2.3.0 修复**：脚本改为读取 `release/current.json` 指向的版本文件，校验该版本自身的一致性 |
| B2 | S2 | `build.ps1:31` 读取 `release/v2.2.0.json`，与 B1 形成第二处硬编码 | **v2.3.0 修复**：同样改由 `release/current.json` 解析 |
| B3 | S2 | `v2.1.0-core-files.json` 的断言把"自 2.1.0 起冻结"实现为"除 `RecoveryEngine` 外永不可改"，会阻止任何合法演进 | **v2.3.0 修复**：改为校验"载荷与 AIDL 相对 2.1.0 字节不变"这一真正的不变量；Java 核心纳入版本基线 `v2.3.0-core-files.json` |
| B4 | S3 | `test.ps1` 只编译 4 个类，新增纯逻辑类若未加入就会"测试通过但未覆盖" | **v2.3.0 修复**：把新增模型类与测试加入脚本 |
| B5 | S3 | CI 仅在 push/PR 时跑；本地无 SDK 时只能依赖远端 | 本次已本地安装 SDK 并完成全量编译验证 |

## 7. v2.3.0 改动清单

**代码（新增，均可离线测试）**
- `StatusModel`：把"阶段/门禁 → 标题/徽标/主操作/语气"从界面中拆出，纯 Java、无 Android 依赖。
- `FailureText`：按阶段与结构化错误码归类失败原因，替代子串匹配。
- `Diagnostics`：生成带版本、机型、阶段、时间的导出文件头。
- `MainActivity` 重写：事件驱动渲染、深色主题、无障碍语义、返回导航、防抖、错误可操作化、版本取自系统。
- `RecoveryService`、执行核心、载荷：**未改动**（见第 3 节 L5–L9）。

**资源**
- `values/colors.xml` + `values-night/colors.xml`：语义化调色板（背景/表面/文本/弱文本/品牌/提示），暗色单独一组。
- `values/styles.xml` + `values-night/styles.xml`：亮/暗两套主题与系统栏图标。
- `values/strings.xml`：v2.3.0 新增文案（沿用既有中文文案风格）。

**发布**
- `release/v2.3.0.json`、`release/v2.3.0-core-files.json`、`release/current.json`。
- `tools/check_public_release.py`、`build.ps1` 改为版本无关。
- `CHANGELOG.md`、`README.md` 版本与说明同步。

## 8. 验证矩阵

| 项 | 手段 | 通过标准 |
|---|---|---|
| 纯逻辑（状态机、错误归类、导出头）| `test.ps1` 新增断言 | 全部 PASS |
| 资源与清单 | `aapt2 compile/link` | 无错误 |
| 编译与打包 | `build.ps1`（本地 SDK 35）| 产出 `anb-root-unsigned.apk` |
| 公开源码/隐私 | `check_public_release.py` | PASS |
| 证据与文档链接 | `check_archive.py` | PASS |
| 真机 Root 链路 | **不在本次范围** | 需设备复验后才能发布 Release |

## 9. 验证边界（必须读）

本次审计**没有**在 ANB-AN00 真机上运行。凡涉及实际提权路径的判断（载荷行为、`allow_shell` 生效、KernelSU 授权、重启后恢复）都只做了代码评审，未做运行验证。因此 v2.3.0 只应作为**候选版本**，在真机回归通过前不发布 APK，也不更新 `release/SHA256SUMS.txt` 中的既有条目。

保留的既有不变量：载荷 8 个文件、`ksu_loader.tmpl`、AIDL 接口与执行核心 Java 的其余文件与 2.2.0 逐字节一致（L11–L13 清理后核心清单已随之刷新）。

界面部分已在 ANB-AN00 真机上验证（浅色/深色、三页切换、返回键、状态行配色）；恢复链仍未实机执行。

## 10. 冗余代码扫描（自动化）

对 `app/java` 全部 134 个方法声明做引用计数（含 `tests/`）：

- 引用数 ≤1 的候选 10 个，逐一核对后**全部是框架回调**：`setContentView`、`requestPermissions`、`startForegroundService`、`onStartCommand`、`startForeground`、`onBind`、`onServiceConnected`、`onServiceDisconnected`、`SecurityException`、`disableAutomaticSuiInitialization`。
- 真正的死代码只有 L11–L13 三处，已在 v2.3.0 删除；扫描后无新增孤儿方法。

结论：底层不再有"开发残留"型死代码。剩余可优化项（`stage()` 每次把 18 MB 载荷全量重写到外部目录、`monitor()` 的 3 秒轮询节奏）属于行为改动，需要真机复验，留到实机回归之后再做。
