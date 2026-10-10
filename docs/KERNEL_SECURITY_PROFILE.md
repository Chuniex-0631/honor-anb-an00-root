# ANB-AN00 内核配置专题：实机证据与安全边界

> 2026-10-11 | 基于本仓库真实 gzip 文件，由 GitHub Actions 自动解析；不是其他机型的 Kconfig。

- 设备：HONOR ANB-AN00 / HNANB；MagicOS 9.0.0.230，Android 15；Linux 5.10.236。
- 原始 gzip：**42277 B**；解压后：**191897 B**；SHA-256：`76b2a150f18baf42547b5989b80933c57a8e77fc464a05d04804577c9ac5e6c3`。
- 配置总数：**5672**；`y=2044`、`m=0`、`n=3525`、其它非布尔值 `103`。
- `not present` 表示该名称不在配置中，**不等于** `n`；非布尔值不向公共报告复制具体字符串。

## Futex and locking - prerequisite, not proof of vulnerability

| Kconfig key | 实际状态 |
|---|---|
| `CONFIG_FUTEX` | `y` |
| `CONFIG_FUTEX_PI` | `y` |
| `CONFIG_RT_MUTEXES` | `y` |
| `CONFIG_PI_FUTEX` | `not present` |
| `CONFIG_PREEMPT` | `y` |
| `CONFIG_PREEMPT_VOLUNTARY` | `n` |
| `CONFIG_PREEMPT_NONE` | `n` |
| `CONFIG_PANIC_ON_OOPS` | `y` |

## Compiler and memory integrity hardening

| Kconfig key | 实际状态 |
|---|---|
| `CONFIG_CFI_CLANG` | `y` |
| `CONFIG_CFI_PERMISSIVE` | `n` |
| `CONFIG_LTO_CLANG` | `y` |
| `CONFIG_LTO_CLANG_FULL` | `y` |
| `CONFIG_LTO_CLANG_THIN` | `n` |
| `CONFIG_SHADOW_CALL_STACK` | `y` |
| `CONFIG_ARM64_PTR_AUTH` | `y` |
| `CONFIG_ARM64_BTI` | `y` |
| `CONFIG_RANDOMIZE_BASE` | `y` |
| `CONFIG_STACKPROTECTOR` | `y` |
| `CONFIG_STACKPROTECTOR_STRONG` | `y` |
| `CONFIG_HARDENED_USERCOPY` | `y` |
| `CONFIG_FORTIFY_SOURCE` | `not present` |
| `CONFIG_STRICT_KERNEL_RWX` | `y` |
| `CONFIG_STRICT_MODULE_RWX` | `y` |
| `CONFIG_ARM64_PAN` | `y` |
| `CONFIG_ARM64_UAO` | `y` |

## Isolation and kernel interfaces

| Kconfig key | 实际状态 |
|---|---|
| `CONFIG_SECURITY` | `y` |
| `CONFIG_SECURITY_SELINUX` | `y` |
| `CONFIG_SECURITY_SELINUX_DEVELOP` | `y` |
| `CONFIG_SECCOMP` | `y` |
| `CONFIG_SECCOMP_FILTER` | `y` |
| `CONFIG_BPF` | `y` |
| `CONFIG_BPF_SYSCALL` | `y` |
| `CONFIG_BPF_JIT` | `y` |
| `CONFIG_BPF_JIT_ALWAYS_ON` | `y` |
| `CONFIG_BPF_UNPRIV_DEFAULT_OFF` | `n` |
| `CONFIG_USER_NS` | `n` |
| `CONFIG_NAMESPACES` | `y` |
| `CONFIG_ANDROID_BINDER_IPC` | `y` |
| `CONFIG_SECURITY_DMESG_RESTRICT` | `n` |
| `CONFIG_DEVMEM` | `n` |

## Introspection and kernel metadata

| Kconfig key | 实际状态 |
|---|---|
| `CONFIG_IKCONFIG` | `y` |
| `CONFIG_IKCONFIG_PROC` | `y` |
| `CONFIG_KALLSYMS` | `y` |
| `CONFIG_KALLSYMS_ALL` | `y` |
| `CONFIG_FTRACE` | `y` |
| `CONFIG_FUNCTION_TRACER` | `n` |
| `CONFIG_KPROBES` | `y` |
| `CONFIG_KPROBE_EVENTS` | `y` |
| `CONFIG_DEBUG_FS` | `y` |
| `CONFIG_DEBUG_INFO` | `y` |
| `CONFIG_DEBUG_INFO_DWARF4` | `y` |
| `CONFIG_DEBUG_INFO_BTF` | `n` |
| `CONFIG_KASAN` | `n` |
| `CONFIG_KCSAN` | `not present` |

## Modules and device platform

| Kconfig key | 实际状态 |
|---|---|
| `CONFIG_MODULES` | `y` |
| `CONFIG_MODVERSIONS` | `y` |
| `CONFIG_MODULE_SIG` | `y` |
| `CONFIG_MODULE_SIG_FORCE` | `y` |
| `CONFIG_MODULE_UNLOAD` | `y` |
| `CONFIG_MODULE_COMPRESS` | `n` |
| `CONFIG_ARCH_QCOM` | `y` |
| `CONFIG_ARCH_TARO` | `not present` |
| `CONFIG_ARCH_LAHAINA` | `not present` |
| `CONFIG_ARCH_HOLI` | `not present` |
| `CONFIG_ARM64` | `y` |
| `CONFIG_SMP` | `y` |

## 研究结论（严格限定范围）

- 配置中是否开启 Futex PI、RT Mutex 是判断候选漏洞的**必要前提之一**，不是漏洞存在或能利用的证明。必须取得与此固件匹配的内核源码和补丁证据。
- CFI、Full LTO、Shadow Call Stack、指针认证、BTI、KASLR、SELinux 等开关只描述**编译配置**，不能从此推导每个内核运行路径的保护是否可绕过。
- `CONFIG_PANIC_ON_OOPS=y` 时，未验证的内核崩溃触发测试可能造成系统重启或不稳定。优先进行离线研究。
- `CONFIG_MODULES=y` 与配置中 `=m` 计数为 0 可以同时成立，不能推断设备不存在动态加载的 vendor 模块。
- `CONFIG_KALLSYMS` / `CONFIG_KPROBES` / `CONFIG_FTRACE` 被设置不代表非 Root ADB shell 一定能够读取受限制的信息。
- 目前**没有真实原厂 boot.img、vmlinux、System.map 或本机可用的 CVE 利用证据**；Root 和 Bootloader 均未解锁。

## 后续建议

1. 获取本机固件完全匹配的 HONOR 内核源码和构建描述，比较源码身份、修复补丁、具体 Clang 12.0.5 工具链。
2. 对任何候选 CVE，先确认源码位置、修补状态、编译开关和运行触发条件，再决定是否值得进一步验证。
3. 保留完整原始 gzip 文件而不是依赖文字统计；`evidence/reports/anb-config-audit.json` 提供自动化的已脱敏精选开关摘要。
