# 兼容范围 / Compatibility

| 条件 | 发布版要求 |
|---|---|
| 型号 | ANB-AN00，荣耀 80 Pro 直屏版（HONOR 80 Pro Flat） |
| 代号 / 平台 | HNANB / taro |
| 系统 | Android 15，MagicOS 9.0.0.230 |
| 完整固件 | ANB-AN00 9.0.0.230(CHNC00E230R2P5) |
| 精确内核 | 5.10.236-android12-9-gd671cf8c2b02 |
| Shizuku | 13+，无线调试启动，shell UID 2000 |
| KernelSU 管理器 | v3.3.0 |

App 检查型号、精确内核、依赖、Shizuku 权限与实际执行环境。相同 SoC、版本数字或 config 不能替代生产实现的适配验证。

ANP-AN00、其他荣耀机型、其他固件／内核没有在本发布中宣称支持。Bootloader 解锁和永久 Root 不在本工具能力范围内。

## OTA 与功能轻更新

用户于 2026-11-11 确认，安装 **LightUpdate 26.11.0.115（FE55）** 后仍可获取 Root。该次推送的官方配置清单仅列出应用与功能组件，没有内核、boot 或系统分区镜像；它不等于更新到其他完整固件。此记录仅属于该次轻更新，不扩展到后续 OTA、其他内核或固件。更新后仍须核对上表条件；完整重启后须重新启动 Shizuku 并恢复临时 Root。
