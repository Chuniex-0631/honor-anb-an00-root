# ANB-AN00：真实 Kconfig 与 Google 5.10.236 GKI 的对照

> 历史研究快照（截至 2026-10-11）。本页保留当时结论；当前结果见 [研究历史与发布状态](RESEARCH_HISTORY.md)。

> **候选源码验证实验，而不是 HONOR ANB-AN00 出厂内核重建。**

- 设备：HONOR ANB-AN00 / MagicOS 9.0.0.230 / Linux 5.10.236-android12-9。
- 真正设备的 config.gz SHA-256：`76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。
- 候选源码：[Google android12-5.10-2025-05_r1](https://android.googlesource.com/kernel/common/+/refs/tags/android12-5.10-2025-05_r1)，SHA `b97c62c4e7d1e80fb6a2cb0cb381f03bbcd26a4e`。
- **构建环境是 GitHub Runner 的编译器**，非实机 Android Clang 12.0.5，所以编译器版本相关 Kconfig 项的变更不代表设备原厂配置错误。

**术语校正：** “消失/不识别”仅指旧 `olddefconfig` 的输出中缺失，不能由此推断 Google 源码从未定义该符号。另须注意该实验的 `CONFIG_AS_IS_GNU=y`，后续已通过 Android Clang12 / LLVM integrated assembler 的独立控制实验，请参见新报告。独立验证见 [新实验](GKI_SYMBOL_PROVENANCE.md)，旧 JSON 保留原样。

## olddefconfig 配置对照

| 指标 | 数量 |
|---|---:|
| 原始设备配置项 | 5672 |
| Google 候选求解后配置项 | 5351 |
| 双方共享配置项 | 5343 |
| 共享配置值改变 | 5 |
| 设备原始配置项在 Google 树中消失 | 329 |
| 候选树新增配置项 | 8 |
| 原始已启用 (y/m) 而候选树缺失 | 208 |
| 候选树新增已启用 (y/m) | 5 |

## 核心配置实际对照

| 配置项 | ANB 真机原始状态 | Google 候选求解后 |
|---|---|---|
| `CONFIG_FUTEX` | `y` | `y` |
| `CONFIG_FUTEX_PI` | `y` | `y` |
| `CONFIG_RT_MUTEXES` | `y` | `y` |
| `CONFIG_CFI_CLANG` | `y` | `(missing)` |
| `CONFIG_LTO_CLANG_FULL` | `y` | `(missing)` |
| `CONFIG_SHADOW_CALL_STACK` | `y` | `(missing)` |
| `CONFIG_ARM64_PTR_AUTH` | `y` | `(missing)` |
| `CONFIG_ARM64_BTI` | `y` | `y` |
| `CONFIG_RANDOMIZE_BASE` | `y` | `y` |
| `CONFIG_MODULES` | `y` | `y` |
| `CONFIG_MODULE_SIG_FORCE` | `y` | `y` |
| `CONFIG_KALLSYMS` | `y` | `y` |
| `CONFIG_KPROBE_EVENTS` | `y` | `y` |
| `CONFIG_PANIC_ON_OOPS` | `y` | `y` |
| `CONFIG_ARCH_QCOM` | `y` | `y` |

## 原本启用但 Google 参考树不识别的配置

这些配置不应被静默抹掉；可能与 OEM 驱动、保护选项、不同 GKI 版本或厂商私有源代码有关：

- `CONFIG_ALLOC_STACK_PAGE_ON_DEMAND`（实机 `y`）
- `CONFIG_ARCH_SUPPORTS_SHADOW_CALL_STACK`（实机 `y`）
- `CONFIG_ARCH_USES_HIGH_VMA_FLAGS`（实机 `y`）
- `CONFIG_ARM64_AS_HAS_MTE`（实机 `y`）
- `CONFIG_ARM64_BTI_KERNEL`（实机 `y`）
- `CONFIG_ARM64_LSE_ATOMICS`（实机 `y`）
- `CONFIG_ARM64_MTE`（实机 `y`）
- `CONFIG_ARM64_PTR_AUTH`（实机 `y`）
- `CONFIG_ARM64_TLB_RANGE`（实机 `y`）
- `CONFIG_AS_HAS_ARMV8_4`（实机 `y`）
- `CONFIG_AS_HAS_ARMV8_5`（实机 `y`）
- `CONFIG_AS_HAS_LDAPR`（实机 `y`）
- `CONFIG_AS_HAS_LSE_ATOMICS`（实机 `y`）
- `CONFIG_AS_HAS_PAC`（实机 `y`）
- `CONFIG_AS_IS_LLVM`（实机 `y`）
- `CONFIG_BINDER_HONOR_TRANSACTION_BRIEF`（实机 `y`）
- `CONFIG_BLK_CGROUP_IOSMART`（实机 `y`）
- `CONFIG_BLK_SCH_BIGDATA`（实机 `y`）
- `CONFIG_BOOT_DETECTOR`（实机 `y`）
- `CONFIG_BOOT_DETECTOR_QCOM`（实机 `y`）
- `CONFIG_BOOT_RECOVERY`（实机 `y`）
- `CONFIG_BOOT_TIME`（实机 `y`）
- `CONFIG_CC_HAS_BRANCH_PROT_PAC_RET`（实机 `y`）
- `CONFIG_CC_HAS_BRANCH_PROT_PAC_RET_BTI`（实机 `y`）
- `CONFIG_CC_HAS_SIGN_RETURN_ADDRESS`（实机 `y`）
- `CONFIG_CC_HAVE_SHADOW_CALL_STACK`（实机 `y`）
- `CONFIG_CFI_CLANG`（实机 `y`）
- `CONFIG_CFI_CLANG_SHADOW`（实机 `y`）
- `CONFIG_CFS_BANDWIDTH_DATA_STAT`（实机 `y`）
- `CONFIG_CGROUP_IOLIMIT`（实机 `y`）
- `CONFIG_CHR_NETLINK_MODULE`（实机 `y`）
- `CONFIG_CLOSE_SHMEM_HYPERHOLD_OUT`（实机 `y`）
- `CONFIG_CMA_REPORT`（实机 `y`）
- `CONFIG_CPU_FREQ_POWER_STAT`（实机 `y`）
- `CONFIG_CVE_2023_52340`（实机 `y`）
- `CONFIG_CVE_2023_5717`（实机 `y`）
- `CONFIG_CVE_2024_26882`（实机 `y`）
- `CONFIG_DIRECT_SWAPPINESS`（实机 `y`）
- `CONFIG_DMA_BUF_INFO`（实机 `y`）
- `CONFIG_DMA_BUF_REPORT`（实机 `y`）
- `CONFIG_DM_HONOR_OPT_PARAMETER`（实机 `y`）
- `CONFIG_F2FS_BLKFRAG`（实机 `y`）
- `CONFIG_F2FS_FS_COMPRESSION_OPTM`（实机 `y`）
- `CONFIG_F2FS_FS_SIS_DISK`（实机 `y`）
- `CONFIG_F2FS_HYPER_PREALLOC`（实机 `y`）
- `CONFIG_F2FS_SMART_DISCARD`（实机 `y`）
- `CONFIG_F2FS_SMART_GC`（实机 `y`）
- `CONFIG_F2FS_TIRM_INTERRUPT`（实机 `y`）
- `CONFIG_FS_TOPIO_TRACE`（实机 `y`）
- `CONFIG_GCMA`（实机 `y`）
- `CONFIG_GPU_MEM_REPORT`（实机 `y`）
- `CONFIG_HANDSET_SYSRQ_RESET`（实机 `y`）
- `CONFIG_HAS_LTO_CLANG`（实机 `y`）
- `CONFIG_HAVE_ARCH_KASAN_HW_TAGS`（实机 `y`）
- `CONFIG_HIDESYMS_CFI_JT`（实机 `y`）
- `CONFIG_HN_ASYNC_BINDER_THREAD_PRIO_RT`（实机 `y`）
- `CONFIG_HN_BOOSTER`（实机 `y`）
- `CONFIG_HN_CGROUP_WORKINGSET`（实机 `y`）
- `CONFIG_HN_DIE_CATCH`（实机 `y`）
- `CONFIG_HN_DPIMARK_MODULE`（实机 `y`）
- `CONFIG_HN_EMCOM_NSTACK`（实机 `y`）
- `CONFIG_HN_KERNEL_STP`（实机 `y`）
- `CONFIG_HN_LOCKDUMP_V3`（实机 `y`）
- `CONFIG_HN_MEMCHECK`（实机 `y`）
- `CONFIG_HN_NETFILTER_MODULE`（实机 `y`）
- `CONFIG_HN_NETWORK_AWARE`（实机 `y`）
- `CONFIG_HN_NETWORK_MEASUREMENT`（实机 `y`）
- `CONFIG_HN_NETWORK_QOE`（实机 `y`）
- `CONFIG_HN_NSTACK_FI`（实机 `y`）
- `CONFIG_HN_PACKET_FILTER_BYPASS`（实机 `y`）
- `CONFIG_HN_PERF_EVENT_MUX_INTERVAL_WRITE`（实机 `y`）
- `CONFIG_HN_QOS_THREAD`（实机 `y`）
- `CONFIG_HN_ROOT_SCAN`（实机 `y`）
- `CONFIG_HN_ROOT_SCAN_KCODE`（实机 `y`）
- `CONFIG_HN_RW_SEM_DEBUG`（实机 `y`）
- `CONFIG_HN_SCHED_TRACE_ENHANCEMENT`（实机 `y`）
- `CONFIG_HN_SET_ASYNC_BINDER_RT`（实机 `y`）
- `CONFIG_HN_SMARTCARE_FI`（实机 `y`）
- `CONFIG_HN_SYN_LINEAR_RETRY`（实机 `y`）
- `CONFIG_HN_VIP_THREAD_BUILDIN`（实机 `y`）
- `CONFIG_HN_VIP_TO_TA_BUILDIN`（实机 `y`）
- `CONFIG_HN_VIP_WQ_BUILDIN`（实机 `y`）
- `CONFIG_HN_VSIM`（实机 `y`）
- `CONFIG_HN_WIFI`（实机 `y`）
- `CONFIG_HN_WIFIPRO`（实机 `y`）
- `CONFIG_HN_WIFIPRO_PROC`（实机 `y`）
- `CONFIG_HN_ZEROHUNG_ENHANCEMENT`（实机 `y`）
- `CONFIG_HONOR_ALLOC_STATS`（实机 `y`）
- `CONFIG_HONOR_BASTET_QCOM`（实机 `y`）
- `CONFIG_HONOR_CACHE_DISK`（实机 `y`）
- `CONFIG_HONOR_CFI_POINTER_INFO`（实机 `y`）
- `CONFIG_HONOR_DUBAI_COMMON`（实机 `y`）
- `CONFIG_HONOR_DUBAI_GPU_COMMON`（实机 `y`）
- `CONFIG_HONOR_EIMA`（实机 `y`）
- `CONFIG_HONOR_EIMA_FS`（实机 `y`）
- `CONFIG_HONOR_EIMA_GKI`（实机 `y`）
- `CONFIG_HONOR_EIMA_STATIC_MEASURE`（实机 `y`）
- `CONFIG_HONOR_EMCOM`（实机 `y`）
- `CONFIG_HONOR_EMMC_VENDOR_MODE`（实机 `y`）
- `CONFIG_HONOR_F2FS_DSM`（实机 `y`）
- `CONFIG_HONOR_FEATURE_PRINT_PID_NAME`（实机 `y`）
- `CONFIG_HONOR_FILEMAP_TRACE`（实机 `y`）
- `CONFIG_HONOR_HIDESYMS`（实机 `y`）
- `CONFIG_HONOR_HOTFILE_DETECT`（实机 `y`）
- `CONFIG_HONOR_IO_MEASURE`（实机 `y`）
- `CONFIG_HONOR_MSS_AUTO_CHANGE`（实机 `y`）
- `CONFIG_HONOR_NWEVAL`（实机 `y`）
- `CONFIG_HONOR_PERF_CTRL`（实机 `y`）
- `CONFIG_HONOR_PGO_CLANG`（实机 `y`）
- `CONFIG_HONOR_PGPRESS`（实机 `y`）
- `CONFIG_HONOR_PROC_CHECK_ROOT`（实机 `y`）
- `CONFIG_HONOR_PSI`（实机 `y`）
- `CONFIG_HONOR_QOS_INTF_PERF_V1`（实机 `y`）
- `CONFIG_HONOR_QOS_INTF_PERF_V2`（实机 `y`）
- `CONFIG_HONOR_RPHONE_DEBUG`（实机 `y`）
- `CONFIG_HONOR_RTG`（实机 `y`）
- `CONFIG_HONOR_RTG_AUX`（实机 `y`）
- `CONFIG_HONOR_RTG_CUSTOM`（实机 `y`）
- `CONFIG_HONOR_RTG_DEBUG`（实机 `y`）
- `CONFIG_HONOR_RTG_FOCUS_CHANGE`（实机 `y`）
- 其余 88 项见 JSON 原始清单。

## 原本共享但取值改变的选项（前 80 项）

- `CONFIG_AS_VERSION`: `120005` → `23800`
- `CONFIG_CC_VERSION_TEXT`: `"Android (7284624, based on r416183b) clang version 12.0.5 (https://android.googlesource.com/toolchain/llvm-project c935d99d7cf2016289302412d708641d52d2f7ee)"` → `"Ubuntu clang version 14.0.0-1ubuntu1.1"`
- `CONFIG_CLANG_VERSION`: `120005` → `140000`
- `CONFIG_LLD_VERSION`: `120005` → `140000`
- `CONFIG_LTO_NONE`: `n` → `y`

## 结论与禁止误判

- 这份源码是 Google GKI 的**公共参照系**；5.10.236 一致**不足以证明** HONOR 的 `gd671cf8c2b02` 对应同一提交或没有后移补丁。
- 共享配置数高只能说明 Kconfig 的符号覆盖率，不能推导 `vmlinux` 二进制相等；被移除的启用配置尤其表明存在来源差异。
- `olddefconfig` 既不编译运行内核，也不验证任何 CVE / Root / Bootloader 解锁；实际目标机仍为 locked/green。
- Google 官方 GKI 5.10.236 r1 不等于可刷写 HONOR boot.img。**不要将 Google 的通用镜像写入真实 ANB 手机。**

## 下一步

1. 申请或检索与 `ANB-AN00 9.0.0.230(CHNC00E230R2P5)` 完全匹配的荣耀源码，尤其是 Google 参考树无法解释的已启用 Kconfig 符号。
2. 若要研究具体 CVE，只离线查相应补丁的真实源码状态；在取得可信 OEM 二进制前不给出具体漏洞利用或符号偏移。
3. 如需排除编译器差异，另用实际 `clang-r416183b` 在相同公共源码上重跑 `olddefconfig`，不要用宿主版 Clang 结果作为原厂一致性证明。
