# 构建说明

正式 APK 在 [Releases](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest)，以 [发布清单](../release/current.json) 指向的版本文件中的 SHA-256 和证书指纹为准。签名私钥不在仓库。

## 2.4.0 签名迁移

原发布签名文件已无法取得，2.4.0 经维护者确认更换发布签名。新证书 SHA-256：

```text
623049d5e0d4bb3806077181e4865836e925ad79d40b683a7f65c027ca04f3b2
```

2.3.5 及以前版本不能覆盖升级为 2.4.0，必须按 [使用指南](USER_GUIDE.md#从旧版迁移到-240) 导出诊断、处理当前开机恢复状态、卸载旧版再安装。卸载会清除 App 设置与私有记录；已有 Root 时建议先完整重启，避免原开机锁和已加载模块与新 App 的空记录冲突。Shizuku、KernelSU 无需卸载。后续使用相同新证书的版本可覆盖升级。

签名私钥保留在仓库外，不提交到源码或 CI。验证下载 APK 的证书时使用 Android SDK `apksigner verify --verbose --print-certs ANB-Root-2.4.0.apk`；证书指纹和 APK 文件 SHA-256 是不同校验值，均应与 [发布元数据](../release/current.json) 核对。

## 工具

- JDK 17、Python 3、PowerShell 7。
- Android SDK platform 35 / build-tools 35.0.0，设置 `ANDROID_SDK_ROOT`。
- Windows 构建使用 SDK 的 aapt2.exe / aidl.exe / d8.bat / zipalign.exe。

```powershell
git clone https://github.com/Chuniex-0631/honor-anb-an00-root.git
cd honor-anb-an00-root
python tools/check_public_release.py
pwsh -File test.ps1
pwsh -File build.ps1
```

依赖仅从 Maven Central 与 Google Maven 的固定版本下载，并按 [dependencies.lock.json](../deps/dependencies.lock.json) 验证 JAR 哈希。App 不需要联网，联网只用于开发构建下载依赖。

结果为 `build/anb-root-unsigned.apk`。本地开发者应使用自己的 Android 签名密钥与 apksigner；自行签名的包不能覆盖正式发行签名，不能将自行构建文件称为官方发布 APK。CI 只构建未签名工件，不持有发布密钥。

可用 `-BuildToolsPath` 和 `-AndroidJar` 指定非标准 SDK 布局。没有把操作者个人目录写进源码。

## 运行时资产

Java/AIDL 源码可直接审计；8 个原载荷保留字节。校验见 [当前核心文件清单](../release/current.json) 与 [资产哈希](../app/assets/payload-hashes.properties)。上游 native 源码／许可见 [第三方说明](THIRD_PARTY.md)。

编译成功不是实机利用成功；支持范围不因编译通过而扩展。2.4.0 的控制逻辑、原 loader 收尾标记及实时保护读回检查见 [审计与验证](AUDIT_2.4.0.md)，不要仅用模块存在或 `uid=0` 替代完整成功判定。

部分 Windows SDK 原生工具在中文路径下会报告“数据无效”或无法打开输出。源码、输出目录和 SDK 工具都应放在实际不含中文的路径；仅为含中文的 SDK 目录映射盘符仍可能失败。本轮用官方 build-tools 35.0.0 的原字节工具复制到纯英文目录后构建通过。
