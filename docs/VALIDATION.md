# 公开验证摘要

日期：2026-10-31。公开摘要保留兼容条件、结果与哈希，不公开设备标识、运行时地址和原始私人日志。

- 在 [支持组合](COMPATIBILITY.md) 验证 Shizuku 无线调试 UserService：UID 2000、u:r:shell:s0、Seccomp 0。
- 恢复后 KernelSU 模块 Live，App 实际返回 uid=0(root)、u:r:ksu:s0，SELinux Enforcing。
- 完整重启后，由手机 App 经 Shizuku 独立恢复；机主随后再次重启并复验成功。
- 2.0 的旧管理器进程可能显示未安装；2.0.1 补回加载前停止／完成后重新启动，实测显示工作中、越狱模式、LKM。
- 2.1.0 为界面包装更新，17 项核心输入、18 个核心编译类与执行声明一致。公开摘要见 [core-files.json](../release/core-files.json)。
- 准备条件、执行身份、同次／跨次开机、路径、成功条件、UTF-8、退出码、超时、输出上限测试通过。

不是大样本成功率或所有固件保证。实际 APK 哈希和签名证书指纹见 [发布清单](../release/v2.1.0.json)。构建成功与实机利用成功是不同证据。
