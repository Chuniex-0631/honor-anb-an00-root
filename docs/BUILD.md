# 构建说明

正式 APK 在 [Releases](https://github.com/Chuniex-0631/honor-anb-an00-root/releases/latest)，以 [发布清单](../release/v2.1.0.json) 的 SHA-256 和证书指纹为准。签名私钥不在仓库。

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

Java/AIDL 源码可直接审计；8 个原载荷保留字节。校验见 [core-files.json](../release/core-files.json) 与 [资产哈希](../app/assets/payload-hashes.properties)。上游 native 源码／许可见 [第三方说明](THIRD_PARTY.md)。

编译成功不是实机利用成功；支持范围不因编译通过而扩展。
