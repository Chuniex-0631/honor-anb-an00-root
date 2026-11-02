$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$outputPath = Join-Path $projectRoot 'test-build'
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$production = @('CommandRunner','Readiness','RunStatus','RecoveryOptions','StatusModel','FailureText','Diagnostics','DeviceProfile','TaskGate','RecoveryFailure') | ForEach-Object { Join-Path $projectRoot ('app/java/com/anb/root/' + $_ + '.java') }
$tests = @(Get-ChildItem (Join-Path $projectRoot 'tests') -Filter '*Test.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -d $outputPath $production $tests
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -cp $outputPath com.anb.root.WorkflowTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
& java -cp $outputPath com.anb.root.RecoveryOptionsTest (Join-Path $projectRoot 'app/assets/payload/ksu_loader.tmpl')
if ($LASTEXITCODE -ne 0) { throw 'Recovery option tests failed' }
& java -cp $outputPath com.anb.root.StatusModelTest
if ($LASTEXITCODE -ne 0) { throw 'Status model tests failed' }
& java -cp $outputPath com.anb.root.FailureTextTest
if ($LASTEXITCODE -ne 0) { throw 'Failure classification tests failed' }
& java -cp $outputPath com.anb.root.DiagnosticsTest
if ($LASTEXITCODE -ne 0) { throw 'Diagnostics tests failed' }

& java -cp $outputPath com.anb.root.AuditRegressionTest
if ($LASTEXITCODE -ne 0) { throw 'Audit regression tests failed' }

$engineOutput = Join-Path $outputPath 'engine-fixtures'
New-Item -ItemType Directory -Force -Path $engineOutput | Out-Null
$engineFixtures = @(Get-ChildItem -Recurse (Join-Path $projectRoot 'tests/engine-fixtures') -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -cp $outputPath -d $engineOutput $engineFixtures (Join-Path $projectRoot 'app/java/com/anb/root/RecoveryEngine.java')
if ($LASTEXITCODE -ne 0) { throw 'Engine fixture compilation failed' }
& java -cp ($engineOutput + [IO.Path]::PathSeparator + $outputPath) com.anb.root.EngineIntegrationTest (Join-Path $projectRoot 'app/assets') $outputPath
if ($LASTEXITCODE -ne 0) { throw 'Engine integration tests failed' }

$serviceOutput = Join-Path $outputPath 'service-fixtures'
New-Item -ItemType Directory -Force -Path $serviceOutput | Out-Null
$serviceFixtures = @(Get-ChildItem -Recurse (Join-Path $projectRoot 'tests/service-fixtures') -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -d $serviceOutput $serviceFixtures (Join-Path $projectRoot 'app/java/com/anb/root/RecoveryService.java')
if ($LASTEXITCODE -ne 0) { throw 'Service fixture compilation failed' }
& java -cp $serviceOutput ServiceRegressionTest
if ($LASTEXITCODE -ne 0) { throw 'Service integration tests failed' }
