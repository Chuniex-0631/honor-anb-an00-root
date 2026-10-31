# 固件、源码和历史调查线索

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

更新：2026-10-11。以下明确区分**实体机事实、官方渠道和第三方来源**。

## 当前实体机

- ANB-AN00 / HNANB / taro
- **9.0.0.230(CHNC00E230R2P5)**、Android 15。
- **2026-08-01** 安全补丁；内核 **5.10.236**。

**尚未获取**与该版本一致的完整官方 OTA、boot.img、vendor_boot.img、vmlinux 或源码包。

## 历史固件索引（第三方、未经校验）

[0x8008 的 Honor firmware links](https://gist.github.com/0x8008/3ef2413fbd09648b0c02a0d1cf585d22) 列有 ANB-AN00 的历史版本条目：

| 示例版本 | 当前证据 | 尚缺 |
|---|---|---|
| 7.0.0.111 / 7.0.0.202 / 7.0.0.220 | 索引列出 | 实际下载可用性、文件类型、签名/哈希 |
| 8.0.0.135 / 8.0.0.143 / 8.0.0.150 | 索引列出 | 同上 |
| 9.0.0.230 | 用户实体机当前版本 | 未在已检查的该第三方索引中确认为可获取整包 |

该来源并非荣耀官方固件认证中心。**ANB-AN00、ANN-AN00、ANP-AN00** 的名字类似却是不同的索引条目；不能跨型号用 boot.img 或降级包。更不能把“有历史版本条目”当作“BL 已解锁或已取得厂商签名镜像”。

## 荣耀官方源码渠道

[HONOR Open Source Release Center](https://www.honor.com/global/opensource/) 明确提供属于其开放源码发布义务范围的代码下载/申请渠道；如未找到对应代码，官方引导使用 [联系支持](https://www.honor.com/global/support/contact-us/)。

**尚未确认**官方网站上是否存在对应 ANB-AN00、MagicOS 9.0.0.230 和 kernel 5.10.236 的完整源码。必须核对型号、构建版本、commit 和发布许可证。

## 独立研究顺序

首先获取实机配置，再比对官方源码与当前补丁，之后才考虑离线镜像解析、内核对象编译或漏洞适用性。锁定 Bootloader 的设备不应直接测试不可信固件。

## 本轮新增的官方验证来源（2026-10-11）

- [HONOR 官方发布说明 8.0.0.150](https://club.honor.com/cn/thread-28854024-1-1.html)（2024-09-21）、[9.0.0.120](https://club.honor.com/cn/thread-29163791-1-1.html)（2025-01-16/17）、[9.0.0.175](https://club.honor.com/thread-29483449-1-1.html)（2025-06-11）、[9.0.0.200](https://club.honor.com/cn/thread-29882734-1-1.html)（2025-12-02）均列出 ANB-AN00 为适配机型。
- [Google 官方 5.10.236 GKI 发布表](https://source.android.com/docs/core/architecture/kernel/gki-android12-5_10-deprecated-builds) 指明参考源标签 `android12-5.10-2025-05_r1` 对应 SHA `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`。
- 官方 [android12-5.10 build.config.common](https://android.googlesource.com/kernel/common/+/refs/heads/android12-5.10/build.config.common) 对应 KMI generation 9 与 Clang `r416183b`；这不代表当前 HONOR 二进制使用原样 Google 源码。
- **用户实际 9.0.0.230** 的对应官方下载包及可验证的 HONOR 完整内核源码仍未取得。具体文献证据参见 [SOURCE_PROVENANCE](SOURCE_PROVENANCE.md)。

## 补充官方版本轨迹（2026-10-11）

检索到荣耀官方 [9.0.0.157（2025-02-24）](https://club.honor.com/cn/thread-29245413-1-1.html) 和 [9.0.0.187（2025-08-28）](https://club.honor.com/cn/thread-29639896-1-1.html) 发布说明，两者均列出 ANB-AN00；这是 9.0.0.120→175→200 之间的官方版本线索，**不是 9.0.0.230 完整 OTA 或 OEM 源码下载证据**。
