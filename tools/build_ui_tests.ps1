param([string]$BuildToolsPath,[string]$AndroidJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$out=Join-Path $root 'test-build/android-ui'
New-Item -ItemType Directory -Force $out,(Join-Path $out 'classes'),(Join-Path $out 'dex') | Out-Null
& javac --release 8 -encoding UTF-8 -cp ($AndroidJar+[IO.Path]::PathSeparator+(Join-Path $root 'build/classes')) -d (Join-Path $out 'classes') (Join-Path $root 'tests/android-ui/UiAuditInstrumentation.java')
if($LASTEXITCODE -ne 0){throw 'UI test compilation failed'}
& jar cf (Join-Path $out 'app-api.jar') -C (Join-Path $root 'build/classes') .
& (Join-Path $BuildToolsPath 'd8.bat') --lib $AndroidJar --classpath (Join-Path $out 'app-api.jar') --min-api 26 --output (Join-Path $out 'dex') (Join-Path $out 'classes/com/anb/root/uitest/UiAuditInstrumentation.class')
if($LASTEXITCODE -ne 0){throw 'UI test dex failed'}
& (Join-Path $BuildToolsPath 'aapt2.exe') link -o (Join-Path $out 'ui-tests-unsigned.apk') -I $AndroidJar --manifest (Join-Path $root 'tests/android-ui/AndroidManifest.xml') --min-sdk-version 26 --target-sdk-version 35
if($LASTEXITCODE -ne 0){throw 'UI test packaging failed'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::Open((Join-Path $out 'ui-tests-unsigned.apk'),'Update')
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $out 'dex/classes.dex'),'classes.dex') | Out-Null}finally{$archive.Dispose()}
Write-Host 'Built test-build/android-ui/ui-tests-unsigned.apk; sign with the installed app certificate before using on a test device.'
