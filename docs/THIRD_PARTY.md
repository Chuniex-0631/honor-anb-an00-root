# 第三方来源与许可

| 组件 | 来源 | 许可／说明 |
|---|---|---|
| GhostLock-H80GT | https://github.com/yakidango-official/GhostLock-H80GT | v0.3.1 成功载荷未改动；exploit/tooling Apache-2.0 |
| Honor KernelSU LKM | https://github.com/yakidango-official/GhostLock-H80GT/tree/main/ksu | GPL-2.0；修改、构建说明和源码在上游 |
| KernelSU | https://github.com/tiann/KernelSU | 原项目许可；本版管理器 v3.3.0 |
| Shizuku API 13.1.5 | https://github.com/RikkaApps/Shizuku-API | MIT |
| AndroidX Annotation 1.3.0 | https://android.googlesource.com/platform/frameworks/support/ | Apache-2.0 |
| Magisk 工具 | https://github.com/topjohnwu/Magisk | 随原载荷的许可和归属 |
| 原始研究 | https://github.com/NebuSec/CyberMeowfia | 上游 IonStack/GhostLock 归属 |

App 自有源码为 Apache-2.0，不覆盖第三方许可。原文见 [许可目录](../app/assets/licenses) 和 [NOTICE](../NOTICE)。

本项目贡献为 ANB-AN00 验证、Shizuku 集成、状态与重入处理、界面和发布整理；不将漏洞发现、原始 exploit 或 KernelSU 当作自有原创。

## 固定上游源码

本版载荷来源：GhostLock-H80GT `v0.3.1`，提交 [`33541d1a852a926f782602aa34d79cfbffbd4bb6`](https://github.com/yakidango-official/GhostLock-H80GT/tree/33541d1a852a926f782602aa34d79cfbffbd4bb6)。其 [LKM 构建脚本与补丁](https://github.com/yakidango-official/GhostLock-H80GT/tree/33541d1a852a926f782602aa34d79cfbffbd4bb6/ksu) 固定使用 [KernelSU v3.3.0 源码](https://github.com/tiann/KernelSU/tree/v3.3.0)。设备内核树和工具链要求见该固定版本的构建说明；公共 GKI 对照不能替代厂商内核。
