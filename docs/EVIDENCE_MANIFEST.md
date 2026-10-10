# 已归档证据和未取得原件

| 文件/资料 | 目前状态 | 说明 |
|---|---|---|
| [honor80pro-flat-baseline.txt](../evidence/device/honor80pro-flat-baseline.txt) | **已归档文本** | 用户 2026-10-11 的真实只读 ADB 输出；源附件 1,292 B；GitHub 是可读文本转录，非已独立 SHA 验证的原始字节备份。 |
| [honor-anb-config.gz](../evidence/device/honor-anb-config.gz) | **原始 gzip 已入 Git 并通过校验** | 42,277 B；解压 191,897 B；SHA-256 `76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3` |
| ANB 9.0.0.230 完整 OTA | **未取得** | 历史固件版本列表不足以证明整包存在。 |
| 匹配 ANB 的 boot.img/vendor_boot.img/vmlinux | **未取得** | 未获得原厂固件二进制。 |
| 同版本对应的荣耀内核源码 | **未取得** | 已找到官方开源渠道，具体包未验证。 |
| [第三方历史固件索引](https://gist.github.com/0x8008/3ef2413fbd09648b0c02a0d1cf585d22) | **已登记链接和版本示例** | 未取得验证过的下载文件。 |
| [荣耀开源中心](https://www.honor.com/global/opensource/) | **官方网页已找到** | 不代表存在精确设备版本源码。 |

### 隐私

仓库目前是 **Public**，所以不归档 IMEI、序列号、SIM/运营商账号、完整系统 Bugreport、含认证令牌的 OTA URL 或未经筛查的全量设备日志。未来若要保存这些原件，需要先确认私人存储方案并对 Git 历史保密。

## 2026-10-11 配置的实物验收与历史状态修正

- 本文件此前记录“附件已上传、尚未归档”：**已失效**。用户已将原始 `honor-anb-config.gz` 上传至本 GitHub 仓库；Actions [#38072192104](https://github.com/Chuniex-0631/honor-anb-an00-research/actions/runs/38072192104) 实际解压并生成报告。
- [KERNEL_CONFIG_REPORT](KERNEL_CONFIG_REPORT.md) 是直接读取实际 gzip 的自动结果；[KERNEL_SECURITY_PROFILE](KERNEL_SECURITY_PROFILE.md) 和 [JSON](../evidence/reports/anb-config-audit.json) 则提供分类的精选符号、统计和研究边界。
- 经过 SHA-256 对仓库**当前原件**的完整性核查；尚未和机主本地未改动的备份单独做 SHA 比对，不声称存在多个独立来源认证。
- `ro.oem_unlock_supported` 为空结果、Bootloader 仍 locked；配置可读取不是 Root 或解锁证据。

