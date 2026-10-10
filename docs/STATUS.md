# 研究状态总表 — 2026-10-11

资料等级：**A = 用户 ANB-AN00 实机**；**B = 厂商官方网页**；**C = 第三方网络线索**；**D = 推断、尚需验证**。

| 命题 | 状态 | 证据/限制 |
|---|---|---|
| 型号 ANB-AN00、代号 HNANB、平台 taro | **实机确认** | A，ADB baseline |
| MagicOS 9.0.0.230、Android 15、补丁 2026-08-01 | **实机确认** | A |
| 内核 Linux 5.10.236、Android Clang 12.0.5 | **实机确认** | A |
| Flash locked、VBMeta locked、AVB green | **实机确认** | A，不代表任何已解锁能力 |
| /proc/config.gz 存在，大小 42,277 B | **文件列表确认** | A，内容尚未读取 |
| ro.oem_unlock_supported 的结果为空 | **实机确认** | A，不能解释为空 false |
| 能否读取 config.gz 原始字节 | **待实验** | 未获得 gzip 本体 |
| 荣耀官方开源发行渠道存在 | **确认** | B，未确认有匹配本机版本的源码 |
| 第三方 ANB 历史固件索引 | **发现** | C，未验证下载、签名、包类型 |
| 适用于当前系统的 Root 提权漏洞 | **未知** | 无精确修补/运行证据 |
| Fastboot 与 EDL / Sahara 交互 | **尚未测试** | 不能挪用 P780 的测试结果 |
| Bootloader 解锁、Root、修改分区 | **均未达成** | 无成功记录 |

**优先任务：** 提取并检验设备自身的 /proc/config.gz，寻找与该版本匹配的源码；继续离线工作，不使用未经适配的刷机文件。详见 [下一步](NEXT_STEPS.md)。

## 2026-10-11 内核配置采集进度

用户已提供 honor-anb-config.gz 聊天附件（文件服务报告 **42,277 字节**），与实体机基线记录的文件大小相同。由于当前文件执行环境异常，尚未核实内容/哈希，**状态为“附件已接收、原件未入 Git、配置项未验证”**。GitHub 的 [只读分析工具](../tools/analyze_kernel_config.py) 和 [自动工作流](../.github/workflows/kernel-config-audit.yml) 已就绪，上传原件后触发审计。不得将 CONFIG_FUTEX_PI、CONFIG_CFI_CLANG 等项写成已知 y/n，除非实际报告产生。
