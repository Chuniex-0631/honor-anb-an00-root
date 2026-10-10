# Google GKI 5.10.236：三个 CVE 的源码特征复查

> 仅针对固定 Google 公共参考源码 **`b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`**，不是 ANB-AN00 的生产内核、不是漏洞可利用性报告。

## 复查方法

直接使用 GitHub 连接器读取公共 [AOSP kernel/common 镜像](https://github.com/aosp-mirror/kernel_common/tree/b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e) 的固定提交及四个源码文件，对函数内部的公开上游修复特征进行定位。原始 Git blob SHA、源码链接与结构化布尔检查见 [JSON](../evidence/reports/anb-gki-cve-static-markers.json)。没有通过 Kconfig 宏名代替代码复核。

| CVE | 固定 Google 参考树中的实际观察 | 解读边界 |
|---|---|---|
| CVE-2023-5717 | `kernel/events/core.c::__perf_read_group_add()` **存在** `parent->group_generation != leader->group_generation`，失败返回 `-ECHILD`；`include/linux/perf_event.h` 有 `group_generation` 成员 | 与 [上游 perf 修复](https://www.spinics.net/lists/stable-commits/msg321171.html) 的核心检查相符，只能说明**公共 GKI 此处存在修复特征** |
| CVE-2024-26882 | `net/ipv4/ip_tunnel.c::ip_tunnel_rcv()` **未找到** 上游补丁使用的 `pskb_inet_may_pull` 调用 | 不等于厂商生产源码缺修复，也不能排除其它等效处理；仍需完整 diff |
| CVE-2023-52340 | `net/ipv6/route.c::ip6_dst_gc()` **仍包含** `entries > rt_max_size` 条件和 `fib6_run_gc` 路径 | 与 [上游删除 max_size 检查的补丁方向](https://bugzilla.redhat.com/show_bug.cgi?id=2257979) 不一致，但未完成完整修复等价性验证 |

## 可回查 Git blob

- [`kernel/events/core.c`](https://github.com/aosp-mirror/kernel_common/blob/b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e/kernel/events/core.c) — Git blob `4e571370eed0003906fd2d9ae0e8cc0bee2abce0`
- [`include/linux/perf_event.h`](https://github.com/aosp-mirror/kernel_common/blob/b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e/include/linux/perf_event.h) — Git blob `5cac2f291568a7d010f33a14617332090b06a712`
- [`net/ipv4/ip_tunnel.c`](https://github.com/aosp-mirror/kernel_common/blob/b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e/net/ipv4/ip_tunnel.c) — Git blob `f1e8f8513ecd30d286642e82e46531ed7e4f710e`
- [`net/ipv6/route.c`](https://github.com/aosp-mirror/kernel_common/blob/b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e/net/ipv6/route.c) — Git blob `d45cddd1ab5ca66e43ffeda55d51cd4404999823`

## 重要结论

- **不能把“5.10.236 高于 5.10.214”直接当作特定 Google Android GKI 分支所有补丁都存在的证明。** 对于 CVE-2024-26882，本次直接查看源码时未发现公开修复的标志调用，因此必须进行分支级源码验证，而非纯版本号推断。
- **不可以反向推出真实 ANB 9.0.0.230 存在漏洞。** 手机具有 HONOR 私有扩展和 `CONFIG_CVE_...` 三项厂商配置标志，可能实现了回移或独立修复。尚无当前生产内核二进制或厂商源码。
- 下一阶段：取得与 ANB 9.0.0.230 匹配的 OEM 内核源码、带补丁历史的源码包或可信 boot 镜像，只做离线对照。Root / Bootloader 仍然 locked/未达成。
