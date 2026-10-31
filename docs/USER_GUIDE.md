# 使用指南

从 [正式发布页](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest) 下载 APK，已有旧版时覆盖安装。核对 [兼容范围](COMPATIBILITY.md)。

## 第一次使用

1. 安装官方 [Shizuku](https://shizuku.rikka.app/download/) 和 [KernelSU v3.3.0](https://github.com/tiann/KernelSU/releases/tag/v3.3.0)。
2. 在 Shizuku 中通过无线调试完成配对并启动；本工具要求 shell 模式。
3. 打开 ANB Root，允许 Shizuku 访问。按钮会引导完成缺少的准备步骤。
4. 检查通过后点“完成首次准备”，然后点“恢复 Root”。
5. 如等待授权，在 KernelSU → 超级用户中允许 ANB Root，再返回验证。

## 每次重启后

解锁手机、等待开机约 90 秒 → 重新启动 Shizuku → 回到 ANB Root 恢复。不是后台自动开机 Root。

## 页面与失败处理

- 首页：准备状态、进度与主要按钮。
- 记录：本地诊断、用户选择位置导出、单独刷新 KernelSU。
- 帮助：首次使用、重启流程、问题和许可证。

管理器用于授权，本工具自动完成定制模块加载。不要并发运行其他恢复链。失败或超时先保存诊断，再按提示重新启动系统。

关闭开发者选项、无线调试或撤销配对后，需要先在手机上恢复 Shizuku 的启动条件。免电脑工作流仍需要调试授权。
