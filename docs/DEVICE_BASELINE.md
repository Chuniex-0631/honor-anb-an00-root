# 实体机基线与精确构建身份

来源为用户上传的 [ADB 只读文本](../evidence/device/honor80pro-flat-baseline.txt)，采集时间 2026-10-11。该文本为从附件读取后存入 Git 的**文本转录**；目前尚未独立比较两份文件的原始字节和 SHA-256。

## 核心属性

| 属性 | 值 |
|---|---|
| ro.product.model | ANB-AN00 |
| ro.product.device | HNANB |
| ro.product.board / ro.board.platform | taro / taro |
| ro.build.display.id | ANB-AN00 9.0.0.230(CHNC00E230R2P5) |
| ro.build.fingerprint | HONOR/ANB-AN00/HNANB:15/HONORANB-AN00/9DLDLD230CHNC00E230:user/release-keys |
| ro.build.version.release / sdk | 15 / 35 |
| ro.build.version.security_patch | 2026-08-01 |
| ro.vendor.build.security_patch | 2026-08-01 |
| ro.build.version.emui | MagicOS_9.0.0 |
| ro.boot.verifiedbootstate | green |
| ro.boot.flash.locked | 1 |
| ro.boot.vbmeta.device_state | locked |
| ro.oem_unlock_supported | 空结果（不是 0，也不是 1） |

## 内核

- uname 及 /proc/version：Linux **5.10.236-android12-9-gd671cf8c2b02**；ARM64 / SMP PREEMPT。
- 内核构建字符串时间：**2026-08-11 02:55:06 UTC**，不是系统 OTA 推送时间。
- 编译器：Android (7284624, based on r416183b) **clang version 12.0.5**；LLD 12.0.5。
- /proc/config.gz：列表显示 **42,277 B**，权限模式 -r--r--r--；用户已成功将 gzip 导出并上传至 [仓库](../evidence/device/honor-anb-config.gz)，解压检查成功，见 [配置报告](KERNEL_CONFIG_REPORT.md)。
- /sys/kernel/config.gz：不存在。

**注意：** 名称含 android12-9 的 kernel 与 Android 15 用户空间并不矛盾；不能仅凭内核命名假定设备运行 Android 12。

## 未取得

已归档原始 config.gz 内容；仍缺 Fastboot getvar、分区映射、原厂 boot.img、完整 OTA、官方对应源码或可用于本机的符号布局。
