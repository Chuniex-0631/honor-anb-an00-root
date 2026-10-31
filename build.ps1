param(
    [string]$SdkRoot = $env:ANDROID_SDK_ROOT,
    [string]$BuildToolsVersion = '35.0.0',
    [string]$BuildToolsPath,
    [string]$AndroidJar
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if (-not $BuildToolsPath) {
    if (-not $SdkRoot) { throw 'Set ANDROID_SDK_ROOT or pass SDK tool paths.' }
    $BuildToolsPath = Join-Path $SdkRoot "build-tools/$BuildToolsVersion"
}
if (-not $AndroidJar) { $AndroidJar = Join-Path $SdkRoot 'platforms/android-35/android.jar' }
$framework = Join-Path (Split-Path $AndroidJar) 'framework.aidl'
$out = Join-Path $root 'build'
$app = Join-Path $root 'app'
$generated = Join-Path $out 'generated'
$resolvedOut = [IO.Path]::GetFullPath($out)
if ($resolvedOut -ne (Join-Path ([IO.Path]::GetFullPath($root)) 'build')) { throw 'Unexpected output path.' }
if (Test-Path -LiteralPath $resolvedOut) { Remove-Item -LiteralPath $resolvedOut -Recurse -Force }
New-Item -ItemType Directory -Force $generated,(Join-Path $out 'classes'),(Join-Path $out 'dex') | Out-Null
& python (Join-Path $root 'tools/fetch_android_deps.py')
if ($LASTEXITCODE -ne 0) { throw 'Dependency verification failed.' }
$jars = @('api.jar','provider.jar','aidl.jar','shared.jar','annotation.jar') | ForEach-Object { Join-Path (Join-Path $root 'deps') $_ }
$classpath = (@($AndroidJar) + $jars) -join [IO.Path]::PathSeparator
$release = Get-Content -LiteralPath (Join-Path $root 'release/v2.1.0.json') -Raw | ConvertFrom-Json
& (Join-Path $BuildToolsPath 'aapt2.exe') compile --dir (Join-Path $app 'res') -o (Join-Path $out 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'Resource compile failed.' }
& (Join-Path $BuildToolsPath 'aapt2.exe') link -o (Join-Path $out 'base.apk') -I $AndroidJar `
    --manifest (Join-Path $app 'AndroidManifest.xml') -R (Join-Path $out 'res.zip') -A (Join-Path $app 'assets') `
    --min-sdk-version 26 --target-sdk-version 35 --version-code $release.version_code --version-name $release.version --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw 'Resource link failed.' }
& (Join-Path $BuildToolsPath 'aidl.exe') ('-p' + $framework) ('-I' + (Join-Path $app 'aidl')) ('-o' + $generated) (Join-Path $app 'aidl/com/anb/root/IShellService.aidl')
if ($LASTEXITCODE -ne 0) { throw 'AIDL compile failed.' }
$sources = @(Get-ChildItem -Recurse (Join-Path $app 'java'),$generated -Filter *.java | ForEach-Object FullName)
& javac --release 8 -classpath $classpath -encoding UTF-8 -d (Join-Path $out 'classes') $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compile failed.' }
$classes = @(Get-ChildItem -Recurse (Join-Path $out 'classes') -Filter *.class | ForEach-Object FullName)
& (Join-Path $BuildToolsPath 'd8.bat') --lib $AndroidJar --min-api 26 --output (Join-Path $out 'dex') $classes $jars
if ($LASTEXITCODE -ne 0) { throw 'DEX compile failed.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open((Join-Path $out 'base.apk'), 'Update')
try {
    $entry = $zip.CreateEntry('classes.dex', [IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try { $data = [IO.File]::ReadAllBytes((Join-Path $out 'dex/classes.dex')); $stream.Write($data,0,$data.Length) } finally { $stream.Dispose() }
} finally { $zip.Dispose() }
& python (Join-Path $root 'tools/normalize_apk.py') (Join-Path $out 'base.apk') (Join-Path $out 'normalized.apk')
if ($LASTEXITCODE -ne 0) { throw 'APK normalization failed.' }
& (Join-Path $BuildToolsPath 'zipalign.exe') -f 4 (Join-Path $out 'normalized.apk') (Join-Path $out 'anb-root-unsigned.apk')
if ($LASTEXITCODE -ne 0) { throw 'Alignment failed.' }
Write-Host 'Built build/anb-root-unsigned.apk. Sign with your own keystore for local development.'
