# 下一阶段的最小必要操作

## 2026-10-11 最新说明：不用重新运行 ADB

用户**已经**在对话提供 honor-anb-config.gz（文件服务大小 42,277 B）。当前缺口不再是采集，而是将**已经取得的同一份原件**保存到 GitHub 的 evidence/device/honor-anb-config.gz。上传后 [配置审计工作流](../.github/workflows/kernel-config-audit.yml) 会自动校验并保存摘要。请勿重复执行此前的 ADB 提取命令，除非现有原件被证明损坏。


## P0：读取真实内核配置

真机报告显示 **/proc/config.gz（42,277 字节）**，但并未读取其内容。这是目前最直接、最值得补齐的证据。

用户已配置 Windows ADB。可以从 **Windows CMD** 运行：

~~~bat
adb devices
adb exec-out cat /proc/config.gz > honor-anb-config.gz
~~~

如果在 **Windows PowerShell** 中操作，建议使用：

~~~powershell
cmd /c "adb exec-out cat /proc/config.gz > honor-anb-config.gz"
~~~

以上只读取文件并写到电脑，不修改手机分区。注意旧版 PowerShell 的二进制重定向可能破坏 gzip 字节，因此推荐经 CMD 处理。**如果 permission denied 或文件为空，保留错误消息，不强行绕过权限。**

文件获取后，要验证 gzip 是否可解压、大小/哈希、真实内核 CONFIG 配置、与当前源码/工具链是否匹配，再进行其它实验。

## P1：查找原厂代码

通过 [荣耀开源中心](https://www.honor.com/global/opensource/) 查找 ANB/ HNANB / MagicOS 9.0.0.230 匹配源码。尚未找到之前不要给当前手机套用其它型号的 Kconfig、结构体偏移或固件文件。

## P2：固件来源与漏洞补丁

离线核对第三方 ANB 历史固件的包类型与签名，不进行强制降级或 Fastboot/EDL 写入。对当前 Linux 5.10.236 的公开修复仅先做源码级比较。

## 不需要现在做

无需安装 WSL/Linux，无需解锁 OEM，无需刷机，也无需发送 IMEI/SN。**第一份缺失的关键文件仅是 honor-anb-config.gz**。
