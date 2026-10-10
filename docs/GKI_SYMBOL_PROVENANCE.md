# GKI 符号定义与 Android Clang12 控制实验

**状态：新 CI 已启动，数字及完整性待实际运行结果核验。**

旧 Ubuntu Clang14 参考树求解：原始 5672、求解 5351，变化 5、缺失 329（原启用 208）、新增 8。`olddefconfig` 未保留某键不等于全树没有定义。旧结果 `CONFIG_AS_IS_GNU=y` 与 `CONFIG_LTO_NONE=y` 提示工具链/汇编器干扰。

## 实验设计

1. 固定 Google `android12-5.10-2025-05_r1` / `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`，扫描所有 `Kconfig*` 声明，区分 208 项在参考源码中“有声明”和“全树没找到声明”。静态声明存在不表示在 ARM64 配置求解图中可达。
2. 下载 Google 官方 Android [Clang r416183b](https://android.googlesource.com/platform/prebuilts/clang/host/linux-x86/+/refs/heads/android12L-gsi/clang-r416183b/)；必须由 CI 记录 `clang --version` 和源 commit。采用 `ARCH=arm64 LLVM=1 LLVM_IAS=1 olddefconfig` 重算，输出新 JSON，不覆盖旧 Ubuntu Clang14 历史证据。
3. 无论成功或失败，工作流把已有原始配置、日志、JSON 和报告保存为独立 Artifact。失败（例如网络、依赖缺失）不是配置不兼容的证明。

代码入口：[脚本](../tools/audit_gki_provenance.py) | [工作流](../.github/workflows/gki-symbol-toolchain.yml)。

## 边界

即便编译器版本匹配，Google GKI 仍**不是荣耀 ANB-AN00 生产内核**。本实验不生成刷机镜像、不改动实体机，也不能证明 CVE、Root 或 Bootloader 解锁结论。

## 运行记录

待核实 GitHub Actions 运行 ID、结果、报告和失败原因；未在本页写入未经检验的数据。
