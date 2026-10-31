$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$outputPath = Join-Path $projectRoot 'test-build'
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
& javac -encoding UTF-8 -d $outputPath `
    (Join-Path $projectRoot 'app/java/com/anb/root/CommandRunner.java') `
    (Join-Path $projectRoot 'app/java/com/anb/root/Readiness.java') `
    (Join-Path $projectRoot 'app/java/com/anb/root/RunStatus.java') `
    (Join-Path $projectRoot 'app/java/com/anb/root/RecoveryOptions.java') `
    (Join-Path $projectRoot 'app/java/com/anb/root/UiState.java') `
    (Join-Path $projectRoot 'tests/WorkflowTest.java') `
    (Join-Path $projectRoot 'tests/RecoveryOptionsTest.java') `
    (Join-Path $projectRoot 'tests/UiStateTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -cp $outputPath com.anb.root.WorkflowTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
& java -cp $outputPath com.anb.root.RecoveryOptionsTest (Join-Path $projectRoot 'app/assets/payload/ksu_loader.tmpl')
if ($LASTEXITCODE -ne 0) { throw 'Recovery option tests failed' }

& java -cp $outputPath com.anb.root.UiStateTest
if ($LASTEXITCODE -ne 0) { throw 'UI state tests failed' }
