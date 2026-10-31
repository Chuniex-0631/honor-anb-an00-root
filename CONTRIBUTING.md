# 贡献指南

先阅读 [支持范围](docs/COMPATIBILITY.md)、[构建说明](docs/BUILD.md)、[安全政策](SECURITY.md)。

- 新设备／固件支持需独立证据，不能仅凭 SoC、内核数字或 config 扩展。
- 执行逻辑改动注明影响、验证和失败行为，不加入无条件重试。
- 不提交设备标识、凭据、私钥、签名密钥或完整私人日志。
- 发布 APK 提供实际校验值；自行构建的签名与正式发行签名不同。
- 运行 `python tools/check_public_release.py`、`python tools/check_archive.py`、`pwsh -File test.ps1`。
