param(
    [string]$BaseUrl = 'http://127.0.0.1:18080/KeebForce',
    [string]$DbConfig = 'config/db.properties'
)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $repoRoot
try {
    $evidenceDir = Join-Path $repoRoot 'docs/verification/p3'
    $testClasses = Join-Path $repoRoot 'build/verify-p307'
    New-Item -ItemType Directory -Path $evidenceDir,$testClasses -Force | Out-Null
    $java = 'C:\Program Files\Java\jdk-17\bin\java.exe'
    $javac = 'C:\Program Files\Java\jdk-17\bin\javac.exe'
    $classPath = "$testClasses;build/web/WEB-INF/classes;web/WEB-INF/lib/*"
    $sources = @('VerifyP301','VerifyP303','VerifyP304','VerifyP305','VerifyP306','VerifyP307') | ForEach-Object { "test/$_.java" }
    & $javac -encoding UTF-8 -cp 'build/web/WEB-INF/classes;web/WEB-INF/lib/*' -d $testClasses $sources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    $before = (& $java '-Dfile.encoding=UTF-8' "-Dkeebforce.db.config=$DbConfig" -cp $classPath VerifyP307 --fingerprint).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot capture database baseline' }
    $results = @()
    foreach ($test in @('VerifyP301','VerifyP302','VerifyP303','VerifyP304','VerifyP305','VerifyP306')) {
        if ($test -eq 'VerifyP302') {
            $output = @(& powershell.exe -NoProfile -ExecutionPolicy Bypass -File test/VerifyP302.ps1 -BaseUrl $BaseUrl)
        } else {
            $output = @(& $java '-Dfile.encoding=UTF-8' "-Dkeebforce.db.config=$DbConfig" -cp $classPath $test $BaseUrl)
        }
        $exitCode = $LASTEXITCODE
        $output | Set-Content -LiteralPath (Join-Path $evidenceDir "$test.txt") -Encoding UTF8
        if ($exitCode -ne 0) { throw "$test failed; inspect its evidence log" }
        $pass = @($output | Where-Object { $_ -match '^PASS (\d+)' }) | Select-Object -Last 1
        if (!$pass -or $pass -notmatch '^PASS (\d+)') { throw "$test produced no verified PASS result" }
        $results += [pscustomobject]@{ test=$test; checks=[int]$Matches[1]; passed=$true }
        Write-Output "$test $pass"
    }
    $after = (& $java '-Dfile.encoding=UTF-8' "-Dkeebforce.db.config=$DbConfig" -cp $classPath VerifyP307 --fingerprint).Trim()
    if ($LASTEXITCODE -ne 0 -or $before -ne $after) { throw 'Database row content differs from baseline after regression' }
    $summary = [pscustomobject]@{
        verifiedAtUtc=[DateTime]::UtcNow.ToString('o'); baseUrl=$BaseUrl
        results=$results; totalChecks=($results | Measure-Object checks -Sum).Sum
        databaseContentsRestored=$true; databaseFingerprint=$after
    }
    $summary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $evidenceDir 'regression-summary.json') -Encoding UTF8
    Write-Output "PASS $($summary.totalChecks) regression checks; database contents restored exactly"
} finally { Pop-Location }
