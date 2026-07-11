param(
    [string]$ProjectRoot = "D:\AI Demo\菁选",
    [string]$JavaHome = "D:\java\jdk",
    [string]$MysqlExe = "D:\java\Mysqls\mysql-8.4.5-winx64\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"

$backendDir = Join-Path $ProjectRoot "backend"
$scriptsDir = Join-Path $ProjectRoot "scripts"

Write-Host "Step 1: apply runtime support schema"
& (Join-Path $scriptsDir "apply-runtime-support.ps1") -MysqlExe $MysqlExe

Write-Host "Step 2: ensure JAVA_HOME for this session"
$env:JAVA_HOME = $JavaHome
$env:Path = "$($JavaHome)\bin;$env:Path"

Write-Host "Step 3: package backend"
Push-Location $backendDir
try {
    mvn -q -DskipTests package
}
finally {
    Pop-Location
}

Write-Host "Step 4: clear existing backend on 8080 if needed"
try {
    $existing = netstat -ano | Select-String ':8080'
    if ($existing) {
        $pids = $existing | ForEach-Object {
            if ($_ -match '\s+(\d+)\s*$') { $matches[1] }
        } | Where-Object { $_ } | Sort-Object -Unique
        foreach ($pid in $pids) {
            taskkill /PID $pid /F | Out-Null
        }
        Start-Sleep -Seconds 2
    }
} catch {
    Write-Host "Skipping backend cleanup: $($_.Exception.Message)"
}

Write-Host "Step 5: start backend in a new PowerShell window"
$startScript = Join-Path $scriptsDir "start-jingxuan-backend.ps1"
$startCommand = "& '$startScript' -JavaHome '$JavaHome' -ProjectRoot '$ProjectRoot'"
Start-Process powershell -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $startCommand

Write-Host "Step 6: wait for 8080"
$ready = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 2
    try {
        Invoke-WebRequest -Uri "http://127.0.0.1:8080/doc.html" -UseBasicParsing -TimeoutSec 2 | Out-Null
        $ready = $true
        break
    } catch {
        # continue
    }
}

if (-not $ready) {
    throw "Backend did not become ready on 8080"
}

Write-Host "Step 7: run runtime sample validation"
& (Join-Path $scriptsDir "test-runtime-demo.ps1")

Write-Host "Runtime validation flow completed."
