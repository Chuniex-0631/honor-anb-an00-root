# 已归档证据和未取得原件

| 文件/资料 | 目前状态 | 说明 |
|---|---|---|
| [honor80pro-flat-baseline.txt](../evidence/device/honor80pro-flat-baseline.txt) | **已归档文本** | 用户 2026-10-11 的真实只读 ADB 输出；源附件 1,292 B；GitHub 是可读文本转录，非已独立 SHA 验证的原始字节备份。 |
| /proc/config.gz | **未上传** | 实机仅确认文件存在，42,277 B；未读到配置文本。 |
| ANB 9.0.0.230 完整 OTA | **未取得** | 历史固件版本列表不足以证明整包存在。 |
| 匹配 ANB 的 boot.img/vendor_boot.img/vmlinux | **未取得** | 未获得原厂固件二进制。 |
| 同版本对应的荣耀内核源码 | **未取得** | 已找到官方开源渠道，具体包未验证。 |
| [第三方历史固件索引](https://gist.github.com/0x8008/3ef2413fbd09648b0c02a0d1cf585d22) | **已登记链接和版本示例** | 未取得验证过的下载文件。 |
| [荣耀开源中心](https://www.honor.com/global/opensource/) | **官方网页已找到** | 不代表存在精确设备版本源码。 |

### 隐私

仓库目前是 **Public**，所以不归档 IMEI、序列号、SIM/运营商账号、完整系统 Bugreport、含认证令牌的 OTA URL 或未经筛查的全量设备日志。未来若要保存这些原件，需要先确认私人存储方案并对 Git 历史保密。

## 2026-10-11 新上传但尚未转移至 Git 的附件

- 原始文件名：honor-anb-config.gz；聊天附件 MIME 为 application/x-gzip，报告大小 42,277 B，与 2026-10-11 ADB 目录显示相符。
- 当前状态：**仍在本次聊天附件中，尚未提交 GitHub**；由于文件执行环境错误，未取得可信 SHA-256 或成功解压的核验。
- 预定入库路径：evidence/device/honor-anb-config.gz。
- 备好的 [配置校验程序](../tools/analyze_kernel_config.py) 及 [CI](../.github/workflows/kernel-config-audit.yml) 会在原件进入仓库后生成核验报告。请勿将准备好的程序或文档误认作分析结果已经完成。
